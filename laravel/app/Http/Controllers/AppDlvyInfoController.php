<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;

// In-delivery infomation Controller
class AppDlvyInfoController extends Controller
{
    // send delivery
    public function send_dlvy($id){
        $send_data=array();
        $dlvy_info = array();
        $dlvy_wait_num = array();
        $rc_gps =array();
        $station_start_gps = array();
        $station_end_gps = array();
        $j =0;


        $send_info = DB::table('dlvy')
                        ->select('dlvy_num','dlvy_status','dlvy_receiver', 'dlvy_start_point', 'dlvy_end_point','dlvy_car_num')
                        ->where('dlvy_sender', $id)
                        ->whereRaw('dlvy_date >= curdate()')
                        ->get();

        // When there is a delivery send by that user
        if(count($send_info)>0){
            for($i=0; $i<count($send_info); $i++){
                if($send_info[$i]->dlvy_status == "배달중" || $send_info[$i]->dlvy_status == "호출중" || $send_info[$i]->dlvy_status == "대기중"){
                    $send_data[$j] = $send_info[$i];
                    $j = $j+1;
                }
            }
            // if sended delivery is all completed
            if(count($send_data)<=0){
                return response()->json([
                    'value' => 'null'
                ]);
            }

            for($i=0; $i<count($send_data); $i++){
            
                // delivery number, delivery stat
                $dlvy_info_data = DB::table('dlvy')
                            ->select('dlvy_num', 'dlvy_status')
                            ->where('dlvy_num',  $send_data[$i]->dlvy_num)
                            ->first();
                $dlvy_info[$i] = $dlvy_info_data;

                // waiting number (if not waiting, waiting number is null)
                if($dlvy_info_data->dlvy_status == "대기중"){
                    $dlvy_wait_num_data = DB::table('dlvy')
                                ->whereNotNull('dlvy_wait_start')
                                ->whereRaw('dlvy_date >= curdate()')
                                ->where('dlvy_status', '대기중')
                                ->where('dlvy_num','<',$send_data[$i]->dlvy_num)
                                ->count();
                    $dlvy_wait_num[$i] = $dlvy_wait_num_data+1;
                }else{
                    $dlvy_wait_num[$i] = 0;
                }
                
                // receiver name
                $user_name_data = DB::table('user')
                            ->where('user_id', $send_data[$i]->dlvy_receiver)
                            ->value('user_name');
                $user_name[$i] = $user_name_data;
               
                // gps of current rc car
                $rc_gps_data = DB::table('car')
                            ->select('car_num','car_lat', 'car_lon')
                            ->where('car_num', $send_data[$i]->dlvy_car_num)
                            ->first();
                // If the rc car is not in running         
                if(!$rc_gps_data){
                    $rc_gps_data = array();
                    $rc_gps_data['car_num']=0;
                    $rc_gps_data['car_lat'] = 0;
                    $rc_gps_data['car_lon'] = 0;
                }
                $rc_gps[$i] = $rc_gps_data;
                
            
                // start point name, start point gps
                $station_start_data = DB::table('station')
                            ->select('station_name', 'station_lat', 'station_lon')
                            ->where('station_name', $send_data[$i]->dlvy_start_point)
                            ->first();
                $station_start[$i] = $station_start_data;
                // end point name, ena point gps
                $station_end_data = DB::table('station')
                            ->select('station_name','station_lat', 'station_lon')
                            ->where('station_name', $send_data[$i]->dlvy_end_point)
                            ->first();
                $station_end[$i] = $station_end_data;
            }

            return response()->json([
                'dlvy_info'=> $dlvy_info,  
                'dlvy_wait_num' => $dlvy_wait_num, 
                'user_name' => $user_name, 
                'rc_gps' => $rc_gps,     
                'station_start' => $station_start, 
                'station_end' =>$station_end,   

            ]);    
        }
        // if no have sending delivery
        else{
            return response()->json([
                'value' => 'null'
            ]);
        }  
       
    }

    // receive delivery
    public function receiv_dlvy($id){
        $receive_data=array();
        $dlvy_info = array();
        $dlvy_wait_num = array();
        $rc_gps =array();
        $station_start_gps = array();
        $station_end_gps = array();
        $user_name = array();
        $j =0;

        $receive_info = DB::table('dlvy')
                        ->select('dlvy_num','dlvy_status', 'dlvy_sender', 'dlvy_start_point', 'dlvy_end_point','dlvy_car_num')
                        ->where('dlvy_receiver', $id)
                        ->whereRaw('dlvy_date >= curdate()')
                        ->get();

        // When there is a delivery receive to that user
        if(count($receive_info)>0){
            for($i=0; $i<count($receive_info); $i++){
                if($receive_info[$i]->dlvy_status == "배달중" || $receive_info[$i]->dlvy_status == "호출중" || $receive_info[$i]->dlvy_status == "대기중"){
                    $receive_data[$j] = $receive_info[$i];
                    $j = $j+1;
                }
            }
            if(count($receive_data)<=0){
                return response()->json([
                    'value' => 'null'
                ]);
            }
            for($i=0; $i<count($receive_data); $i++){
                // delivery number, delivery stat
                $dlvy_info_data = DB::table('dlvy')
                            ->select('dlvy_num', 'dlvy_status')
                            ->where('dlvy_num',  $receive_data[$i]->dlvy_num)
                            ->first();
                $dlvy_info[$i] = $dlvy_info_data;

                // waiting number (if not waiting, waiting number is null)
                if($dlvy_info_data->dlvy_status == "대기중"){
                    $dlvy_wait_num_data = DB::table('dlvy')
                                ->whereNotNull('dlvy_wait_start')
                                ->whereRaw('dlvy_date >= curdate()')
                                ->where('dlvy_status', '대기중')
                                ->where('dlvy_num','<',$receive_data[$i]->dlvy_num)
                                ->count();
                    $dlvy_wait_num[$i] = $dlvy_wait_num_data+1;
                }else{
                    $dlvy_wait_num[$i] = 0;
                }

                // sender name
                $user_name_data = DB::table('user')
                            ->where('user_id', $receive_data[$i]->dlvy_sender)
                            ->value('user_name');
                $user_name[$i] = $user_name_data;

                // gps of current rc car
                $rc_gps_data = DB::table('car')
                            ->select('car_num','car_lat', 'car_lon')
                            ->where('car_num', $receive_data[$i]->dlvy_car_num)
                            ->first();
                // If the rc car is not in running 
                if(!$rc_gps_data){
                    $rc_gps_data = array();
                    $rc_gps_data['car_num']=0;
                    $rc_gps_data['car_lat'] = 0;
                    $rc_gps_data['car_lon'] = 0;
                }
                $rc_gps[$i] = $rc_gps_data;
                
                // start point name, start point gps
                $station_start_data = DB::table('station')
                            ->select('station_name', 'station_lat', 'station_lon')
                            ->where('station_name', $receive_data[$i]->dlvy_start_point)
                            ->first();
                $station_start[$i] = $station_start_data;

                // end point name, ena point gps
                $station_end_data = DB::table('station')
                            ->select('station_name','station_lat', 'station_lon')
                            ->where('station_name', $receive_data[$i]->dlvy_end_point)
                            ->first();
                $station_end[$i] = $station_end_data;
            }
            return response()->json([
                'dlvy_info'=> $dlvy_info,   
                'dlvy_wait_num' => $dlvy_wait_num,  
                'user_name' => $user_name, 
                'rc_gps' => $rc_gps,       
                'station_start' => $station_start, 
                'station_end' =>$station_end,      
            ]);        
        }
        // If there is no incoming delivery
        else{
            return response()->json([
                'value' => 'null'
            ]);
        }
    }
}