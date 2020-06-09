<?php

namespace App;

use Illuminate\Database\Eloquent\Model;

class Checkpoint extends Model
{
    protected $fillable = [
        'checkpoint_id', 'checkpoint_lat', 'checkpoint_lon'
    ];

    public $timestamps = false; // created_at, updated_at 취소하기

    protected $primaryKey = 'checkpoint_id';

    protected $table = 'checkpoint';    
}
