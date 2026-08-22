package com.catedra.eureka.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.catedra.eureka.R

object NotificacionHelper {

    private const val TAG = "NotificacionHelper"
    private const val CANAL_ID = "eureka_alertas"
    private var contadorId = 0

    fun crearCanal(context: Context) {
        val nombreCanal = context.getString(R.string.notif_canal_nombre)
        val descripcionCanal = context.getString(R.string.notif_canal_descripcion)
        val canal = NotificationChannel(
            CANAL_ID,
            nombreCanal,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = descripcionCanal
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
        Log.d(TAG, "Canal de notificaciones creado")
    }

    fun notificarReporteCercano(context: Context, nombreAnimal: String, distanciaKm: Double, pendingIntent: PendingIntent) {
        Log.d(TAG, "Disparando notificación local: $nombreAnimal a $distanciaKm km")
        val manager = context.getSystemService(NotificationManager::class.java)
        val textoContenido = if (distanciaKm < 1.0) {
            val metros = (distanciaKm * 1000).toInt()
            context.getString(R.string.notif_metros, nombreAnimal, metros)
        } else {
            context.getString(R.string.notif_km, nombreAnimal, distanciaKm)
        }
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_mi_ubicacion)
            .setContentTitle(context.getString(R.string.notif_titulo))
            .setContentText(textoContenido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(contadorId++, notificacion)
        Log.d(TAG, "Notificación disparada con id: $contadorId")
    }
}