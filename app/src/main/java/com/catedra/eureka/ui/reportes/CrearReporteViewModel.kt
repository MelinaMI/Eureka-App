package com.catedra.eureka.ui.reportes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.R
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.model.ReporteError
import com.catedra.eureka.data.services.AuthService
import com.catedra.eureka.data.services.ReporteService
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CrearReporteUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

class CrearReporteViewModel(app: Application) : AndroidViewModel(app) {

    private val reporteService = ReporteService()
    private val authService = AuthService()
    private val context = app.applicationContext
    private val _uiState = MutableStateFlow(CrearReporteUiState())
    val uiState: StateFlow<CrearReporteUiState> = _uiState.asStateFlow()

    fun crearReporte(reporte: Reporte, latitud: Double, longitud: Double, direccion: String) {
        val usuarioId = authService.getUsuarioActualId() ?: run {
            _uiState.update { it.copy(error = "No hay sesión activa") }
            return
        }

        val reporteCompleto = reporte.copy(
            usuarioId = usuarioId,
            fechaCreacion = Timestamp.now(),
            latitud = latitud,
            longitud = longitud,
            direccion = direccion

        )

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            reporteService.crearReporte(reporteCompleto)
                .onSuccess { _uiState.update { it.copy(cargando = false, exito = true) } }
                .onFailure { e ->
                    val mensaje = if (e is ReporteService.ReporteException) {
                        when (e.error) {
                            is ReporteError.AnimalRequerido -> context.getString(R.string.error_animal_requerido)
                            is ReporteError.SexoRequerido -> context.getString(R.string.error_sexo_requerido)
                            is ReporteError.TamanioRequerido -> context.getString(R.string.error_tamanio_requerido)
                            is ReporteError.DescripcionRequerida -> context.getString(R.string.error_campo_requerido)
                            is ReporteError.DescripcionDemasiaoLarga -> context.getString(R.string.error_descripcion_larga)
                            is ReporteError.TelefonoRequerido -> context.getString(R.string.error_campo_requerido)
                            is ReporteError.TelefonoDemasiaoLargo -> context.getString(R.string.error_telefono_largo)
                            is ReporteError.FotoRequerida -> context.getString(R.string.error_foto_requerida)
                            is ReporteError.UbicacionRequerida -> context.getString(R.string.error_ubicacion_requerida)
                            is ReporteError.DireccionRequerida -> context.getString(R.string.error_campo_requerido)
                            is ReporteError.Desconocido -> e.error.mensaje ?: context.getString(R.string.error_generico)
                        }
                    } else {
                        e.message ?: context.getString(R.string.error_generico)
                    }
                    _uiState.update { it.copy(cargando = false, error = mensaje) }
                }
        }
    }

    fun exitoConsumido() {
        _uiState.update { it.copy(exito = false) }
    }

    fun errorConsumido() {
        _uiState.update { it.copy(error = null) }
    }
}