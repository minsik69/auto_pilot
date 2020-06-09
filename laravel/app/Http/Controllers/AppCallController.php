<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;

// call Related Controller
class AppCallController extends Controller
{
    // click to call
    public function call(){
        $station_all = DB::table('station')
                        ->get();
        
        return response()->json(['station_all'=>$station_all]);
    }

    // user check (different people with the same name)
    public function check_user($id){

        $same_user = DB::table('user')
                        ->select('user_id','user_name','user_phone')
                        ->where('user_name', $id)
                        ->get();

        return response($same_user);
    }
    // show check point
    public function dlvy_checkpoint($start_point, $end_point){
        $path_id= DB::table('path')
                    ->where('path.path_start_point', $start_point)
                    ->where('path.path_end_point',$end_point)
                    ->value('path_id');

        return $checkpoint = DB::table('checkpoint')
            ->select('checkpoint_id','checkpoint_lat', 'checkpoint_lon')
            ->join('path_check', 'checkpoint.checkpoint_id', '=','path_check.check_id')
            ->where('path_col_id', $path_id)
            ->get();
    }
}
