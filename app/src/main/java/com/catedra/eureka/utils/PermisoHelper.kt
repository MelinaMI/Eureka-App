package com.catedra.eureka.utils

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermisoHelper {

    private const val TAG = "PermisoHelper"

    val PERMISOS_UBICACION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun tienePermisoUbicacion(fragment: Fragment): Boolean {
        val tiene = PERMISOS_UBICACION.all {
            ContextCompat.checkSelfPermission(fragment.requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d(TAG, "Verificación de permisos de ubicación: $tiene")
        return tiene
    }

    fun solicitarPermisos(
        fragment: Fragment,
        launcher: ActivityResultLauncher<Array<String>>
    ) {
        val faltantes = PERMISOS_UBICACION.filter {
            ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        Log.d(TAG, "Permisos faltantes: $faltantes")

        if (faltantes.isEmpty()) {
            Log.d(TAG, "No hay permisos faltantes, no se solicita nada")
            return
        }

        val debeMostrarRacional = faltantes.any {
            fragment.shouldShowRequestPermissionRationale(it)
        }

        if (debeMostrarRacional) {
            Log.d(TAG, "El usuario ya denegó permisos antes, mostrando racional")
        }

        Log.d(TAG, "Lanzando solicitud de permisos")
        launcher.launch(PERMISOS_UBICACION)
    }
}