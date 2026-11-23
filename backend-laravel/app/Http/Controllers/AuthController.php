<?php

namespace App\Http\Controllers;

use App\Models\Personal;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class AuthController extends Controller
{
    public function register(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|min:3|max:120',
            'numero_empleado' => 'required|string|max:50|unique:users,numero_empleado',
            'password' => 'required|string|min:4',
            'rol' => 'nullable|in:ADMIN,SUPERVISOR',
        ], [
            'name.required' => 'El nombre completo es obligatorio.',
            'name.min' => 'El nombre debe tener al menos 3 caracteres.',
            'numero_empleado.required' => 'El número de empleado es obligatorio.',
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

    public function logout(Request $request)
    {
        $request->user()?->currentAccessToken()?->delete();

        return response()->json([
            'message' => 'Sesión cerrada correctamente',
        ], 200);
    }

    public function me(Request $request)
    {
        return response()->json($request->user(), 200);
    }
}
