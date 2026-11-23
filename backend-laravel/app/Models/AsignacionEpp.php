<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class AsignacionEpp extends Model
{
    protected $fillable = [
        'personal_id',
        'user_id',
        'epp_id',
        'nombre_epp',
        'fecha_entrega',
        'vida_util_meses_asignada',
    ];

    protected $casts = [
        'fecha_entrega' => 'datetime',
        'epp_id' => 'integer',
        'vida_util_meses_asignada' => 'integer',
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
