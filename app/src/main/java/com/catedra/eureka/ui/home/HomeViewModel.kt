package com.catedra.eureka.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.services.AuthService
import com.catedra.eureka.data.services.ReporteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val cargando: Boolean = false,
    val misReportes: List<Reporte> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val reporteService = ReporteService()
    private val authService = AuthService()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init {
        cargarMisReportes()
    }
    fun cargarMisReportes() {
        val miId = authService.getUsuarioActualId()
        if (miId == null) {
            _uiState.update { it.copy(error = "Usuario no autenticado") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            reporteService.obtenerListaReportesUsuarioFlow(miId)
                .catch { e ->
                    _uiState.update { it.copy(cargando = false, error = e.message) }
                }
                .collect { reportes ->
                    _uiState.update { it.copy(cargando = false, misReportes = reportes) }
                }
        }
    }
}