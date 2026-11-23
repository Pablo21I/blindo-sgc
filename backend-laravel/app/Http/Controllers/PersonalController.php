<?php

namespace App\Http\Controllers;

use App\Models\Personal;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class PersonalController extends Controller
{
    public function index()
    {
        $lista = Personal::where('activo', true)
            ->orderByDesc('id')
            ->get();

        return response()->json($lista, 200);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'nombre' => 'required|string|min:3|max:120',
            'rol' => 'required|in:OPERADOR,SUPERVISOR',
            'numero_empleado' => 'required|string|max:50|unique:personals,numero_empleado',
        ], [
            'nombre.required' => 'El nombre completo es obligatorio.',
            'nombre.min' => 'El nombre debe tener al menos 3 caracteres.',
            'rol.in' => 'El rol debe ser OPERADOR o SUPERVISOR.',
            'numero_empleado.required' => 'El número de empleado es obligatorio.',
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

    public function show(Personal $personal)
    {
        return response()->json($personal->load('asignacionEpps'), 200);
    }

    public function update(Request $request, Personal $personal)
    {
        $validated = $request->validate([
            'nombre' => 'required|string|min:3|max:120',
            'rol' => 'required|in:OPERADOR,SUPERVISOR',
            'numero_empleado' => [
                'required',
                'string',
                'max:50',
                Rule::unique('personals', 'numero_empleado')->ignore($personal->id),
            ],
        ], [
            'nombre.required' => 'El nombre completo es obligatorio.',
            'numero_empleado.unique' => 'Ese número de empleado pertenece a otro trabajador.',
        ]);

        $personal->update([
            'nombre' => trim($validated['nombre']),
            'rol' => $validated['rol'],
            'numero_empleado' => strtoupper(trim($validated['numero_empleado'])),
        ]);

        return response()->json($personal, 200);
    }

    public function destroy(Personal $personal)
    {
        $personal->delete();

        return response()->json([
            'message' => 'Trabajador eliminado correctamente',
        ], 200);
    }
}
