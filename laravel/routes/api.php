<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| is assigned the "api" middleware group. Enjoy building your API!
|
*/

Route::middleware('auth:api')->get('/user', function (Request $request) {
    return $request->user();
});

///////////// WEB /////////////
Route::get('/dlvy/control', 'WebControlController@index');
Route::get('/dlvy/control/show/{id}', 'WebControlController@run_status');
Route::get('/dlvy/control/car', 'WebControlController@inquireRc');

Route::get('dlvy/statistics/complete/{mode}/{term}/{date}', 'WebStatisticsController@divy_complete');
Route::get('dlvy/statistics/waitcancel/{mode}/{term}/{date}', 'WebStatisticsController@wait_and_cancle');
Route::get('dlvy/statistics/waittimeavg/{term}/{date}', 'WebStatisticsController@wait_time_avg');
# mode = acc, avg
# term = day, week, month
# date = Specified Date (Type:2020-05-09) 

Route::resource('dlvy/management/station', 'WebStationManagementController');
Route::resource('dlvy/management/checkpoint', 'WebCheckPointManagementController');
Route::resource('dlvy/management/car', 'WebCarManagementController');
Route::resource('dlvy/management/path', 'WebPathManagementController');
Route::get('dlvy/management/pathcheck/{id}', 'WebPathManagementController@show_path_check');


///////////// APP /////////////
Route::post('app/login', 'AppLoginController@login_check' );
Route::get('dlvy/call', 'AppCallController@call');
Route::post('dlvy/check_user/{id}', 'AppCallController@check_user');
Route::get('dlvy/checkpoint/{start_point}/{end_point}', 'AppCallController@dlvy_checkpoint');

Route::get('dlvy/senddlvy/{id}', 'AppDlvyInfoController@send_dlvy');
Route::get('dlvy/receivdlvy/{id}', 'AppDlvyInfoController@receiv_dlvy');
Route::get('dlvy/completedlvy/{id}/{term}/{date_start}/{date_end}', 'AppDlvyCompleteController@completed_dlvy');