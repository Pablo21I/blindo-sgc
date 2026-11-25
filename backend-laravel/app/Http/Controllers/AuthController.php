<?php

namespace App\Http\Controllers;

use App\Models\Personal;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

/**
 * Controlador de autenticación con Laravel Sanctum:
 * maneja registro de cuentas, inicio de sesión (crea token), cierre de sesión (borra token) y perfil actual.
 */
class AuthController extends Controller
{
    // Registra un usuario nuevo en MySQL, lo da de alta en personal y devuelve su token Bearer
    public function register(Request $request)
    {
        // Normalizamos el número de empleado a mayúsculas antes de validar unicidad
        $request->merge([
            'numero_empleado' => strtoupper(trim((string) $request->input('numero_empleado', ''))),
        ]);

        $validated = $request->validate([
            'name' => ['required', 'string', 'min:3', 'max:120', 'regex:/^[\pL][\pL\s.]*$/u'],
            'numero_empleado' => ['required', 'string', 'min:3', 'max:20', 'regex:/^[A-Z0-9_-]+$/', 'unique:users,numero_empleado'],
            'password' => 'required|string|min:4|max:40',
            'rol' => 'nullable|in:ADMIN,SUPERVISOR',
        ], [
            'name.required' => 'El nombre completo es obligatorio.',
            'name.min' => 'El nombre debe tener al menos 3 letras.',
            'name.regex' => 'El nombre solo debe contener letras y espacios (sin números ni símbolos).',
            'numero_empleado.required' => 'El número de empleado es obligatorio.',
            'numero_empleado.min' => 'El número de empleado debe tener al menos 3 caracteres.',
            'numero_empleado.regex' => 'El número de empleado solo acepta letras, números o guiones (ej. SUP-001).',
            'numero_empleado.unique' => 'Ya existe una cuenta registrada con ese número de empleado.',
            'password.required' => 'La contraseña o PIN es obligatorio.',
            'password.min' => 'La contraseña debe tener al menos 4 caracteres.',
        ]);

        $numEmp = strtoupper(trim($validated['numero_empleado']));
        $rol = $validated['rol'] ?? 'SUPERVISOR';

        $user = User::create([
            'name' => trim($validated['name']),
            'numero_empleado' => $numEmp,
            'rol' => $rol,
            'password' => Hash::make($validated['password']),
        ]);

        // Alta automática en plantilla como Supervisor si no existe aún
        Personal::firstOrCreate(
            ['numero_empleado' => $numEmp],
            [
                'user_id' => $user->id,
                'nombre' => $user->name,
                'rol' => 'SUPERVISOR',
                'activo' => true,
            ]
        );

        $token = $user->createToken('blindo-android')->plainTextToken;

        return response()->json([
            'message' => 'Cuenta registrada correctamente',
            'token' => $token,
            'user' => $user,
        ], 201);
    }

    // Verifica empleado y contraseña; si coinciden emite token Sanctum, si no devuelve HTTP 401
    public function login(Request $request)
    {
        $validated = $request->validate([
            'numero_empleado' => 'required|string',
            'password' => 'required|string',
        ], [
            'numero_empleado.required' => 'Ingresa tu número de empleado.',
            'password.required' => 'Ingresa tu contraseña o PIN.',
        ]);

        $numEmp = strtoupper(trim($validated['numero_empleado']));
        $user = User::whereRaw('UPPER(numero_empleado) = ?', [$numEmp])->first();

        if (!$user || !Hash::check($validated['password'], $user->password)) {
            return response()->json([
                'message' => 'Credenciales incorrectas. Verifica tu número de empleado y contraseña.',
            ], 401);
        }

        $token = $user->createToken('blindo-android')->plainTextToken;

        return response()->json([
            'message' => 'Inicio de sesión exitoso',
            'token' => $token,
            'user' => $user,
        ], 200);
    }

    // Revoca (elimina) el token actual en personal_access_tokens al cerrar sesión
    public function logout(Request $request)
    {
        $request->user()?->currentAccessToken()?->delete();

        return response()->json([
            'message' => 'Sesión cerrada correctamente',
        ], 200);
    }

    // Devuelve los datos del usuario dueño del token Bearer activo
    public function me(Request $request)
    {
        return response()->json($request->user(), 200);
    }
}
