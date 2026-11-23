<?php

namespace App\Http\Controllers;

use App\Models\Validacion;
use Illuminate\Http\Request;

class ValidacionController extends Controller
{
    public function index(Request $request)
    {
        $query = Validacion::orderByDesc('id');

        if ($request->has('activa')) {
            $activa = filter_var($request->query('activa'), FILTER_VALIDATE_BOOLEAN);
            $query->where('activa', $activa);
        }

        $items = $query->get()->map(fn (Validacion $v) => $this->formatItem($v));

        return response()->json($items, 200);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'personal_id' => 'nullable|integer|exists:personals,id',
            'trabajador' => 'required|string|max:160',
            'supervisor' => 'required|string|max:160',
            'turno' => 'required|string|max:80',
            'zona' => 'required|string|max:120',
            'trabajo_id' => 'required|integer',
            'nombre_trabajo' => 'required|string|max:160',
            'estado' => 'required|in:VERDE,AMARILLO,ROJO',
            'activa' => 'required|boolean',
            'notas' => 'nullable|string',
        ]);

        $validacion = Validacion::create([
            'personal_id' => $validated['personal_id'] ?? null,
            'user_id' => $request->user()?->id,
            'trabajador' => $validated['trabajador'],
            'supervisor' => $validated['supervisor'],
            'turno' => $validated['turno'],
            'zona' => $validated['zona'],
            'trabajo_id' => $validated['trabajo_id'],
            'nombre_trabajo' => $validated['nombre_trabajo'],
            'estado' => $validated['estado'],
            'activa' => $validated['activa'],
            'notas' => $validated['notas'] ?? '',
        ]);

        return response()->json($this->formatItem($validacion), 201);
    }

    public function update(Request $request, Validacion $validacion)
    {
        $validated = $request->validate([
            'notas' => 'sometimes|nullable|string',
            'activa' => 'sometimes|boolean',
        ]);

        if (array_key_exists('notas', $validated)) {
            $validacion->notas = $validated['notas'] ?? '';
        }
        if (array_key_exists('activa', $validated)) {
            $validacion->activa = $validated['activa'];
        }
        $validacion->save();

        return response()->json($this->formatItem($validacion), 200);
    }

    public function destroy(Validacion $validacion)
    {
        $validacion->delete();

        return response()->json([
            'message' => 'Registro eliminado correctamente',
        ], 200);
    }

    private function formatItem(Validacion $v): array
    {
        return [
            'id' => $v->id,
            'personal_id' => $v->personal_id ?? 0,
            'trabajador' => $v->trabajador,
            'supervisor' => $v->supervisor,
            'turno' => $v->turno,
            'zona' => $v->zona,
            'trabajo_id' => $v->trabajo_id,
            'nombre_trabajo' => $v->nombre_trabajo,
            'estado' => $v->estado,
            'activa' => (bool) $v->activa,
            'notas' => $v->notas ?? '',
            'fecha_hora_millis' => $v->created_at ? $v->created_at->getTimestampMs() : now()->getTimestampMs(),
        ];
    }
}
