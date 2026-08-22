package com.catedra.eureka.ui.mapa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.model.Usuario
import com.catedra.eureka.data.services.AuthService
import com.catedra.eureka.data.services.ReporteService
import com.catedra.eureka.data.services.UsuarioService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val reportes: List<Reporte> = emptyList(),
    val usuario: Usuario? = null,
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val TAG = "MapViewModel"

    private val reporteService = ReporteService()
    private val usuarioService = UsuarioService()
    private val authService = AuthService()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        cargarUsuario()
        escucharReportes()
    }

    private fun cargarUsuario() {
        val uid = authService.getUsuarioActualId()
        if (uid == null) {
            Log.w(TAG, "No hay usuario autenticado")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Cargando datos de usuario: $uid")
            usuarioService.obtenerUsuario(uid)
                .onSuccess { usuario ->
                    Log.d(TAG, "Usuario cargado: radioAlertaKm=${usuario.radioAlertaKm}")
                    _uiState.value = _uiState.value.copy(usuario = usuario)
                }
                .onFailure { e ->
                    Log.e(TAG, "Error cargando usuario: ${e.message}")
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    private fun escucharReportes() {
        viewModelScope.launch {
            Log.d(TAG, "Iniciando escucha de reportes")
            reporteService.obtenerReportesMapaFlow().collect { lista ->
                Log.d(TAG, "Reportes actualizados: ${lista.size}")
                _uiState.value = _uiState.value.copy(reportes = lista)
            }
        }
    }

    fun guardarUbicacionUsuario(latitud: Double, longitud: Double) {
        val uid = authService.getUsuarioActualId() ?: run {
            Log.w(TAG, "No hay usuario autenticado para guardar ubicación")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Guardando ubicación: lat=$latitud, lng=$longitud")
            usuarioService.actualizarUbicacion(uid, latitud, longitud)
                .onSuccess { Log.d(TAG, "Ubicación guardada en Firestore") }
                .onFailure { e -> Log.e(TAG, "Error guardando ubicación: ${e.message}") }
        }
    }
}