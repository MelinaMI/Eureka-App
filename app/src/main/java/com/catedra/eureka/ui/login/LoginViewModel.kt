package com.catedra.eureka.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val navegarAlHome: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = AuthService()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            authService.login(email, password, getApplication())
                .onSuccess {
                    _uiState.update { it.copy(cargando = false, navegarAlHome = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(cargando = false, error = e.message ?: "Error al iniciar sesión") }
                }
        }
    }

    fun navegacionConsumida() {
        _uiState.update { it.copy(navegarAlHome = false) }
    }
}