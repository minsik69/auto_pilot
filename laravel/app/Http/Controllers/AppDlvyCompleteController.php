<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;

// Show completed delivery Controller
class AppDlvyCompleteController extends Controller
{
    public function completed_dlvy($id, $term='default', $date_start='0', $date_end='0'){
        if($date_start == '0'){
            $date_start= date('Y-m-d');
            $date_end = date('Y-m-d');
            if($term == 'day'){
                $date_end = date('Y-m-d');
            }elseif($term == 'week'){
                $date_end = date('Y-m-d', strtotime("$date_start-6 day"));
            }elseif($term == 'month'){
                $date_end = date('Y-m-d', strtotime("$date_start-1 month"));
            }elseif($term == '6month'){
                $date_end = date('Y-m-d', strtotime("$date_start-6 month"));
            }
        }else{
            $date_start = $date_start;
            $date_end = $date_end;
        }
        // if term is all, show delivery everything to that user
        if($term=='all'){
            $completed_send_dlvy = DB::table('dlvy as d')
                            ->select('d.dlvy_num', 'd.dlvy_date', 'd.dlvy_status', 'd.dlvy_start_point', 'dlvy_end_point', 'u.user_name as receiver_name')
                            ->LeftJoin('user as u','d.dlvy_receiver','=','u.user_id')
                            ->where('dlvy_sender', $id)
                            ->where('dlvy_status', '배달완료')
                            ->orderBy('dlvy_date', 'desc')
                            ->orderBy('dlvy_num', 'desc')
                            ->get();
            $completed_receive_dlvy = DB::table('dlvy as d')
                            ->select('d.dlvy_num', 'd.dlvy_date', 'd.dlvy_status', 'd.dlvy_start_point', 'dlvy_end_point', 'u.user_name as sender_name')
                            ->LeftJoin('user as u','d.dlvy_sender','=','u.user_id')
                            ->where('dlvy_receiver', $id)
                            ->where('dlvy_status', '배달완료')
                            ->orderBy('dlvy_date', 'desc')
                            ->orderBy('dlvy_num', 'desc')
                            ->get();
        }
        // if term is day,week,month,6month, show delivery during the period to that user
        else{    
            $completed_send_dlvy = DB::table('dlvy as d')
                               ->select('d.dlvy_num', 'd.dlvy_date', 'd.dlvy_status', 'd.dlvy_start_point', 'dlvy_end_point', 'u.user_name as receiver_name')
                               ->LeftJoin('user as u','d.dlvy_receiver','=','u.user_id')
                               ->where('d.dlvy_sender', $id)
                               ->where('d.dlvy_status', '배달완료')
                               ->whereBetween('d.dlvy_date', [$date_end, $date_start])
                               ->orderBy('d.dlvy_date', 'desc')
                               ->orderBy('d.dlvy_num', 'asc')
                               ->get();

            $completed_receive_dlvy = DB::table('dlvy as d')
                            ->select('d.dlvy_num', 'd.dlvy_date', 'd.dlvy_status', 'd.dlvy_start_point', 'dlvy_end_point', 'u.user_name as sender_name')
                            ->LeftJoin('user as u','d.dlvy_sender','=','u.user_id')
                            ->where('d.dlvy_receiver', $id)
                            ->where('d.dlvy_status', '배달완료')
                            ->whereBetween('d.dlvy_date', [$date_end, $date_start])
                            ->orderBy('d.dlvy_date', 'desc')
                            ->orderBy('d.dlvy_num', 'asc')
                            ->get();
        }
 
        $completed_dlvy = array();
        $re_count=0;
        $sen_count=0;
        // input one variable and sort to date and delivery number
        while(TRUE){
            if(isset($completed_receive_dlvy[$re_count]) && isset($completed_send_dlvy[$sen_count])){
                if($completed_send_dlvy[$sen_count]->dlvy_date == $completed_receive_dlvy[$re_count]->dlvy_date){
                    if($completed_send_dlvy[$sen_count]->dlvy_num > $completed_receive_dlvy[$re_count]->dlvy_num){
                        array_push($completed_dlvy, $completed_send_dlvy[$sen_count]);
                        $sen_count = $sen_count+1;
                    }else{
                        array_push($completed_dlvy, $completed_receive_dlvy[$re_count]);
                        $re_count = $re_count+1;
                    }
                }
                elseif($completed_send_dlvy[$sen_count]->dlvy_date > $completed_receive_dlvy[$re_count]->dlvy_date){
                    array_push($completed_dlvy, $completed_send_dlvy[$sen_count]);
                    $sen_count = $sen_count+1;
                }else{
                    array_push($completed_dlvy, $completed_receive_dlvy[$re_count]);
                    $re_count = $re_count+1;
                }
            }elseif(!isset($completed_receive_dlvy[$re_count]) && isset($completed_send_dlvy[$sen_count])){
                array_push($completed_dlvy, $completed_send_dlvy[$sen_count]);
                $sen_count = $sen_count+1;
            }elseif(isset($completed_receive_dlvy[$re_count]) && !isset($completed_send_dlvy[$sen_count])){
                array_push($completed_dlvy, $completed_receive_dlvy[$re_count]);
                $re_count = $re_count+1;
            }elseif(!isset($completed_receive_dlvy[$re_count]) && !isset($completed_send_dlvy[$sen_count])){
                break;
            }
        }
        return response()->json([
            'completed_dlvy' => $completed_dlvy,
        ]);
    }
}