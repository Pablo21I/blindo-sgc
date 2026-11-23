package com.example.blindo.ui

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Abre cualquier BottomSheetDialog completamente expandido desde el primer momento
 * para que todos los botones de acción sean visibles sin tener que arrastrarlo manualmente.
 */
fun BottomSheetDialog.showExpanded() {
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
    behavior.skipCollapsed = true
    setOnShowListener {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }
    show()
}
