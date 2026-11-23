<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('asignacion_epps', function (Blueprint $table) {
            $table->id();
            $table->foreignId('personal_id')->constrained('personals')->onDelete('cascade');
            $table->foreignId('user_id')->nullable()->constrained('users')->nullOnDelete();
            $table->integer('epp_id');
            $table->string('nombre_epp');
            $table->timestamp('fecha_entrega')->useCurrent();
            $table->integer('vida_util_meses_asignada')->default(12);
            $table->timestamps();

            $table->unique(['personal_id', 'epp_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('asignacion_epps');
    }
};
