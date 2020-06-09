<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Station extends Model
{
    protected $fillable = [
        'station_name', 'station_lat', 'station_lon'
    ];

    protected $primaryKey = 'station_name'; // 기본키 설정안하면 저장이 안되요 ㅠ
    protected $keyType = 'string'; // 기본키 설정안하면 정수로 됩니다.

    public $timestamps = false; // created_at, updated_at 취소하기

    protected $table = 'station';    
}
