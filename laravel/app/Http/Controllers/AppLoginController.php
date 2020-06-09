<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DB;
class AppLoginController extends Controller
{
    // login
    public function login_check(Request $req){   
        if($req->login_id === null) {
            return response()->json(['success'=>'FALSE']);
        }
        if($req->login_password === null){
            return response()->json(['success'=>'FALSE']);
        }
        $user_password = DB::table('user')->where('user_id', $req->login_id)->value('user_password');

        if($user_password === $req->login_password){
            $user_info = DB::table('user')->where('user_id', $req->login_id)->value('user_name');

            return response()->json([
                'success' => 'TRUE',
                'user_info' => $user_info
            ]);
        }else{
            return response()->json(['success'=>'FALSE']);
        }
    }
}
