package com.catedra.eureka.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.services.ReporteService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReportesUiState(
    val cargando: Boolean = false,
    val misReportes: List<Reporte> = emptyList(),
    val error: String? = null
)

class ReportesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    private val reporteService = ReporteService()
    private val auth = FirebaseAuth.getInstance()
    val filtroEstado = MutableStateFlow("TODOS")
    val filtroMascota = MutableStateFlow("TODOS")
    val filtroFecha = MutableStateFlow("TODAS")

    val reportesFiltrados: StateFlow<List<Reporte>> = combine(_uiState, filtroEstado, filtroMascota, filtroFecha) {
            state, estado, mascota, fecha ->
        if (state.cargando || state.error != null) return@combine emptyList()

        state.misReportes.filter { reporte ->
            evaluarEstado(reporte, estado) &&
                    evaluarMascota(reporte, mascota) &&
                    evaluarFecha(reporte, fecha)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        cargarHistorialUsuario()
    }

    fun cargarHistorialUsuario() {
        val usuarioId = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(error = "Usuario no autenticado") }
            return
        }
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            reporteService.obtenerListaReportesUsuarioFlow(usuarioId)
                .catch { e ->
                    _uiState.update { it.copy(cargando = false, error = e.localizedMessage) }
                }
                .collect { reportes ->
                    _uiState.update { it.copy(cargando = false, misReportes = reportes) }
                }
        }
    }
    fun limpiarFiltros() {
        filtroEstado.value = "TODOS"
        filtroMascota.value = "TODOS"
        filtroFecha.value = "TODAS"
    }
    private fun evaluarEstado(reporte: Reporte, filtro: String): Boolean {
        if (filtro == "TODOS") return true
        return reporte.estado.equals(filtro, ignoreCase = true)
    }

    private fun evaluarMascota(reporte: Reporte, filtro: String): Boolean {
        if (filtro == "TODOS") return true
        return reporte.animal.equals(filtro, ignoreCase = true)
    }

    private fun evaluarFecha(reporte: Reporte, filtro: String): Boolean {
        if (filtro == "TODAS" || reporte.fechaCreacion == null) return true

        val fechaReporte = reporte.fechaCreacion.toDate()

        // Convertimos la fecha de este reporte a formato "yyyy-MM-dd"
        val formatoComparacion = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaReporteString = formatoComparacion.format(fechaReporte)

        return fechaReporteString == filtro
    }
}