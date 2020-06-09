<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class divy extends Model
{
    protected $table = 'dlvy';
    protected $fillable = [
        'dlvy_num','dlvy_car_num', 'dlvy_status', 'dlvy_start_point', 'dlvy_end_point', 'dlvy_sender', 'dlvy_receiver', 'dlvy_wait_num', 'dlvy_wait_start', 'dlvy_wait_time', 'dlvy_call_start', 'dlvy_start', 'dlvy_end', 'dlvy_error', 'dlvy_date'
    ];
}
