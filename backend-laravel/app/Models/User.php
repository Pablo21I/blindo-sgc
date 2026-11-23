<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    protected $fillable = [
        'name',
        'numero_empleado',
        'email',
        'rol',
        'password',
    ];

    protected $hidden = [
        'password',
    ];

    protected function casts(): array
    {
        return [
            'password' => 'hashed',
        ];
    }

    public function personals()
    {
        return $this->hasMany(Personal::class);
    }

    public function asignacionEpps()
    {
        return $this->hasMany(AsignacionEpp::class);
    }

    public function validaciones()
    {
        return $this->hasMany(Validacion::class);
    }
}
