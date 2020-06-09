<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Car extends Model
{
    protected $fillable = [
        'checkpoint_id', 'checkpoint_lat', 'checkpoint_lon'
    ];

    public $timestamps = false; // created_at, updated_at 취소하기

    protected $primaryKey = 'car_num';

    protected $table = 'car';    
}
