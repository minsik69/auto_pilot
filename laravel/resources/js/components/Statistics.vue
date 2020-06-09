<template>
    <div class="container">
        <div class="left_container">
            <div class="left_container_item1">     <!--category -->
                <b-card
                    header="카테고리"
                    header-tag="header"
                    style="max-width: 14rem;text-align:center;"
                    class="mb-2">
                    <b-button class="button" @click="categori_change('complete')">
                        배달 완료 건 수
                    </b-button>
                    <b-button class="button" @click="categori_change('waiting_status')">
                        대기 완료 / 취소 건 수
                    </b-button>
                    <b-button class="button" @click="categori_change('waiting_time_avg')">
                        평균 대기 시간
                    </b-button>
                </b-card>                         <!-- category -->
            </div>
        </div>
        <div class="right_container">
            <div class="right_container_item1">   <!-- margin -->
            </div>
            <div class="right_container_item2">    <!-- chart header, datepicker, button, body -->  
                <div class="right_container_item2_left">
                    <div class="right_container_item2_header">  <!-- chart header -->
                        {{categori}}
                    </div>
                    <div class="right_container_item2_header2">
                        <div class="right_container_item2_item1"> <!-- chart Accumulation, average button -->
                            <b-button variant= "success" class ="chart_button" @click="clicked('acc', term)" v-if="term != 'day' & categori != '평균 대기 시간'">
                                    누적
                            </b-button>
                            <b-button variant="info" class ="chart_button" @click="clicked('avg', term)" v-if="term != 'day' & categori != '평균 대기 시간'">
                                    평균
                            </b-button>
                            </div>
                        <div class="right_container_item2_item2">  <!-- chart datepicker -->
                            <vc-date-picker
                                v-if="term=='day'"
                                v-model="date"
                                @dayfocusin="selected"
                                :max-date='yesterday'
                                />
                            <vc-date-picker
                                v-if="term=='week'"
                                :attributes='attributes'
                                @dayfocusin="selected"
                                :max-date='for_week_disable'
                                :masks="{ input: `${input_text}`}"
                                :input-props='{ 
                                    placeholder: `${attributes[0].dates.start.getFullYear()}.${attributes[0].dates.start.getMonth()+1}.${attributes[0].dates.start.getDate()} ~ ${attributes[0].dates.end.getFullYear()}.${attributes[0].dates.end.getMonth()+1}.${attributes[0].dates.end.getDate()}`,
                                }'
                                />
                            <vue-monthly-picker
                                v-if="term=='month'"
                                @selected="showDate"
                                :max="new Date(today.getFullYear(), today.getMonth()-1,1)"
                                v-model="selected_month"
                                />
                        </div>
                        <div class="right_container_item2_item3"> <!-- chart day, week, month button -->
                            <b-button variant="info" class ="chart_button" @click="clicked(mode, 'day')">
                                일간
                            </b-button>
                            <b-button variant="info" class ="chart_button" @click="clicked(mode, 'week')">
                                주간
                            </b-button>
                            <b-button variant="info" class ="chart_button" @click="clicked(mode, 'month')">
                                월간
                            </b-button>
                        </div>
                    </div>
                                <bar-chart
                                :chart-data="datacollection"/>
                    </div>        
            </div>
        </div>
    </div>
</template>
<script>
import BarChart from './Bar_Chart.js';
import VueMonthlyPicker from 'vue-monthly-picker'

    export default {
        data(){
            return {
                categori : '배달 완료',
                date : new Date(),  //graph's date
                today : new Date(), //today's date
                for_week_disable : '', //disabled week
                selected_month : new Date().getFullYear()+'/'+new Date().getMonth(),    //selected month
                yesterday : '', //yesterday's date
                mode : 'acc',       //mode(accumulation, average)
                term : 'day',       //term(day, week, month)
                input_text : '',    //week picker's input value
                attributes: [       //week picker's style
                    {
                    highlight: {
                        color: 'purple'
                    },
                    dates : {
                        start: '',
                        end: '',
                    },
                    }
                ],
                complete_date : [],         //completed delivery graph's date
                complete_number : [],       //completed delivery graph's value
                waiting_status_date : [],   //waiting info graph's date
                waiting_complete : [],      //completed waiting graph's value
                waiting_cancel : [],        //canceled waiting graph's value
                wait_time_avg_date : [],    //average waiting time graph's date
                wait_time_avg : [],         //average waiting time graph's value
                datacollection: null,       //graph's option
            }
        },
        components: {
            VueMonthlyPicker,
            BarChart
        },
        mounted(){
            this.yesterday = this.date.getTime() - (1 * 24 * 60 * 60 * 1000);   //date of yesterday
            this.date.setTime(this.yesterday)        //for disabled picker

            this.loaded();
            var week_start =  this.today.getDate() - (this.today.getDay()+7);   //set the date for default week
            var week_end = this.today.getDate() - (this.today.getDay()+1);
            this.attributes[0].dates = {
                start : new Date(this.today.getFullYear(), this.today.getMonth(), week_start),
                end : new Date(this.today.getFullYear(), this.today.getMonth(),week_end)
            }
            this.for_week_disable = new Date(this.today.getFullYear(), this.today.getMonth(), week_end);
        },
        methods : {
            categori_change(val){                       //change categori
                if(val == 'complete'){
                    this.categori = "배달 완료";
                }else if(val == 'waiting_status'){
                    this.categori = "대기 완료/취소";
                }else if(val == 'waiting_time_avg'){
                    this.categori = "평균 대기 시간";
                }
                this.mode = "acc"
                this.term = "day"
                this.loaded();
            },
            clicked(mode, term){    //change mode or term
                this.mode = mode;
                this.term = term;
                this.loaded(); 
            },
            selected(day){  //user selected date
                if(this.categori == "배달 완료"){   //when categori is completed delivery
                    if(this.term == 'day'){     //when term is day
                        Axios.get('/api/dlvy/statistics/complete/'+this.mode+'/'+this.term+'/'+this.getFormDate(day.date))   
                        .then((response) => {
                            this.complete_date = response.data.date_info;
                            this.complete_number = response.data.statis_info;
                            this.datacollection = { //set graph's option
                                    labels : this.complete_date,
                                    datasets: [
                                        {
                                            label : '배달 완료 건 수',
                                            backgroundColor: '#f87979',
                                            data : this.complete_number
                                        },
                                    ]
                            }
                        })
                        .catch(error => {
                            console.log(error)
                        })
                    }else if(this.term == 'week'){ //when term is week
                        if(day.date <= this.for_week_disable){
                            var week_start = day.day - (day.weekday-1); //calculate week
                            var week_end = day.day + (7-day.weekday);
                            this.attributes[0].dates = {
                                start : new Date(day.year, day.month-1, week_start),
                                end : new Date(day.year, day.month-1,week_end)
                            }
                        }
                        this.loaded();
                    }
                }else if(this.categori == "대기 완료/취소"){    //when categori is waiting info
                    if(this.term == 'day'){ //when term is day
                        Axios.get('/api/dlvy/statistics/waitcancel/'+this.mode+'/'+this.term+'/'+this.getFormDate(day.date))
                        .then((response) => {
                            this.waiting_status_date = response.data.date_info;
                            this.waiting_complete = response.data.wait_count;
                            this.waiting_cancel = response.data.wait_cancel;
                            this.datacollection = { 
                                labels : this.waiting_status_date,
                                datasets: [
                                    {
                                        label : '대기 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.waiting_complete
                                    },
                                    {
                                        label : '대기 취소 건 수',
                                        backgroundColor : '#2E2EFE',
                                        data : this.waiting_cancel
                                    }
                                ]
                            }
                        })
                    }else if(this.term == 'week'){  //when term is week
                        if(day.date <= this.for_week_disable){
                            var week_start = day.day - (day.weekday-1);
                            var week_end = day.day + (7-day.weekday);
                            this.attributes[0].dates = {
                                start : new Date(day.year, day.month-1, week_start),
                                end : new Date(day.year, day.month-1,week_end)
                            }
                        }
                        this.loaded();
                    }
                }else if(this.categori == "평균 대기 시간"){    //when categori is average waiting time
                    if(this.term == 'day'){
                        Axios.get('/api/dlvy/statistics/waittimeavg/'+this.term+'/'+this.getFormDate(day.date))
                        .then((response) => {
                            this.wait_time_avg_date = response.data.date_info;
                            this.wait_time_avg = response.data.statis_info;
                            this.datacollection = {
                                labels : this.wait_time_avg_date,
                                datasets: [
                                    {
                                        label : '평균 대기 시간',
                                        backgroundColor: '#f87979',
                                        data : this.wait_time_avg
                                    },
                                ]
                            }
                        })
                    }else if(this.term == 'week'){
                        if(day.date <= this.for_week_disable){
                            var week_start = day.day - (day.weekday-1); 
                            var week_end = day.day + (7-day.weekday);
                            this.attributes[0].dates = {
                                start : new Date(day.year, day.month-1, week_start),
                                end : new Date(day.year, day.month-1,week_end)
                            }
                        }
                        this.loaded();
                    }
                }
            },
            showDate(date){                     //user selected date when term is month
                this.selected_month = date._i
                this.loaded();
            },
            getFormDate(date){  //set the format of date
                var year = date.getFullYear();              //yyyy
                var month = (1 + date.getMonth());          //M
                month = month >= 10 ? month : '0' + month;  //month
                var day = date.getDate();                   //d
                day = day >= 10 ? day : '0' + day;          //day
                return  year + '-' + month + '-' + day;
            },
            loaded(){       //graph load method
                if(this.categori == "배달 완료"){
                    if(this.term=='day'){
                        Axios.get('/api/dlvy/statistics/complete/'+this.mode+'/'+this.term+'/'+this.getFormDate(this.date))
                        .then((response) => {
                            this.complete_date = response.data.date_info;
                            this.complete_number = response.data.statis_info;
                            this.datacollection = {
                                labels : this.complete_date,
                                datasets: [
                                    {
                                        label : '배달 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.complete_number
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });                        
                    }else if(this.term == 'week'){
                        Axios.get('/api/dlvy/statistics/complete/'+this.mode+'/'+this.term+'/'+this.getFormDate(this.attributes[0].dates.start))
                        .then((response) => {
                            this.complete_date = response.data.date_info;
                            this.complete_number = response.data.statis_info;
                            this.datacollection = {
                                labels : this.complete_date,
                                datasets: [
                                    {
                                        label : '배달 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.complete_number
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }else if(this.term == 'month'){
                        var parse_month = this.selected_month.split('/');
                        Axios.get('/api/dlvy/statistics/complete/'+this.mode+'/'+this.term+'/'+this.getFormDate(new Date(parse_month[0], parse_month[1]-1, 1)))
                        .then((response) => {
                            this.complete_date = response.data.date_info;
                            this.complete_number = response.data.statis_info;
                            this.datacollection = {
                                labels : this.complete_date,
                                datasets: [
                                    {
                                        label : '배달 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.complete_number
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }
                }else if(this.categori == "대기 완료/취소"){
                    if(this.term=='day'){
                        Axios.get('/api/dlvy/statistics/waitcancel/'+this.mode+'/'+this.term+'/'+this.getFormDate(this.date)) 
                        .then((response) => {
                            this.waiting_status_date = response.data.date_info;
                            this.waiting_complete = response.data.wait_count;
                            this.waiting_cancel = response.data.wait_cancel;
                            this.datacollection = {
                                labels : this.waiting_status_date,
                                datasets: [
                                    {
                                        label : '대기 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.waiting_complete
                                    },
                                    {
                                        label : '대기 취소 건 수',
                                        backgroundColor : '#2E2EFE',
                                        data : this.waiting_cancel
                                    }
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });                        
                    }else if(this.term == 'week'){
                        Axios.get('/api/dlvy/statistics/waitcancel/'+this.mode+'/'+this.term+'/'+this.getFormDate(this.attributes[0].dates.start))
                        .then((response) => {
                            this.waiting_status_date = response.data.date_info;
                            this.waiting_complete = response.data.wait_count;
                            this.waiting_cancel = response.data.wait_cancel;
                            this.datacollection = {
                                labels : this.waiting_status_date,
                                datasets: [
                                    {
                                        label : '대기 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.waiting_complete
                                    },
                                    {
                                        label : '대기 취소 건 수',
                                        backgroundColor : '#2E2EFE',
                                        data : this.waiting_cancel
                                    }
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }else if(this.term == 'month'){
                        var parse_month = this.selected_month.split('/'); 
                        Axios.get('/api/dlvy/statistics/waitcancel/'+this.mode+'/'+this.term+'/'+this.getFormDate(new Date(parse_month[0], parse_month[1]-1, 1)))
                        .then((response) => {
                            this.waiting_status_date = response.data.date_info;
                            this.waiting_complete = response.data.wait_count;
                            this.waiting_cancel = response.data.wait_cancel;
                            this.datacollection = {
                                labels : this.waiting_status_date,
                                datasets: [
                                    {
                                        label : '대기 완료 건 수',
                                        backgroundColor: '#f87979',
                                        data : this.waiting_complete
                                    },
                                    {
                                        label : '대기 취소 건 수',
                                        backgroundColor : '#2E2EFE',
                                        data : this.waiting_cancel
                                    }
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }
                }else if(this.categori == "평균 대기 시간"){
                    if(this.term=='day'){
                        Axios.get('/api/dlvy/statistics/waittimeavg/'+this.term+'/'+this.getFormDate(this.date))
                        .then((response) => {
                            this.wait_time_avg_date = response.data.date_info;
                            this.wait_time_avg = response.data.statis_info;
                            this.datacollection = {
                                labels : this.wait_time_avg_date,
                                datasets: [
                                    {
                                        label : '평균 대기 시간',
                                        backgroundColor: '#f87979',
                                        data : this.wait_time_avg
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });                        
                    }else if(this.term == 'week'){
                        Axios.get('/api/dlvy/statistics/waittimeavg/'+this.term+'/'+this.getFormDate(this.attributes[0].dates.start))
                        .then((response) => {
                            this.wait_time_avg_date = response.data.date_info;
                            this.wait_time_avg = response.data.statis_info;
                            this.datacollection = {
                                labels : this.wait_time_avg_date,
                                datasets: [
                                    {
                                        label : '평균 대기 시간',
                                        backgroundColor: '#f87979',
                                        data : this.wait_time_avg
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }else if(this.term == 'month'){
                        var parse_month = this.selected_month.split('/');
                        Axios.get('/api/dlvy/statistics/waittimeavg/'+this.term+'/'+this.getFormDate(new Date(parse_month[0], parse_month[1]-1, 1))) 
                        .then((response) => {
                            this.wait_time_avg_date = response.data.date_info;
                            this.wait_time_avg = response.data.statis_info;
                            this.datacollection = {
                                labels : this.wait_time_avg_date,
                                datasets: [
                                    {
                                        label : '평균 대기 시간',
                                        backgroundColor: '#f87979',
                                        data : this.wait_time_avg
                                    },
                                ]
                            }
                        })
                        .catch(error => {
                            console.log(error);
                        });
                    }
                }
            }
        },
        watch : {      //change input value when user select date
            attributes : {
                deep: true,
                handler(){
                    this.input_text = this.attributes[0].dates.start.getFullYear()+'.'+(this.attributes[0].dates.start.getMonth()+1)+'.'+
                                      this.attributes[0].dates.start.getDate()+" ~ "+ this.attributes[0].dates.end.getFullYear()+'.'+
                                      (this.attributes[0].dates.end.getMonth()+1)+'.'+this.attributes[0].dates.end.getDate();
                }
                
            }
        }
    }
</script>

<style scoped>

.container{          /* left, right container */
    display:grid;
    grid-template-columns: 22% 78%;
    width: 100%;
    max-width:1500px;
}

.left_container_item1{ /* category */
    width: 70%;
    margin-top: 220px;
}

.right_container{ 
    display:grid;
    grid-template-columns: 10% 80%;
    grid-template-rows: 5% 95%;
    border-style: solid;
    border-width: 1px;
    border-color: #E6E9ED;
    padding: 10px;
    margin-top: 20px;
    height: 660px;
}

.right_container_item1{   /* margin */
    display:grid;
    grid-template-columns: 13% 80%;
    grid-column-start: 1;
    grid-column-end: 4;
   
}

.right_container_item1_left{
    border-bottom:2px solid #E6E9ED;
    margin-bottom: 50px;
}

.right_container_item2{   /*chart header, buttons, date picker ,body  */
    display:grid;
    grid-template-columns: 100%;
    grid-template-rows: 18% 15% 67%;
    grid-column-start: 1;
    grid-column-end: 4;
    font-size: 2.2em;
    margin-left: 20px;
    margin-top: 40px;

}

.right_container_item2_header{  /*chart header */
    display: grid;
    grid-column-start: 1;
    grid-column-end: 4;
    text-align: center;
    font-size: 1em;
    color:#716A6A;
    margin-right: 25px;
    margin-bottom: 5px;
}

.right_container_item2_header2{ /*chart buttons, date picker */
    display: grid;
    grid-column-start: 1;    
    grid-column-end: 4;
    grid-template-columns: 33% 33% 33%;

}

.right_container_item2_item1{ /* chart Accumulation, average button */
    margin-left: 210px;
}

.right_container_item2_item2{ /* date picker */
    margin-left: 5px;
}
.right_container_item2_item3{ /* chart day, week, month button */
    margin-left: 50px;
}

.button{  /* category buttons */
    color : black;
    background-color : white;
    border : none;
    margin-top : 5%;
    
}
 
.chart_button{ 
    font-size: 0.4em;
    
}
</style>