package com.catedra.eureka.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val navegarAlHome: Boolean = false
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = AuthService()
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun registrar(email: String, password: String, nombre: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            authService.register(email, password, nombre, getApplication())
                .onSuccess {
                    _uiState.update { it.copy(cargando = false, navegarAlHome = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(cargando = false, error = e.message ?: "Error al crear la cuenta") }
                }
        }
    }

    fun navegacionConsumida() {
        _uiState.update { it.copy(navegarAlHome = false) }
    }
}