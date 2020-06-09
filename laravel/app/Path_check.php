<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Path_check extends Model
{
    public $timestamps = false; // created_at, updated_at 취소하기

    protected $primaryKey = 'path_check_id';

    protected $table = 'path_check';    
}
