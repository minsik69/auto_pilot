<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
// path manage controller
class WebPathManagementController extends Controller
{
    // page load
    public function index()
    {
        return response()->json(['station_all'=>DB::table('station')->get(), 'checkpoint_all'=>DB::table('checkpoint')->get()]);
    }

    // register to path
    public function store(Request $request)
    {
        // register to path
        $path_id = DB::table('path')->insertGetId(
            ['path_start_point' => $request->path_start_point, 'path_end_point'=>$request->path_end_point],
        );
        // reverse and register to path
        $reverse_path_id =DB::table('path')->insertGetId(
            ['path_start_point'=>$request->path_end_point , 'path_end_point'=>$request->path_start_point ]   
        );

        $sequence = 1;
        // register to path_check
        for($i=0;$i<count($request->checkpoint_id); $i++){
            DB::table('path_check')->insert(
                ['path_col_id'=>$path_id, 'check_id'=>$request->checkpoint_id[$i], 'sequence'=>$sequence]
            );
            $sequence = $sequence+1;
        }
        // reverse to checkpoint sequence
        $reverse_checkpoint_id = array_reverse($request->checkpoint_id);

        $sequence = 1;
        // reverse and register to path_check
        for($i=0;$i<count($request->checkpoint_id); $i++){
            DB::table('path_check')->insert(
                ['path_col_id'=>$reverse_path_id, 'check_id'=>$reverse_checkpoint_id[$i], 'sequence'=>$sequence]
            );
            $sequence= $sequence+1;
        }

    }

    // click to station
    public function show($id)
    {
        return response(DB::table('path')->where('path_start_point', $id)->get());
    }

    // click to list
    public function show_path_check($id){
        return response(DB::table('path_check')->select('check_id','sequence')->where('path_col_id', $id)->orderBy('sequence','asc')->get());

    }

    // modify to path
    public function update(Request $request, $id)
    {


        $path_data = DB::table('path')
                        ->select('path_start_point', 'path_end_point')
                        ->where('path_id', $id)
                        ->first();

        $reverse_path_id = DB::table('path')
                        ->where('path_start_point', $path_data->path_end_point)
                        ->where('path_end_point', $path_data->path_start_point)
                        ->value('path_id');
        // Delete an existing path
        DB::table('path_check')->where('path_col_id', $id)->delete();
        DB::table('path_check')->where('path_col_id', $reverse_path_id)->delete();
        

        $sequence = 1;
        // register to path
        for($i=0;$i<count($request->checkpoint_id); $i++){
            DB::table('path_check')->insert(
                ['path_col_id'=>$id, 'check_id'=>$request->checkpoint_id[$i], 'sequence'=>$sequence]
            );
            $sequence = $sequence+1;
        }
        // reverse to checkpoint
        $reverse_checkpoint_id = array_reverse($request->checkpoint_id);
        
         $sequence = 1;
        // reverse and register to path_check
        for($i=0;$i<count($request->checkpoint_id); $i++){
            DB::table('path_check')->insert(
                ['path_col_id'=>$reverse_path_id, 'check_id'=>$reverse_checkpoint_id[$i], 'sequence'=>$sequence]
            );
            $sequence= $sequence+1;
        }

        $return_path_id = DB::table('path')
                        ->where('path_id', $id)
                        ->value('path_start_point');
        return response(DB::table('path')->where('path_start_point',$return_path_id)->get());
    }

    // delete to path
    public function destroy($id)
    {
        $path_data = DB::table('path')
                        ->select('path_start_point', 'path_end_point')
                        ->where('path_id', $id)
                        ->first();

        $reverse_path_id = DB::table('path')
                        ->where('path_start_point', $path_data->path_end_point)
                        ->where('path_end_point', $path_data->path_start_point)
                        ->value('path_id');
        DB::table('station')
        ->where('station_name',$id)
        ->delete();


        DB::table('path')->where('path_id', $id)->delete();
        DB::table('path')->where('path_id', $reverse_path_id)->delete();


        return response(DB::table('path')->where('path_start_point',$path_data->path_start_point)->get());
    }
}
