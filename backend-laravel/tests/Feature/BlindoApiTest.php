<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class BlindoApiTest extends TestCase
{
    use RefreshDatabase;

    public function test_flujo_completo_sanctum_crud_y_errores_401_422(): void
    {
        // 1. Ruta protegida sin token -> 401
        $this->getJson('/api/personal')->assertStatus(401);

        // 2. Registro con datos incompletos -> 422
        $this->postJson('/api/register', [
            'name' => '',
            'numero_empleado' => '',
            'password' => '12',
        ])->assertStatus(422)
          ->assertJsonValidationErrors(['name', 'numero_empleado', 'password']);

        // 3. Registro real -> 201 + Token Sanctum + alta automática en personals
        $registerRes = $this->postJson('/api/register', [
            'name' => 'Ing. Pablo Navarro',
            'numero_empleado' => 'SUP-001',
            'password' => 'seguro123',
            'rol' => 'ADMIN',
        ]);
        $registerRes->assertStatus(201)->assertJsonStructure(['token', 'user' => ['id', 'name', 'numero_empleado']]);

        // 4. Login con contraseña incorrecta -> 401 amigable
        $this->postJson('/api/login', [
            'numero_empleado' => 'SUP-001',
            'password' => 'malapass',
        ])->assertStatus(401)
          ->assertJsonStructure(['message']);

        // 5. Login correcto -> 200 + Token Sanctum
        $loginRes = $this->postJson('/api/login', [
            'numero_empleado' => 'SUP-001',
            'password' => 'seguro123',
        ]);
        $loginRes->assertStatus(200);
        $token = $loginRes->json('token');
        $headers = ['Authorization' => "Bearer $token"];

        // 6. CRUD Personal: POST (422 vacío, 201 válido), GET, PUT, DELETE
        $this->withHeaders($headers)->postJson('/api/personal', [
            'nombre' => '',
            'rol' => 'OPERADOR',
            'numero_empleado' => '',
        ])->assertStatus(422);

        $crearOp = $this->withHeaders($headers)->postJson('/api/personal', [
            'nombre' => 'Carlos Méndez',
            'rol' => 'OPERADOR',
            'numero_empleado' => 'OP-201',
        ]);
        $crearOp->assertStatus(201);
        $personalId = $crearOp->json('id');

        $this->withHeaders($headers)->getJson('/api/personal')
            ->assertStatus(200)
            ->assertJsonFragment(['numero_empleado' => 'OP-201']);

        $this->withHeaders($headers)->putJson("/api/personal/$personalId", [
            'nombre' => 'Carlos Méndez López',
            'rol' => 'OPERADOR',
            'numero_empleado' => 'OP-201',
        ])->assertStatus(200)
          ->assertJsonFragment(['nombre' => 'Carlos Méndez López']);

        // 7. CRUD Asignaciones EPP: POST, GET, PUT (editar vigencia + renovar), DELETE
        $asigRes = $this->withHeaders($headers)->postJson('/api/asignaciones-epp', [
            'personal_id' => $personalId,
            'epp_id' => 14,
            'nombre_epp' => 'Botas dieléctricas c/ casquillo',
            'vida_util_meses_asignada' => 12,
        ]);
        $asigRes->assertStatus(201);
        $asigId = $asigRes->json('data.id');

        $this->withHeaders($headers)->getJson('/api/asignaciones-epp')
            ->assertStatus(200)
            ->assertJsonFragment(['nombre_epp' => 'Botas dieléctricas c/ casquillo']);

        $this->withHeaders($headers)->putJson("/api/asignaciones-epp/$asigId", [
            'vida_util_meses_asignada' => 24,
            'renovar' => true,
        ])->assertStatus(200)
          ->assertJsonFragment(['vida_util_meses_asignada' => 24]);

        $this->withHeaders($headers)->deleteJson("/api/asignaciones-epp/$asigId")
            ->assertStatus(200);

        // 8. CRUD Validaciones (Órdenes Activas + Bitácora): POST, GET, PUT (notas + finalizar), DELETE
        $valRes = $this->withHeaders($headers)->postJson('/api/validaciones', [
            'personal_id' => $personalId,
            'trabajador' => 'Carlos Méndez López (OP-201)',
            'supervisor' => 'Ing. Pablo Navarro (SUP-001)',
            'turno' => 'Matutino (1er Turno)',
            'zona' => 'Zona A - Nave Principal',
            'trabajo_id' => 1,
            'nombre_trabajo' => 'Soldadura con arco eléctrico',
            'estado' => 'VERDE',
            'activa' => true,
            'notas' => 'Validación aprobada',
        ]);
        $valRes->assertStatus(201);
        $valId = $valRes->json('id');

        $this->withHeaders($headers)->getJson('/api/validaciones?activa=true')
            ->assertStatus(200)
            ->assertJsonCount(1);

        $this->withHeaders($headers)->putJson("/api/validaciones/$valId", [
            'notas' => 'Trabajo concluido sin incidentes',
            'activa' => false,
        ])->assertStatus(200)
          ->assertJsonFragment(['activa' => false]);

        $this->withHeaders($headers)->deleteJson("/api/validaciones/$valId")
            ->assertStatus(200);

        // 9. DELETE Personal
        $this->withHeaders($headers)->deleteJson("/api/personal/$personalId")
            ->assertStatus(200);

        // 10. Logout revoca token
        $this->withHeaders($headers)->postJson('/api/logout')
            ->assertStatus(200);
    }
}
