<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
// Web Control Page Controller
class WebControlController extends Controller
{
    
    # Distribute all information when loading the control page
    public function index(){
        # last week
        $dt=date('Y-m-d');
        $week_first = strtotime($dt) - (date('w',strtotime($dt)) * 86400); 
        $week_end = $week_first + (6*86400); 
        $last_week_start = date('Y-m-d', $week_first - (86400 * (7)));
        $last_week_end = date('Y-m-d', $week_end - (86400 * (7)));
        # last month
        $d = mktime(0,0,0, date("m"), 1, date("Y")); 
        $prev_month = strtotime("-1 month", $d); 
        $last_month_start = date("Y-m-01", $prev_month); 
        $last_month_end = date("Y-m-t", $prev_month); 
        $month_count = date("t",$prev_month);  
       
        // Present to state of operation
        # in service rc car
        $proceeding_rc = DB::table('car')   
                        ->select('car_num')
                        ->where('car_status', '호출중')
                        ->orwhere('car_status', '배달중')
                        ->count();   
        # Operable rc car
        $waiting_rc = DB::table('car')   
                    ->select('car_num')
                    ->where('car_status', '배달대기')
                    ->count();
        # Error rc car
        $error_rc = DB::table('car')    
                    ->select('car_num')
                    ->where('car_status', '오류')
                    ->count();
        
        // Present to delivery status
        # Total Calls Last Month
        $call_count_month_ago = DB::table('dlvy')
                        ->whereBetween('dlvy_date', [$last_month_start,$last_month_end])
                        ->count('dlvy_num');
        # Average number of calls last month
        $call_avg_month_ago = $call_count_month_ago / $month_count;

        # Total number of calls today
        $entire_call = DB::table('dlvy')   
                        ->select('dlvy_num')
                        ->whereRaw('dlvy_date >= curdate()')
                        ->count();
        # Number of deliveries completed today
        $complete_call = DB::table('dlvy')  
            ->select('dlvy_num')
            ->whereRaw('dlvy_date >= curdate()')
            ->where('dlvy_status','=',  '배달완료')
            ->count();
                        
        // Standby Cancellation Status     
        # Number of queues completed for delivery today
        $complete_waiting = DB::table('dlvy')  
                        ->select('dlvy_num')
                        ->whereRaw('dlvy_date >= curdate()')
                        ->whereNotNull('dlvy_wait_time')
                        ->count();
        # Current Queued Count
        $now_waiting = DB::table('dlvy')  
                        ->select('dlvy_num')
                        ->whereRaw('dlvy_date >= curdate()')
                        ->where('dlvy_status', '=', '대기중')
                        ->count();

        # Today's Wait Cancellation Count
        $canceled_waiting = DB::table('dlvy')   
                        ->select('dlvy_num')
                        ->whereRaw('dlvy_date >= curdate()')
                        ->where('dlvy_status', '=','대기취소')
                        ->count();
                        
        // Last week's Calling Building Rank
        $build_rank_and_count = DB::table('dlvy')
                        ->select('station.station_name as station')
                        ->selectRaw('count(*) as call_count')
                        ->leftJoin('station', 'station.station_name', '=','dlvy.dlvy_start_point')
                        ->whereNotNull('dlvy_start_point')
                        ->whereBetween('dlvy_date', [$last_week_start,$last_week_end])
                        ->groupBy('station')
                        ->orderBy('call_count', 'desc')
                        ->limit(3)
                        ->get();
        $build_rank = [];
        for($i=0; $i<count($build_rank_and_count); $i++){
            $build_rank[$i] = $build_rank_and_count[$i]->station;
        }
        // Current Average Latency
        # Average Latency Today
        $avg_waiting_time = DB::table('dlvy')  
                        ->whereRaw('dlvy_date >= curdate()')
                        ->whereNotNull('dlvy_wait_time')
                        ->avg('dlvy_wait_time');
        # Total Latency Last Month
        $sum_waiting_time_month_ago = DB::table('dlvy')
                        ->whereBetween('dlvy_date', [$last_month_start,$last_month_end])
                        ->sum('dlvy_wait_time');
        # Total number of queues last month
        $count_waiting_time_month_ago = DB::table('dlvy')
                        ->whereBetween('dlvy_date', [$last_month_start,$last_month_end])
                        ->whereNotNull('dlvy_wait_time')
                        ->count();   

        # Average Latency Last Month
        $avg_waiting_time_month_ago = $sum_waiting_time_month_ago / $count_waiting_time_month_ago;
        
        // Map - station location and name, RC car location and name
        $map_car_status = DB::table('car')
                        ->select('*')
                        ->get();

        $station_info = DB::table('station')
                        ->select('*')
                        ->get();


        return response()->json([
            # Present to state of operation
            'proceeding_rc'=>$proceeding_rc, # in service rc car
            'waiting_rc'=>$waiting_rc, # Operable rc car
            'error_rc' =>$error_rc, # Error rc car
            # Present to delivery status
            'call_avg_month_ago' => $call_avg_month_ago, # Average number of calls last month
            'entire_call' => $entire_call,    # Total number of calls today
            'complete_call' => $complete_call, # Number of deliveries completed today
            # Standby Cancellation Status 
            'complete_waiting'=> $complete_waiting, # Number of queues completed for delivery today
            'now_waiting' => $now_waiting, # Current Queued Count
            'canceled_waiting' => $canceled_waiting,    # Today's Wait Cancellation Count
            # Last week's Calling Building Rank
            'build_rank' => $build_rank,
            # Current Average Latency
            'avg_waiting_time_month_ago' => $avg_waiting_time_month_ago, # Average Latency Last Month
            'avg_waiting_time' => $avg_waiting_time,    # Average Latency Today
            # Map - stop, RC car location, name display
            'map_car_status' => $map_car_status,    # rc car information
            'station_info' => $station_info,       # station information

        ]);
    }
    // Driving information when you click the marker of the rc car on the map
    public function run_status($id){

        # RC car id, RC car name, RC car state, RC car lines of longitude and latitude 
        $car = DB::table('car') 
                        ->select('*')
                        ->where('car_num', $id)
                        ->first();
        # if on delivery  rc car
        if($car->car_status ==="배달중" || $car->car_status==="호출중" ){

            # Error, Call time, departure time to end point by start point
            $dlvy_status = DB::table('dlvy') 
                            ->select('dlvy_call_start', 'dlvy_start')
                            ->where('dlvy_status', $car->car_status)
                            ->where('dlvy_car_num', $id)
                            ->first();
            #  start point, start point lines of longitude and latitude 
            $dlvy_start_point = DB::table('station')
                            ->select('station.station_name', 'station.station_lat','station.station_lon')
                            ->join('dlvy', 'station.station_name', '=','dlvy.dlvy_start_point')
                            ->where('dlvy_status', $car->car_status)
                            ->where('dlvy_car_num', $id)
                            ->first();

            # end point, end point lines of longitude and latitude 
            $dlvy_end_point = DB::table('station')
                            ->select('station.station_name', 'station.station_lat','station.station_lon')
                            ->join('dlvy', 'station.station_name', '=','dlvy.dlvy_end_point')
                            ->where('dlvy_status', $car->car_status)
                            ->where('dlvy_car_num', $id)
                            ->first();

            # sender, receiver information
            $dlvy_status_s_r = DB::table('dlvy') 
                            ->select('dlvy_sender', 'dlvy_receiver')
                            ->where('dlvy_status', $car->car_status)
                            ->where('dlvy_car_num', $id)
                            ->first();
            # sender name and phone number
            $sender_info = DB::table('user')    
                            ->select('user_name', 'user_phone')
                            ->where('user_id', $dlvy_status_s_r->dlvy_sender)
                            ->first();

            # receiver name and phone number
            $receiver_info = DB::table('user')  
                            ->select('user_name', 'user_phone')
                            ->where('user_id', $dlvy_status_s_r->dlvy_receiver)
                            ->first();
            

            return response()->json([
                'car' => $car, 
                'dlvy_status' => $dlvy_status, 
                'dlvy_start_point' => $dlvy_start_point,  
                'dlvy_end_point' => $dlvy_end_point,       
                'sender_info' => $sender_info, 
                'receiver_info'=>$receiver_info 
            ]);
        }
        # if on standby delivery rc car or error rc car
        else{
            return response()->json($car); 
        }
    }
    // change to marker image
    public function inquireRc(){
        $car = DB::table('car')
                        ->select('*')
                        ->get();
        return response()->json($car);
    }
}