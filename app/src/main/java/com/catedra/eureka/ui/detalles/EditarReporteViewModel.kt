package com.catedra.eureka.ui.detalles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.R
import com.catedra.eureka.data.services.ReporteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditarReporteUiState(
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val errorResId: Int? = null,
    val errorMensajeDinamico: String? = null,
    val guardadoExitoso: Boolean = false,
    val nombre: String = "",
    val descripcion: String = "",
    val telefono: Long = 0L
)

class EditarReporteViewModel : ViewModel() {
    private val reporteService = ReporteService()
    private val _uiState = MutableStateFlow(EditarReporteUiState())
    val uiState: StateFlow<EditarReporteUiState> = _uiState.asStateFlow()

    fun cargarReporte(reporteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorResId = null, errorMensajeDinamico = null) }
            reporteService.escucharReportePorIdFlow(reporteId)
                .catch { e ->
                    _uiState.update { it.copy(
                        cargando = false,
                        errorMensajeDinamico = e.localizedMessage,
                        errorResId = if (e.localizedMessage == null) R.string.editar_error_cargar else null
                    )}
                }
                .collect { reporte ->
                    if (reporte != null) {
                        _uiState.update { it.copy(
                            cargando = false,
                            nombre = reporte.nombre,
                            descripcion = reporte.descripcion,
                            telefono = reporte.telefono
                        )}
                    } else {
                        _uiState.update { it.copy(
                            cargando = false,
                            errorResId = R.string.editar_error_no_encontrado
                        )}
                    }
                }
        }
    }


    fun guardarCambios(reporteId: String, nombre: String, descripcion: String, telefono: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errorResId = null, errorMensajeDinamico = null) }
            reporteService.editarReporte(
                reporteId = reporteId,
                nombre = nombre.trim(),
                descripcion = descripcion.trim(),
                telefono = telefono.trim().toLongOrNull() ?: 0L
            )
                .onSuccess {
                    _uiState.update { it.copy(guardando = false, guardadoExitoso = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        guardando = false,
                        errorMensajeDinamico = e.localizedMessage,
                        errorResId = if (e.localizedMessage == null) R.string.editar_error_guardar else null
                    )}
                }
        }
    }

    fun errorConsumido() {
        _uiState.update { it.copy(errorResId = null, errorMensajeDinamico = null) }
    }
}