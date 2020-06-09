<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;

// check point manage controller
class WebCheckPointManagementController extends Controller
{   

    // page load
    public function index()
    {
        return response(['checkpoint_all' => DB::table('checkpoint')->get()]);
    }

    // register to check point
    public function store(Request $request)
    {
        DB::table('checkpoint')
                        ->insert(
                            ['checkpoint_lat'=>$request->checkpoint_lat, 'checkpoint_lon'=>$request->checkpoint_lon ]
                        );

        return response(['checkpoint_all' => DB::table('checkpoint')->get()]);
    }

    // modify to check point
    public function update(Request $request, $id)
    {
        DB::table('checkpoint')
                        ->where('checkpoint_id', $id)
                        ->update(['checkpoint_lat' => $request->checkpoint_lat, 'checkpoint_lon' => $request->checkpoint_lon]);

        return response(['checkpoint_all' => DB::table('checkpoint')->get()]);
    }

    // delete to check point
    public function destroy($id)
    {
        $update_path_check = DB::table('path_check')
                        ->select('path_check_id', 'sequence')
                        ->where('check_id', $id)
                        ->get();

        for($i=0; $i<count($update_path_check); $i++){
            $update_path_col_id = DB::table('path_check')
                            ->where('path_check_id', (int)$update_path_check[$i]->path_check_id)
                            ->value('path_col_id');
            DB::table('path_check')
                            ->where('path_col_id', $update_path_col_id)
                            ->where('sequence', '>', (int)$update_path_check[$i]->sequence)
                            ->decrement('sequence');
            
        }
        DB::table('checkpoint')
                        ->where('checkpoint_id',$id)
                        ->delete();
        
        
        return response(['checkpoint_all' => DB::table('checkpoint')->get()]);
    }
}
