<?php

namespace App\Http\Controllers;

use App\Models\Personal;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

/**
 * Controlador CRUD para la tabla personals (Operadores y Supervisores).
 * Todas las rutas requieren token válido de Sanctum.
 */
class PersonalController extends Controller
{
    // Lista todo el personal activo ordenado del más reciente al más antiguo (GET /api/personal)
    public function index()
    {
        $lista = Personal::where('activo', true)
            ->orderByDesc('id')
            ->get();

        return response()->json($lista, 200);
    }

    // Crea un nuevo operador o supervisor validando campos obligatorios y duplicados (POST /api/personal)
    public function store(Request $request)
    {
        // Normalizamos el número de empleado antes de validar que no esté repetido
        $request->merge([
            'numero_empleado' => strtoupper(trim((string) $request->input('numero_empleado', ''))),
        ]);

        $validated = $request->validate([
            'nombre' => ['required', 'string', 'min:3', 'max:120', 'regex:/^[\pL][\pL\s.]*$/u'],
            'rol' => 'required|in:OPERADOR,SUPERVISOR',
            'numero_empleado' => ['required', 'string', 'min:3', 'max:20', 'regex:/^[A-Z0-9_-]+$/', 'unique:personals,numero_empleado'],
        ], [
            'nombre.required' => 'El nombre completo es obligatorio.',
            'nombre.min' => 'El nombre debe tener al menos 3 letras.',
            'nombre.regex' => 'El nombre solo debe contener letras y espacios (sin números ni símbolos).',
            'rol.in' => 'El rol debe ser OPERADOR o SUPERVISOR.',
            'numero_empleado.required' => 'El número de empleado es obligatorio.',
            'numero_empleado.min' => 'El número de empleado debe tener al menos 3 caracteres.',
            'numero_empleado.regex' => 'El número de empleado solo acepta letras, números o guiones (ej. OP-201).',
            'numero_empleado.unique' => 'Ese número de empleado ya está registrado.',
        ]);

        $personal = Personal::create([
            'user_id' => $request->user()?->id,
            'nombre' => trim($validated['nombre']),
            'rol' => $validated['rol'],
            'numero_empleado' => strtoupper(trim($validated['numero_empleado'])),
            'activo' => true,
        ]);

        return response()->json($personal, 201);
    }

    // Devuelve un trabajador específico con sus EPP asignados (GET /api/personal/{id})
    public function show(Personal $personal)
    {
        return response()->json($personal->load('asignacionEpps'), 200);
    }

    // Actualiza nombre, rol o número de empleado de un trabajador (PUT /api/personal/{id})
    public function update(Request $request, Personal $personal)
    {
        $request->merge([
            'numero_empleado' => strtoupper(trim((string) $request->input('numero_empleado', ''))),
        ]);

        $validated = $request->validate([
            'nombre' => ['required', 'string', 'min:3', 'max:120', 'regex:/^[\pL][\pL\s.]*$/u'],
            'rol' => 'required|in:OPERADOR,SUPERVISOR',
            'numero_empleado' => [
                'required',
                'string',
                'min:3',
                'max:20',
                'regex:/^[A-Z0-9_-]+$/',
                Rule::unique('personals', 'numero_empleado')->ignore($personal->id),
            ],
        ], [
            'nombre.required' => 'El nombre completo es obligatorio.',
            'nombre.min' => 'El nombre debe tener al menos 3 letras.',
            'nombre.regex' => 'El nombre solo debe contener letras y espacios (sin números ni símbolos).',
            'numero_empleado.min' => 'El número de empleado debe tener al menos 3 caracteres.',
            'numero_empleado.regex' => 'El número de empleado solo acepta letras, números o guiones.',
            'numero_empleado.unique' => 'Ese número de empleado pertenece a otro trabajador.',
        ]);

        $personal->update([
            'nombre' => trim($validated['nombre']),
            'rol' => $validated['rol'],
            'numero_empleado' => strtoupper(trim($validated['numero_empleado'])),
        ]);

        return response()->json($personal, 200);
    }

    // Elimina al trabajador de MySQL y, por cascada, también sus asignaciones de EPP (DELETE /api/personal/{id})
    public function destroy(Personal $personal)
    {
        $personal->delete();

        return response()->json([
            'message' => 'Trabajador eliminado correctamente',
        ], 200);
    }
}
