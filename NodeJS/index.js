var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var admin = require('firebase-admin');
var db_config = require('./database.json');
var mysql = require('mysql');

var connection = mysql.createConnection({
    host: db_config.host,
    user: db_config.user,
    password: db_config.password,
    port: db_config.port,
    database: db_config.database,
    multipleStatements: true,
});
connection.connect();

var serviceAccount = require('./capstone-car-firebase-adminsdk-zqm0k-2248c7ebc5.json');
 
// token
var sender_token = "f0Ane5gaRo2KTtUjjo6ugl:APA91bGZwT-Mi5TexV1C3DeHVI4DFa9IECxrOm_wg8axxZcFEXWex5yIjOxTcEGXjeE7uht2djnnqs9qL_cbEYuKrE_griGa-Jlj-Yzvimm3XrtApWQclEzt8WqxIOIDjVRVsVHdNXj-";
var receiver_token = "f0Ane5gaRo2KTtUjjo6ugl:APA91bGZwT-Mi5TexV1C3DeHVI4DFa9IECxrOm_wg8axxZcFEXWex5yIjOxTcEGXjeE7uht2djnnqs9qL_cbEYuKrE_griGa-Jlj-Yzvimm3XrtApWQclEzt8WqxIOIDjVRVsVHdNXj-";

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

// fcm_message
var user; // reciver , sender
var title_msg; // title
var body_msg; // message

io.on('connection', (socket) => {
    console.log('one user connection // ' + socket.id);

    // Delivery Call
    socket.on('dlvy_call', (data) => { 

        var {sender_id, receiver_id, start_point, end_point, sender_name} = data;
        var wait; // waiting status
        var waiting_num; // waiting number
        var car_num; 
        
        // Search for cars waiting to be used
        connection.query('select car_num from car where car_status = "배달대기"', (err, rows, fields) => {
            if(err) console.log("err : " + err); 

            if(rows.length >= 1) { 
                car_num = rows[0].car_num;
                wait = false;
                waiting_num = 0;

                dlvy_call_db_insert(car_num, wait, waiting_num, sender_id, receiver_id, start_point, end_point, sender_name);
                connection.query(`select count(*) as count from dlvy where dlvy_date = curdate()`, (err, rows, fields) => {
                    console.log(rows);
                    io.emit("call_count", rows);
                });
            }
            else if(rows.length == 0) { 
                // Get Wait Number
                connection.query('select dlvy_num from dlvy where dlvy_status = "대기중" and dlvy_date = curdate()', (err, rows, fields) => {
                    if(err) console.log("err : " + err); 
                    car_num = 0;
                    wait = true;
                    waiting_num = rows.length + 1;
                    
                    dlvy_call_db_insert(car_num, wait, waiting_num, sender_id, receiver_id, start_point, end_point, sender_name);
                    connection.query(`select count(*) as count from dlvy where dlvy_date = curdate()`, (err, rows, fields) => {
                        console.log(rows);
                        io.emit("call_count", rows); 
                    });
                });
            }
        });
    });

    // Delivery Acceptance Confirmation
    socket.on('accept', (data) => { 
        if(data.accept == 'yes') { // delivery acceptance
            if(data.wait == 'no') { // no waiting

                // Send status of cars to the web
                connection.query(`select car_status, count(*) as cnt from car group by car_status`, (err, rows, fields) => {
                    var dd = new Object();
                    dd.call = 0;
                    dd.dlvy = 0;
                    dd.err = 0;
                    dd.wait = 0;
                    for(var i = 0; i < rows.length; i++){
                        if(rows[i].car_status=="호출중"){
                            dd.call = rows[i].cnt;
                        }else if(rows[i].car_status == "배달중"){
                            dd.dlvy = rows[i].cnt;
                        }else if(rows[i].car_status == "배달대기"){
                            dd.wait = rows[i].cnt;
                        }else if(rows[i].car_status == "오류"){
                            dd.err = rows[i].cnt;
                        }
                    }
                    io.emit("rc_status", dd);
                });
                
                var dlvy_data = new Object();
                dlvy_data.dlvy_num = data.dlvy_num; 
                dlvy_data.car_num = data.car_num; 

                // Deliver the work to the car
                connection.query(`select dlvy_start_point, dlvy_end_point from dlvy where dlvy_num = ${data.dlvy_num}`, (err, rows, fields) => {
                    // Get checkpoint gps
                    connection.query(`SELECT c.checkpoint_lat, c.checkpoint_lon 
                                    FROM path_check as pc left join checkpoint as c on pc.check_id = c.checkpoint_id 
                                    where pc.path_col_id in 
                                        ( select path_id from path where path_start_point = "${rows[0].dlvy_start_point}" and path_end_point = "${rows[0].dlvy_end_point}") 
                                    order by sequence asc;`, (err, rows, fields) => {                        

                        // io.emit('dlvy_start_data', dlvy_data);
                    });
                });    
             
            }else { // waiting
                
                var wait_data = new Object();

                // Send status of waiting operation to the web
                connection.query(`select dlvy_num from dlvy where dlvy_wait_time is not null and dlvy_date = curdate()`, (err, rows, fields) => {
                    wait_data.wait_complete = rows.length; 
                    connection.query(`select dlvy_num from dlvy where dlvy_status = "대기중" and dlvy_date = curdate()`, (err, rows, fields) => {
                        wait_data.wait_now = rows.length; 
                        connection.query(`select dlvy_num from dlvy where dlvy_status = "대기취소" and dlvy_date = curdate()`, (err, rows, fields) => {
                            wait_data.wait_cancel = rows.length;
                            console.log(wait_data);
                            
                            io.emit('wait_data', wait_data);
                        });
                    });
                });
            }
        }else { // delivery rejection
            if(data.wait == 'no') {  // no waiting
                
                // Change to Car Status Delivery Waiting
                connection.query(`update car set car_status = '배달대기' where (car_num = ${data.car_num})`, (err, rows, fields) => {
                    if(err) console.log("err : " + err); 
                    else console.log('대기중 변경 Successfully');
                })
            }
            // Delete from task db
            connection.query(`delete from dlvy where (dlvy_num = ${data.dlvy_num})`, (err, rows, fields) => {
                if(err) console.log("err : " + err); // err 표시
                else console.log('작업 삭제 완료');
            });
        }
    });

    // Vehicle Departure Notification
    socket.on('dlvy_departure', (data) => { 
        if(data.departure == '출발지'){  // Move to the starting point
            user = sender_token;
            title_msg = '호출 시작';
            body_msg = '출발지로 가고 있습니다';
        }else if(data.departure == '물품보내기') { // Move to destination
            user = receiver_token;
            title_msg = '배달 시작';
            body_msg = '목적지로 배달이 시작되었습니다';

            // Changed to Car Status Delivery
            connection.query(`update car set car_status = "배달중" where car_num=${data.car_num}`, (err, rows, fields) => {
                if(err) console.log(err);
                else console.log('배달중 변경 Successfully');
            });

            // Change Delivery Status
            connection.query(`update dlvy set dlvy_status = "배달중", dlvy_start = curtime() where dlvy_num=${data.dlvy_num}`, (err, rows, fields) => {
                if(err) console.log(err);
                else console.log('배달중 변경, 배달시작 시간 저장 Successfully');
            });
        }
        notification_message(user, title_msg, body_msg);
    });

    // Car Arrival Notification
    socket.on('dlvy_arrival', (data) => {
        if(data.arrival == '출발지') { // Arrival at Departure
            user = sender_token;
            title_msg = '자동차 도착';
            body_msg = '물품을 적재 해주십시오';
        }else if(data.arrival == '목적지') { // destination arrival
            user = receiver_token;
            title_msg = '배달 도착';
            body_msg = '물품을 수령 해주십시오';
        }

        notification_message(user, title_msg, body_msg);
    });

    // Delivery complete
    socket.on('dlvy_complete', (data) => { 
        // Change Delivery Job Status to Complete
        connection.query(`update dlvy set dlvy_status = "배달완료", dlvy_end = curtime() where dlvy_num=${data.dlvy_num}`, (err, rows, fields) => {
            if(err) console.log(err);
            else console.log('배달완료 변경, 배달완료 시간 저장 Successfully');
        });

        if(data.complete == "수령완료"){
            user = sender_token;
            title_msg = "배달 완료"
            body_msg = "배달의 완료되었습니다."
        }
        notification_message(user, title_msg, body_msg);

        // Real-time delivery status, number of completed deliveries to the web
        connection.query(`select count(*) as count from dlvy where dlvy_date = curdate() and dlvy_status = "배달완료"`, (err, rows, fields) => {
            io.emit("complete_dlvy_count", rows);
        })

        // Find Remaining waiting delivery
        connection.query(`select dlvy_num from dlvy where dlvy_status = "대기중" and dlvy_date = curdate()`, (err, rows, fields) => {
            if(rows.length > 0){ // waiting Job Found

                // Change to calling completed car status
                connection.query(`update car set car_status = "호출중" where car_num = ${data.car_num}`, (err, rows, fields) => {
                    if(err) console.log(err);
                    else console.log('RC카 상태 호출중 Successfully');
                });
                // Change Delivery Job Status to calling
                connection.query(`update dlvy set dlvy_car_num = ${data.car_num}, dlvy_status = "호출중", dlvy_wait_time = timestampdiff(minute, dlvy_wait_start, curtime()), dlvy_call_start = curtime() where dlvy_num = ${rows[0].dlvy_num}`, (err, rows, fields) => {
                    if(err) console.log(err);
                    else console.log('작업 상태 대기중->호출중 Successfully');
                });

                var dlvy_data = new Object();
                dlvy_data.dlvy_num = rows[0].dlvy_num; 
                dlvy_data.car_num = data.car_num;

                // Deliver the work to the car
                connection.query(`select dlvy_start_point, dlvy_end_point from dlvy where dlvy_num = ${rows[0].dlvy_num}`, (err, rows, fields) => {
                    // Get checkpoint gps
                    connection.query(`SELECT c.checkpoint_lat, c.checkpoint_lon 
                                    FROM path_check as pc left join checkpoint as c on pc.check_id = c.checkpoint_id 
                                    where pc.path_col_id in 
                                        ( select path_id from path where path_start_point = "${rows[0].dlvy_start_point}" and path_end_point = "${rows[0].dlvy_end_point}") 
                                    order by sequence asc;`, (err, rows, fields) => {                        
                        
                        dlvy_data.gps = rows  
                        
                        // io.emit('dlvy_start_data', dlvy_data);
                    });
                });   

                var wait_data = new Object();
                
                // Send status of waiting operation to the web
                connection.query(`select dlvy_num from dlvy where dlvy_wait_time is not null and dlvy_date = curdate()`, (err, rows, fields) => {
                    wait_data.wait_complete = rows.length; 
                    connection.query(`select dlvy_num from dlvy where dlvy_status = "대기중" and dlvy_date = curdate()`, (err, rows, fields) => {
                        wait_data.wait_now = rows.length; 
                        connection.query(`select dlvy_num from dlvy where dlvy_status = "대기취소" and dlvy_date = curdate()`, (err, rows, fields) => {
                            wait_data.wait_cancel = rows.length;
                            
                            connection.query(`select floor(avg(dlvy_wait_time)) as time from dlvy where dlvy_date = curdate() group by dlvy_date`, (err, rows, fields) => {
                                wait_data.wait_avg_time = rows[0].time;

                                io.emit('wait_data', wait_data);
                            });
                        });
                    });
                });

                // Send status of cars to the web
                connection.query(`select car_status, count(*) as cnt from car group by car_status`, (err, rows, fields) => {
                    var dd = new Object();
                    dd.call = 0;
                    dd.dlvy = 0;
                    dd.err = 0;
                    dd.wait = 0;
                    console.log(rows[0].cnt);
                    for(var i = 0; i < rows.length; i++){
                        if(rows[i].car_status=="호출중"){
                            dd.call = rows[i].cnt;
                        }else if(rows[i].car_status == "배달중"){
                            dd.dlvy = rows[i].cnt;
                        }else if(rows[i].car_status == "배달대기"){
                            dd.wait = rows[i].cnt;
                        }else if(rows[i].car_status == "오류"){
                            dd.err = rows[i].cnt;
                        }
                    }
                    io.emit("rc_status", dd);
                });
            }else if(rows.length == 0) { // No waiting Operations , End of operation
                connection.query(`update car set car_status = "배달대기" where car_num = ${data.car_num}`, (err, rows, fields) => {
                    if(err) console.log(err);

                    // Send status of cars to the web
                    connection.query(`select car_status, count(*) as cnt from car group by car_status`, (err, rows, fields) => {
                        var dd = new Object();
                        dd.call = 0;
                        dd.dlvy = 0;
                        dd.err = 0;
                        dd.wait = 0;
                        console.log(rows[0].cnt);
                        for(var i = 0; i < rows.length; i++){
                            if(rows[i].car_status=="호출중"){
                                dd.call = rows[i].cnt;
                            }else if(rows[i].car_status == "배달중"){
                                dd.dlvy = rows[i].cnt;
                            }else if(rows[i].car_status == "배달대기"){
                                dd.wait = rows[i].cnt;
                            }else if(rows[i].car_status == "오류"){
                                dd.err = rows[i].cnt;
                            }
                        }
                        io.emit("rc_status", dd);
                    });
                })
            }
        });
    });

    // Canceling a pending delivery
    socket.on('dlvy_wait_cancel', (data) => {

        // Change Delivery Job Status to Unwaiting
        connection.query(`update dlvy set dlvy_status = "대기취소" where (dlvy_num = ${data.dlvy_num})`, (err, rows, fields) => {
            if(err) console.log(err);
            
            var wait_data = new Object();
            
            // Send status of waiting operation to the web
            connection.query(`select dlvy_num from dlvy where dlvy_wait_time is not null and dlvy_date = curdate()`, (err, rows, fields) => {
                wait_data.wait_complete = rows.length; 
                connection.query(`select dlvy_num from dlvy where dlvy_status = "대기중" and dlvy_date = curdate()`, (err, rows, fields) => {
                    wait_data.wait_now = rows.length; 
                    connection.query(`select dlvy_num from dlvy where dlvy_status = "대기취소" and dlvy_date = curdate()`, (err, rows, fields) => {
                        wait_data.wait_cancel = rows.length; 
                                    
                        io.emit('wait_data', wait_data);
                    });
                });
            });
        });

    });

    // real-time gps information for cars
    socket.on('rc_gps', (data) => { 

        var car_data = new Object();
        car_data.car_num = data.car_num;
        car_data.car_lat = data.car_lat;
        car_data.car_lon = data.car_lon;
        console.log(car_data.car_num, car_data.car_lat, car_data.car_lon);
        
        connection.query(`update car set car_lat = ${data.car_lat}, car_lon = ${data.car_lon} where car_num = ${data.car_num}`, (err, rows, fields) => {
            if(err) console.log(err);
            // Car Information to the Web ( status, car's number, latitude, longitude )
            connection.query(`select car_status from car where car_num = ${data.car_num}`, (err, rows, data) => {
                car_data.car_status = rows[0].car_status;

                io.emit("car_data", car_data);
            });
        });
           
    });

    // car error information
    socket.on('rc_error', (data) => { // rc카 id, 작업번호, 오류내역
        
        // Change to car state error, save error history
        connection.query(`update car set car_status = "오류", car_error = "${data.err_msg}" where car_num = ${data.car_num}`, (err, rows, fields) => {
            if(err) console.log(err);
        });
        // Change job status to error
        connection.query(`update dlvy set dlvy_status = "오류", dlvy_error = "${data.err_msg}" where dlvy_num = ${data.dlvy_num}`, (err, rows, fields) => {
            if(err) console.log(err);
        });

        // Car status, error notification to web
        var car_data = new Object();
        car_data.car_num = data.car_num;
        car_data.err_msg = data.err_msg;
        car_data.dlvy_num = data.dlvy_num;
        
        connection.query(`select car_status, count(*) as cnt from car group by car_status`, (err, rows, fields) => {
            car_data.call = 0;
            car_data.dlvy = 0;
            car_data.err = 0;
            car_data.wait = 0;
            for(var i = 0; i < rows.length; i++){
                if(rows[i].car_status=="호출중"){
                    car_data.call = rows[i].cnt;
                }else if(rows[i].car_status == "배달중"){
                    car_data.dlvy = rows[i].cnt;
                }else if(rows[i].car_status == "배달대기"){
                    car_data.wait = rows[i].cnt;
                }else if(rows[i].car_status == "오류"){
                    car_data.err = rows[i].cnt;
                }
            }
            
            io.emit('car_err', car_data);
        });
    });

});

// Save to Call Operation Database
function dlvy_call_db_insert(car_num, wait, waiting_num, sender_id, receiver_id, start_point, end_point, sender_name) {
    
    var sql = '';
    var insert_dlvy_num; 

    if(!wait) { // no waiting
        
        sql = `insert into dlvy(dlvy_car_num, dlvy_status, dlvy_start_point, dlvy_end_point,  dlvy_sender,    dlvy_receiver,   dlvy_call_start, dlvy_date) 
                          values(${car_num},   "호출중",    '${start_point}', '${end_point}', '${sender_id}', '${receiver_id}',   curtime(),    curdate())`;

        // Change to car status calling
        connection.query(`update car set car_status = "호출중" where car_num=${car_num}`, (err, rows, fields) => {
            if(err) console.log(err);
            else console.log('호출중 변경 Successfully');
        });
    }else { // waiting
        
        sql = `insert into dlvy(dlvy_status, dlvy_start_point,  dlvy_end_point,   dlvy_sender,  dlvy_receiver, dlvy_wait_start, dlvy_date) 
                         values("대기중",    '${start_point}',  '${end_point}', '${sender_id}', '${receiver_id}', curtime(),    curdate())`;
    }
    
    connection.query(sql, (err, rows, fields) => {
        if(err) console.log("err : " + err); 
        else {
            
            insert_dlvy_num = rows.insertId;

            console.log('insert_dlvy_num: ' +insert_dlvy_num);

            // Send job acceptance notifications to users
            var fcm_message = {
                notification: { 
                    title: "배달 수락",
                    body: "배달을 확인해 주세요",
                },
                data: { 
                    waiting_num: ''+waiting_num, 
                    car_num: ''+car_num,
                    dlvy_num: ''+insert_dlvy_num,
                    sender_name: ''+sender_name,
                },
                token: receiver_token
            };
        
            admin.messaging().send(fcm_message)
                .then((res) => {
                    console.log('Successfully sent message: ', res);
                })
                .catch((err) => {
                    console.log('Error sending message: ', err);
                });
        }


    })

};

// Car Departure and Arrival Notification
function notification_message(user, title_msg, body_msg) {
    
    var fcm_message = {
        notification: { 
            title: title_msg,
            body: body_msg,
        },
        data: { 
        },
        token: user
    };

    admin.messaging().send(fcm_message)
        .then((res) => {
            console.log('Successfully sent message: ', res);
        })
        .catch((err) => {
            console.log('Error sending message: ', err);
        });
}

http.listen(3000, () => {
    console.log('server listening on port 3000');
});