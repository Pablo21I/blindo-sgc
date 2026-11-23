<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('validaciones', function (Blueprint $table) {
            $table->id();
            $table->foreignId('personal_id')->nullable()->constrained('personals')->nullOnDelete();
            $table->foreignId('user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->string('trabajador');
            $table->string('supervisor');
            $table->string('turno');
            $table->string('zona');
            $table->integer('trabajo_id');
            $table->string('nombre_trabajo');
            $table->string('estado'); // VERDE, AMARILLO o ROJO
            $table->boolean('activa')->default(false); // true = orden activa en Taller, false = historial/bloqueo
            $table->text('notas')->nullable();
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('validaciones');
    }
};
