<?php

use App\Http\Controllers\AsignacionEppController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\PersonalController;
use App\Http\Controllers\ValidacionController;
use Illuminate\Support\Facades\Route;

// Rutas públicas de autenticación (Laravel Sanctum)
Route::post('/register', [AuthController::class, 'register']);
Route::post('/login', [AuthController::class, 'login']);

// Rutas protegidas con Bearer Token de Laravel Sanctum
Route::middleware('auth:sanctum')->group(function () {
    Route::get('/me', [AuthController::class, 'me']);
    Route::post('/logout', [AuthController::class, 'logout']);

    // CRUD Personal
    Route::get('/personal', [PersonalController::class, 'index']);
    Route::post('/personal', [PersonalController::class, 'store']);
    Route::get('/personal/{personal}', [PersonalController::class, 'show']);
    Route::put('/personal/{personal}', [PersonalController::class, 'update']);
    Route::patch('/personal/{personal}', [PersonalController::class, 'update']);
    Route::delete('/personal/{personal}', [PersonalController::class, 'destroy']);

    // CRUD Asignaciones EPP
    Route::get('/asignaciones-epp', [AsignacionEppController::class, 'index']);
    Route::post('/asignaciones-epp', [AsignacionEppController::class, 'store']);
    Route::put('/asignaciones-epp/{asignacionEpp}', [AsignacionEppController::class, 'update']);
    Route::patch('/asignaciones-epp/{asignacionEpp}', [AsignacionEppController::class, 'update']);
    Route::delete('/asignaciones-epp/{asignacionEpp}', [AsignacionEppController::class, 'destroy']);

    // CRUD Validaciones (Órdenes Activas + Bitácora SGC)
    Route::get('/validaciones', [ValidacionController::class, 'index']);
    Route::post('/validaciones', [ValidacionController::class, 'store']);
    Route::put('/validaciones/{validacion}', [ValidacionController::class, 'update']);
    Route::patch('/validaciones/{validacion}', [ValidacionController::class, 'update']);
    Route::delete('/validaciones/{validacion}', [ValidacionController::class, 'destroy']);
});
