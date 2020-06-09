<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
// RC car manage controller
class WebCarManagementController extends Controller
{   

    // page load
    public function index()
    {
        return response(['car_all'=> DB::table('car')->select('car_num','car_name')->get()]);
    }

    // register to rc car
    public function store(Request $request)
    {
        DB::table('car')
                        ->insert(
                            ['car_num' => $request->car_num, 'car_name'=>$request->car_name, 'car_status'=>'배달대기', 'car_lat'=>35.896157, 'car_lon'=>128.622522]
                        );

        return response(['car_all'=> DB::table('car')->select('car_num','car_name')->get()]);
    }

    // modify to rc car
    public function update(Request $request, $id)
    {
        DB::table('car')
                        ->where('car_num', $id)
                        ->update(['car_name' => $request->car_name]);


        return response(['car_all'=> DB::table('car')->select('car_num','car_name')->get()]);  
    }  

    // delete to rc car
    public function destroy($id)
    {
        DB::table('car')
                        ->where('car_num',$id)
                        ->delete();

        return response(['car_all'=> DB::table('car')->select('car_num','car_name')->get()]);   
    }
}
