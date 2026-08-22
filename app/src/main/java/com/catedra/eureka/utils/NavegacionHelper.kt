package com.catedra.eureka.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.catedra.eureka.MainActivity
import com.catedra.eureka.ui.detalles.DetallesFragment

object NavegacionHelper {

    const val EXTRA_REPORTE_ID = "reporte_id"

    fun irADetalles(fragmentOrigen: Fragment, contenedorId: Int, reporteId: String) {
        val fragmentDetalles = DetallesFragment()
        val datos = Bundle()
        datos.putString(EXTRA_REPORTE_ID, reporteId)
        fragmentDetalles.arguments = datos

        fragmentOrigen.parentFragmentManager.beginTransaction()
            .replace(contenedorId, fragmentDetalles)
            .addToBackStack(null)
            .commit()
    }

    fun crearPendingIntentDetalles(context: Context, reporteId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_REPORTE_ID, reporteId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            reporteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}