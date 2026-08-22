package com.catedra.eureka.data.services

import android.content.Context
import android.util.Log
import com.catedra.eureka.data.model.EstadoReporte
import com.catedra.eureka.utils.DistanciaHelper
import com.catedra.eureka.utils.NavegacionHelper
import com.catedra.eureka.utils.NotificacionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlertaService {

    private val TAG = "AlertaService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val reporteService = ReporteService()
    private val usuarioService = UsuarioService()
    private val authService = AuthService()

    private val idsYaVistos = mutableSetOf<String>()
    private var primeraEmision = true

    fun iniciar(context: Context) {
        if (job?.isActive == true) {
            Log.d(TAG, "Listener ya activo, se omite inicio duplicado")
            return
        }
        val uid = authService.getUsuarioActualId()
        if (uid == null) {
            Log.w(TAG, "No hay usuario autenticado, no se inicia el listener")
            return
        }
        Log.d(TAG, "Iniciando listener de reportes para uid=$uid")
        job = scope.launch {
            reporteService.obtenerReportesMapaFlow().collect { lista ->
                Log.d(TAG, "Snapshot recibido: ${lista.size} reportes")

                if (primeraEmision) {
                    Log.d(TAG, "Primera emisión: registrando ${lista.size} reportes sin notificar")
                    idsYaVistos.addAll(lista.map { it.id })
                    primeraEmision = false
                    return@collect
                }

                val nuevos = lista.filter { it.id !in idsYaVistos }
                Log.d(TAG, "Reportes nuevos detectados: ${nuevos.size}")
                idsYaVistos.addAll(nuevos.map { it.id })

                if (nuevos.isEmpty()) return@collect

                val resultadoUsuario = usuarioService.obtenerUsuario(uid)
                if (resultadoUsuario.isFailure) {
                    Log.e(TAG, "No se pudo obtener el usuario: ${resultadoUsuario.exceptionOrNull()?.message}")
                    return@collect
                }

                val usuario = resultadoUsuario.getOrNull() ?: return@collect

                if (!usuario.alertasActivadas) {
                    Log.d(TAG, "Alertas desactivadas, se omiten notificaciones")
                    return@collect
                }

                if (usuario.latitud == 0.0 && usuario.longitud == 0.0) {
                    Log.w(TAG, "Ubicación del usuario no disponible en Firestore")
                    return@collect
                }

                val resultadoReportesProyios = reporteService.obtenerDelUsuario(uid)
                if (resultadoReportesProyios.isFailure) {
                    Log.e(TAG, "No se pudieron obtener los reportes propios: ${resultadoReportesProyios.exceptionOrNull()?.message}")
                    return@collect
                }

                val animalesPropiosPerdidos = resultadoReportesProyios.getOrNull()
                    .orEmpty()
                    .filter { it.estado == EstadoReporte.PERDIDO.name }
                    .map { it.animal }
                    .toSet()

                Log.d(TAG, "Tipos de animales propios perdidos: $animalesPropiosPerdidos")

                if (animalesPropiosPerdidos.isEmpty()) {
                    Log.d(TAG, "El usuario no tiene reportes propios activos, se omiten notificaciones")
                    return@collect
                }

                for (reporte in nuevos) {
                    if (reporte.usuarioId == uid) {
                        Log.d(TAG, "Reporte ${reporte.id} es propio, se omite")
                        continue
                    }

                    if (reporte.estado == EstadoReporte.PERDIDO.name && reporte.usuarioId == uid) {
                        Log.d(TAG, "Reporte ${reporte.id} es propio y perdido, se omite como notificable")
                        continue
                    }

                    if (reporte.animal !in animalesPropiosPerdidos) {
                        Log.d(TAG, "Reporte ${reporte.id} es de tipo ${reporte.animal}, no coincide con los propios $animalesPropiosPerdidos, se omite")
                        continue
                    }

                    val distancia = DistanciaHelper.calcularDistanciaKm(usuario.latitud, usuario.longitud, reporte.latitud, reporte.longitud)

                    Log.d(TAG, "Reporte ${reporte.id} a $distancia km (radio: ${usuario.radioAlertaKm} km)")

                    if (distancia <= usuario.radioAlertaKm) {
                        Log.d(TAG, "Reporte dentro del radio, disparando notificación")
                        val nombre = reporte.nombre.ifEmpty { reporte.animal }
                        val pendingIntent = NavegacionHelper.crearPendingIntentDetalles(context, reporte.id)
                        NotificacionHelper.notificarReporteCercano(context, nombre, distancia, pendingIntent)
                    } else {
                        Log.d(TAG, "Reporte ${reporte.id} fuera del radio, se omite")
                    }
                }
            }
        }
    }

    fun detener() {
        Log.d(TAG, "Deteniendo listener de reportes")
        job?.cancel()
        job = null
        idsYaVistos.clear()
        primeraEmision = true
    }
}