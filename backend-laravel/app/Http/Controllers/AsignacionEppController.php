<?php

namespace App\Http\Controllers;

use App\Models\AsignacionEpp;
use Illuminate\Http\Request;

class AsignacionEppController extends Controller
{
    public function index()
    {
        $items = AsignacionEpp::with('personal')
            ->orderBy('fecha_entrega', 'asc')
            ->get()
            ->map(fn (AsignacionEpp $a) => $this->formatItem($a));

        return response()->json($items, 200);
    }

    public function store(Request $request)
    {
        $validated = $request->validate([
            'personal_id' => 'required|integer|exists:personals,id',
            'epp_id' => 'required|integer|min:1',
            'nombre_epp' => 'required|string|max:160',
            'vida_util_meses_asignada' => 'required|integer|min:1',
        ], [
            'personal_id.exists' => 'El trabajador seleccionado no existe en el servidor.',
            'vida_util_meses_asignada.min' => 'La vida útil debe ser mayor a 0.',
        ]);

        $existente = AsignacionEpp::where('personal_id', $validated['personal_id'])
            ->where('epp_id', $validated['epp_id'])
            ->first();

        if ($existente) {
            $existente->update([
                'user_id' => $request->user()?->id,
                'nombre_epp' => $validated['nombre_epp'],
                'fecha_entrega' => now(),
                'vida_util_meses_asignada' => $validated['vida_util_meses_asignada'],
            ]);
            $existente->load('personal');

            return response()->json([
                'actualizado' => true,
                'data' => $this->formatItem($existente),
            ], 200);
        }

        $creado = AsignacionEpp::create([
            'personal_id' => $validated['personal_id'],
            'user_id' => $request->user()?->id,
            'epp_id' => $validated['epp_id'],
            'nombre_epp' => $validated['nombre_epp'],
            'fecha_entrega' => now(),
            'vida_util_meses_asignada' => $validated['vida_util_meses_asignada'],
        ]);
        $creado->load('personal');

        return response()->json([
            'actualizado' => false,
            'data' => $this->formatItem($creado),
        ], 201);
    }

    public function update(Request $request, AsignacionEpp $asignacionEpp)
    {
        $validated = $request->validate([
            'vida_util_meses_asignada' => 'sometimes|integer|min:1',
            'renovar' => 'sometimes|boolean',
        ]);

        if (isset($validated['vida_util_meses_asignada'])) {
            $asignacionEpp->vida_util_meses_asignada = $validated['vida_util_meses_asignada'];
        }
        if (!empty($validated['renovar'])) {
            $asignacionEpp->fecha_entrega = now();
        }
        $asignacionEpp->save();
        $asignacionEpp->load('personal');

        return response()->json($this->formatItem($asignacionEpp), 200);
    }

    public function destroy(AsignacionEpp $asignacionEpp)
    {
        $asignacionEpp->delete();

        return response()->json([
            'message' => 'Asignación de EPP eliminada correctamente',
        ], 200);
    }

    private function formatItem(AsignacionEpp $a): array
    {
        return [
            'id' => $a->id,
            'personal_id' => $a->personal_id,
            'epp_id' => $a->epp_id,
            'nombre_epp' => $a->nombre_epp,
            'fecha_entrega_millis' => $a->fecha_entrega ? $a->fecha_entrega->getTimestampMs() : now()->getTimestampMs(),
            'vida_util_meses_asignada' => $a->vida_util_meses_asignada,
            'personal' => $a->personal ? [
                'id' => $a->personal->id,
                'nombre' => $a->personal->nombre,
                'rol' => $a->personal->rol,
                'numero_empleado' => $a->personal->numero_empleado,
                'activo' => (bool) $a->personal->activo,
            ] : null,
        ];
    }
}
