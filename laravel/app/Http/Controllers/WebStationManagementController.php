<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
// station manage controller
class WebStationManagementController extends Controller 
{    
    
    // page load
    public function index()
    {
        return response(['station_all'=> DB::table('station')->get()]);
    }

    // register to station
    public function store(Request $request)
    {
        DB::table('station')
                        ->insert(
                            ['station_name' => $request->station_name, 'station_lat'=>$request->station_lat, 'station_lon'=>$request->station_lon ]
                        );
        return response(['station_all'=> DB::table('station')->get()]);
    }

    // modify to station
    public function update(Request $request, $id)
    {
        DB::table('station')
                        ->where('station_name', $id)
                        ->update(['station_name'=>$request->station_name, 'station_lat' => $request->station_lat, 'station_lon' => $request->station_lon]);

        return response(['station_all'=> DB::table('station')->get()]);  
    }

    // delete to station
    public function destroy($id)
    {
        DB::table('station')
                        ->where('station_name',$id)
                        ->delete();
        
        return response(['station_all'=> DB::table('station')->get()]);  
    }
}
