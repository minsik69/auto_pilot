<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
use Carbon\Carbon;

// Web statistics controller
class WebStatisticsController extends Controller
{
    # Delivery Completion Count Delivery Completion Accumulated/Average
    public function divy_complete($mode="acc", $term="day", $date='0'){
        $statis_info =array();
        $date_info = array();

        $dt = date('Y-m-d', strtotime($date));    
        $mt = date('Y-m', strtotime($date));    
        $week_first = strtotime($dt) - (date('w',strtotime($dt)) * 86400);  
        $week_end = $week_first + (6*86400); 

        // click to accumulated
        if($mode == "acc"){     # 누적 클릭 시
            // click to day
            if($term == 'day'){       
                for($i=0; $i<9; $i++)
                { 
                    $day = date('Y-m-d', strtotime("$dt -$i day"));

                    $statis_data = DB::table('dlvy')
                                    ->where('dlvy_date', $day)
                                    ->count('dlvy_num');
                    $date_info[$i] = $day;
                    $statis_info[$i] = $statis_data;       
                }
            }
            // click to week
            elseif($term == 'week'){   
                for($i=0; $i<9; $i++)
                {
                    $last_week_start = date('Y-m-d', $week_first - (86400 * (7*$i)));
                    $last_week_end = date('Y-m-d', $week_end - (86400 * (7*$i)));

                    $statis_data = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->count('dlvy_num');
                    $date_info[$i] = "$last_week_start~$last_week_end";
                    $statis_info[$i] = $statis_data;
                }
            }
            // click to month
            elseif($term=='month'){   
                for($i=0; $i<9; $i++)
                {
                    $d = mktime(0,0,0, date("m"), 1, date("Y")); 
                    $prev_month = strtotime("$mt -$i month",$d); 
                    $last_month_start = date("Y-m-01", $prev_month);  
                    $last_month_end = date("Y-m-t", $prev_month); 
        
                    $statis_data = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->count('dlvy_num');
                    $date_info[$i] = date('Y-m', $prev_month);
                    $statis_info[$i] = $statis_data;
                }
            }
        }
        // click to average
        elseif($mode == "avg"){
            // click to week
            if($term == 'week'){        
                for($i=0; $i<9; $i++)
                {
                    $last_week_start = date('Y-m-d', $week_first - (86400 * (7*$i)));
                    $last_week_end = date('Y-m-d', $week_end - (86400 * (7*$i)));

                    $statis_data = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->count('dlvy_num');
                    $date_info[$i] = "$last_week_start~$last_week_end";
                    $statis_info[$i] = round($statis_data / 7, 2);
                }
            }
            // click to month
            elseif($term=='month'){    
                for($i=0; $i<9; $i++)
                {
                    $d = mktime(0,0,0, date("m"), 1, date("Y")); 
                    $prev_month = strtotime("$mt -$i month",$d); 
                    $last_month_start = date("Y-m-01", $prev_month);
                    $last_month_end = date("Y-m-t", $prev_month);   
                    $month_count = date("t",$prev_month);

                    $statis_data = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->count('dlvy_num');

                    $date_info[$i] = date('Y-m', $prev_month);
                    $statis_info[$i] = round($statis_data / $month_count, 2);
                } 
            }
        }else{ 
            return response(['msg'=>'잘못된 요청']);
        }
        $statis_info = array_reverse($statis_info);
        $date_info = array_reverse($date_info);
        
        return response([
            'date_info'=>$date_info,       
            'statis_info'=>$statis_info,   
            
        ]);
    }


    # Standby completed/Cancellation
    public function wait_and_cancle($mode="acc", $term="day", $date='0'){
        $wait_count =array();
        $wait_cancel = array();
        $date_info = array();
   
        $dt = date('Y-m-d', strtotime($date)); 
        $mt = date('Y-m', strtotime($date));   
        $week_first = strtotime($dt) - (date('w',strtotime($dt)) * 86400); 
        $week_end = $week_first + (6*86400);   

        // click to accumulated
        if($mode == "acc"){ 
            // click to day
            if($term == 'day'){       
                for($i=0; $i<9; $i++)
                { 
                    $day = date('Y-m-d', strtotime("$dt -$i day"));
                    $statis_data_count = DB::table('dlvy')
                                    ->where('dlvy_date', $day)
                                    ->count('dlvy_wait_time');
                    $statis_data_cancel = DB::table('dlvy')
                                    ->where('dlvy_date', $day)
                                    ->where('dlvy_status', '대기취소')
                                    ->count('dlvy_status');
                    $date_info[$i] = $day;
                    $wait_count[$i] = $statis_data_count;  
                    $wait_cancel[$i] = $statis_data_cancel;
                }
            }
            // click to week
            elseif($term == 'week'){   
                for($i=0; $i<9; $i++)
                {
                    $last_week_start = date('Y-m-d', $week_first - (86400 * (7*$i)));
                    $last_week_end = date('Y-m-d', $week_end - (86400 * (7*$i)));

                    $statis_data_count = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->count('dlvy_wait_time');
                    $statis_data_cancel = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->where('dlvy_status', '대기취소')
                                ->count('dlvy_status');
                    $date_info[$i] = "$last_week_start~$last_week_end";
                    $wait_count[$i] = $statis_data_count;  
                    $wait_cancel[$i] = $statis_data_cancel;
                }
            }
            // click to month
            elseif($term=='month'){    
                for($i=0; $i<9; $i++)
                {
                    $d = mktime(0,0,0, date("m"), 1, date("Y")); 
                    $prev_month = strtotime("$mt -$i month",$d); 
                    $last_month_start = date("Y-m-01", $prev_month); 
                    $last_month_end = date("Y-m-t", $prev_month);   
        
                    $statis_data_count = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->count('dlvy_wait_time');
                    $statis_data_cancel = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->where('dlvy_status', '대기취소')
                                ->count('dlvy_status');
                    $date_info[$i] = date('Y-m', $prev_month);
                    $wait_count[$i] = $statis_data_count;  
                    $wait_cancel[$i] = $statis_data_cancel;
                }
            }
        }
        // click to average
        elseif($mode == "avg"){ 
            // click to week
            if($term == 'week'){ 
                for($i=0; $i<9; $i++)
                {
                    $last_week_start = date('Y-m-d', $week_first - (86400 * (7*$i)));
                    $last_week_end = date('Y-m-d', $week_end - (86400 * (7*$i)));

                    $statis_data_count = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->count('dlvy_wait_time');
                    $statis_data_cancel = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                                ->where('dlvy_status', '대기취소')
                                ->count('dlvy_status');
                    $date_info[$i] = "$last_week_start~$last_week_end";
                    $wait_count[$i] = round($statis_data_count / 7, 2);  
                    $wait_cancel[$i] = round($statis_data_cancel / 7, 2);
                }
            }
            // click to month
            elseif($term=='month'){  
                for($i=0; $i<9; $i++)
                {
                    $d = mktime(0,0,0, date("m"), 1, date("Y"));
                    $prev_month = strtotime("$mt -$i month",$d);
                    $last_month_start = date("Y-m-01", $prev_month);
                    $last_month_end = date("Y-m-t", $prev_month);   
                    $month_count = date("t",$prev_month);

                    $statis_data_count = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->count('dlvy_wait_time');
                    $statis_data_cancel = DB::table('dlvy')
                                ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                                ->where('dlvy_status', '대기취소')
                                ->count('dlvy_status');
                    $date_info[$i] = date('Y-m', $prev_month);
                    $wait_count[$i] = round($statis_data_count / 7, 2);  
                    $wait_cancel[$i] = round($statis_data_cancel / 7, 2);
                } 
            }
        }else{
            return response(['msg'=>'잘못된 요청']);
        }
        $date_info = array_reverse($date_info);
        $wait_count = array_reverse($wait_count);
        $wait_cancel = array_reverse($wait_cancel);

        return response([
            'date_info'=>$date_info,        
            'wait_count'=>$wait_count,     
            'wait_cancel'=>$wait_cancel,   
        ]);
    }

    # average latency
    public function wait_time_avg($term="day", $date='0'){
        $statis_info =array();
        $date_info = array();

        $dt = date('Y-m-d', strtotime($date)); 
        $mt = date('Y-m', strtotime($date));   
        $week_first = strtotime($dt) - (date('w',strtotime($dt)) * 86400); 
        $week_end = $week_first + (6*86400);  

        // click to day
        if($term == 'day'){   
            for($i=0; $i<9; $i++)
            { 

                $day = date('Y-m-d', strtotime("$dt -$i day"));

                $statis_data = DB::table('dlvy')
                                ->selectRaw('sum(dlvy_wait_time) as sum, count(dlvy_wait_time) as count')
                                ->where('dlvy_date', $day)
                                ->first();
                $date_info[$i] = $day;
                $statis_info[$i] = $statis_data->count ? $statis_data->sum / $statis_data->count : 0;          
            }
        }
        // click to week
        elseif($term == 'week'){ 
            for($i=0; $i<9; $i++)
            {
                $last_week_start = date('Y-m-d', $week_first - (86400 * (7*$i)));
                $last_week_end = date('Y-m-d', $week_end - (86400 * (7*$i)));

                $statis_data = DB::table('dlvy')
                            ->selectRaw('sum(dlvy_wait_time) as sum, count(dlvy_wait_time) as count')
                            ->whereBetween('dlvy_date', [$last_week_start, $last_week_end])
                            ->first();
                $date_info[$i] = "$last_week_start~$last_week_end";
                $statis_info[$i] = $statis_data->count >0 ? $statis_data->sum / $statis_data->count : 0;   
            }
        }
        // click to month
        elseif($term=='month'){   
            for($i=0; $i<9; $i++)
            {
                $d = mktime(0,0,0, date("m"), 1, date("Y")); 
                $prev_month = strtotime("$mt -$i month",$d); 
                $last_month_start = date("Y-m-01", $prev_month); 
                $last_month_end = date("Y-m-t", $prev_month);  
        
                $statis_data = DB::table('dlvy')
                            ->selectRaw('sum(dlvy_wait_time) as sum, count(dlvy_wait_time) as count')
                            ->whereBetween('dlvy_date', [$last_month_start, $last_month_end])
                            ->first();
                $date_info[$i] = date('Y-m', $prev_month);
                $statis_info[$i] = $statis_data->count >0 ? $statis_data->sum / $statis_data->count : 0;   
            }
        }
        $statis_info = array_reverse($statis_info);
        $date_info = array_reverse($date_info);
        
        return response([
            'date_info'=>$date_info,       
            'statis_info'=>$statis_info,   
            
        ]);
    }
    
}