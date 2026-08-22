package com.catedra.eureka.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import java.util.Locale

object UbicacionHelper {

    private const val TAG = "UbicacionHelper"

    @SuppressLint("MissingPermission")
    fun obtenerUbicacionActual(
        context: Context,
        onResultado: (lat: Double, lng: Double, direccion: String) -> Unit,
        onFallo: () -> Unit
    ) {
        Log.d(TAG, "Solicitando ubicación actual con alta precisión")
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Ubicación obtenida: lat=${location.latitude}, lng=${location.longitude}")
                    val lat = location.latitude
                    val lng = location.longitude
                    var direccionText = "Coordenadas: $lat, $lng"
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val direcciones = geocoder.getFromLocation(lat, lng, 1)
                        if (!direcciones.isNullOrEmpty()) {
                            direccionText = direcciones[0].getAddressLine(0) ?: direccionText
                            Log.d(TAG, "Dirección resuelta: $direccionText")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Geocoder falló, usando coordenadas: ${e.message}")
                    }
                    onResultado(lat, lng, direccionText)
                } else {
                    Log.w(TAG, "Ubicación nula, usando fallback")
                    onFallo()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al obtener ubicación: ${it.message}")
                onFallo()
            }
    }
}