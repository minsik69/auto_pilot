<?php

/** @var \Illuminate\Database\Eloquent\Factory $factory */

use App\station;
use Faker\Generator as Faker;

$factory->define(station::class, function (Faker $faker) {
    return [
        'station_name' => $faker->name(),
    ];
});
