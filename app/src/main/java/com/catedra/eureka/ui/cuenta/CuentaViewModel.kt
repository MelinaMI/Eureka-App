package com.catedra.eureka.ui.cuenta

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.eureka.data.services.AuthService
import com.catedra.eureka.data.services.UsuarioService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CuentaViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = AuthService()
    private val usuarioService = UsuarioService()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    data class UserUiState(
        val nombre: String = "Cargando...",
        val email: String = "Cargando...",
        val alertasActivadas: Boolean = true,
        val radioAlertaKm: Int = 5
    )

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState

    init {
        cargarDatosDeColeccion()
    }

    private fun cargarDatosDeColeccion() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    val snapshot = db.collection("usuarios").document(uid).get().await()
                    if (snapshot.exists()) {
                        val nombreDb = snapshot.getString("nombre") ?: "Sin nombre"
                        val emailDb = snapshot.getString("email") ?: "Sin email"
                        val alertasDb = snapshot.getBoolean("alertasActivadas") ?: true
                        val radioDb = snapshot.getLong("radioAlertaKm")?.toInt() ?: 5
                        _uiState.value = UserUiState(
                            nombre = nombreDb,
                            email = emailDb,
                            alertasActivadas = alertasDb,
                            radioAlertaKm = radioDb
                        )
                    } else {
                        _uiState.value = UserUiState(
                            nombre = "Usuario Nuevo",
                            email = auth.currentUser?.email ?: "Sin email"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = UserUiState(
                        nombre = "Error al cargar",
                        email = "Error al cargar"
                    )
                }
            }
        } else {
            _uiState.value = UserUiState(nombre = "Invitado", email = "No autenticado")
        }
    }

    fun actualizarPreferenciaAlertas(activadas: Boolean, radioKm: Int) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(alertasActivadas = activadas, radioAlertaKm = radioKm)
        viewModelScope.launch {
            usuarioService.actualizarAlertas(uid, activadas, radioKm)
        }
    }

    fun cerrarSesion() {
        authService.logout(getApplication())
    }
}