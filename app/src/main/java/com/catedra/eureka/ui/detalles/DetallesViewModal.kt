package com.catedra.eureka.ui.detalles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.R
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.services.ReporteService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class DetalleUiState(
    val cargando: Boolean = false,
    val errorResId: Int? = null,
    val errorMensajeDinamico: String? = null,
    val accionExitosa: Boolean = false,
    val nombreMostrado: String = "",
    val estadoResId: Int? = null,
    val animalResId: Int? = null,
    val fechaValor: String = "",
    val telefonoValor: Long = 0L,
    val sexoResId: Int? = null,
    val tamanioResId: Int? = null,
    val descripcionValor: String = "",
    val direccionValor: String = "",
    val fotoUrl: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val mostrarBotonEncontrado: Boolean = false,
    val esMioElReporte: Boolean = false
)

class DetallesViewModel : ViewModel() {

    private val reporteService = ReporteService()
    private val formateadorFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val _uiState = MutableStateFlow(DetalleUiState())
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    fun cargarDetalleReporte(reporteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorResId = null, errorMensajeDinamico = null) }
            reporteService.escucharReportePorIdFlow(reporteId)
                .catch { e ->
                    _uiState.update { it.copy(
                        cargando = false,
                        errorMensajeDinamico = e.localizedMessage ?: "",
                        errorResId = if (e.localizedMessage == null) R.string.detalle_error_servidor else null
                    )}
                }
                .collect { reporte ->
                    if (reporte != null) {
                        _uiState.update { it.copy(
                            cargando = false,
                            nombreMostrado = reporte.nombre.ifBlank { reporte.animal },
                            estadoResId = estadoAResId(reporte.estado),
                            animalResId = animalAResId(reporte.animal),
                            fechaValor = formatearFecha(reporte.fechaCreacion),
                            telefonoValor = reporte.telefono,
                            sexoResId = sexoAResId(reporte.sexo),
                            tamanioResId = tamanioAResId(reporte.tamanio),
                            descripcionValor = reporte.descripcion,
                            direccionValor = reporte.direccion,
                            fotoUrl = reporte.fotoUrl,
                            latitud = reporte.latitud,
                            longitud = reporte.longitud,
                            mostrarBotonEncontrado = calcularMostrarBoton(reporte),
                            esMioElReporte = reporte.usuarioId == FirebaseAuth.getInstance().currentUser?.uid
                        )}
                    } else {
                        _uiState.update { it.copy(
                            cargando = false,
                            errorResId = R.string.detalle_error_inexistente
                        )}
                    }
                }
        }
    }

    fun cambiarAEncontrado(reporteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            reporteService.marcarComoEncontrado(reporteId)
                .onSuccess {
                    _uiState.update { it.copy(cargando = false, accionExitosa = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        cargando = false,
                        errorMensajeDinamico = e.localizedMessage,
                        errorResId = if (e.localizedMessage == null) R.string.detalle_error_actualizar else null
                    )}
                }
        }
    }

    fun eliminarReporte(reporteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorResId = null, errorMensajeDinamico = null) }
            reporteService.eliminarReporte(reporteId)
                .onSuccess {
                    _uiState.update { it.copy(cargando = false, accionExitosa = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        cargando = false,
                        errorMensajeDinamico = e.localizedMessage,
                        errorResId = if (e.localizedMessage == null) R.string.detalle_error_eliminar else null
                    )}
                }
        }
    }

    fun accionConsumida() {
        _uiState.update { it.copy(accionExitosa = false) }
    }

    private fun estadoAResId(estado: String): Int = when (estado.uppercase()) {
        "PERDIDO" -> R.string.estado_perdido
        "ENCONTRADO" -> R.string.estado_encontrado
        "PUBLICADO" -> R.string.estado_publicado
        else -> R.string.detalle_valor_desconocido
    }

    private fun animalAResId(animal: String): Int = when (animal.uppercase()) {
        "PERRO" -> R.string.animal_perro
        "GATO" -> R.string.animal_gato
        "CONEJO" -> R.string.animal_conejo
        "TORTUGA" -> R.string.animal_tortuga
        else -> R.string.detalle_valor_desconocido
    }

    private fun sexoAResId(sexo: String): Int? = when (sexo.uppercase()) {
        "MACHO" -> R.string.sexo_macho
        "HEMBRA" -> R.string.sexo_hembra
        else -> null
    }

    private fun tamanioAResId(tamanio: String): Int? = when (tamanio.uppercase()) {
        "PEQUEÑO", "PEQUENO" -> R.string.tamanio_pequeno
        "MEDIANO" -> R.string.tamanio_mediano
        "GRANDE" -> R.string.tamanio_grande
        else -> null
    }

    private fun formatearFecha(timestamp: com.google.firebase.Timestamp): String {
        return formateadorFecha.format(timestamp.toDate())
    }

    private fun calcularMostrarBoton(reporte: Reporte): Boolean {
        val uidActual = FirebaseAuth.getInstance().currentUser?.uid
        return reporte.usuarioId == uidActual && reporte.estado != "ENCONTRADO"
    }
}