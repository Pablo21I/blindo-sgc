<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Personal extends Model
{
    protected $fillable = [
        'user_id',
        'nombre',
        'rol',
        'numero_empleado',
        'activo',
    ];

    protected $casts = [
        'activo' => 'boolean',
    ];

    public function user()
    {
        return $this->belongsTo(User::class);
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
