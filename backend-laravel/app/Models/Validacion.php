<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Validacion extends Model
{
    protected $table = 'validaciones';

    protected $fillable = [
        'personal_id',
        'user_id',
        'trabajador',
        'supervisor',
        'turno',
        'zona',
        'trabajo_id',
        'nombre_trabajo',
        'estado',
        'activa',
        'notas',
    ];

    protected $casts = [
        'activa' => 'boolean',
        'trabajo_id' => 'integer',
    ];

    public function personal()
    {
        return $this->belongsTo(Personal::class);
    }

    public function user()
    {
        return $this->belongsTo(User::class);
    }
}
