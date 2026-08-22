package com.catedra.eureka.data.services

import android.content.Context
import android.util.Log
import com.catedra.eureka.EurekaApplication
import com.catedra.eureka.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthService {

    private val TAG = "AuthService"

    private val _FirebaseAuth = FirebaseAuth.getInstance()
    private val _FirebaseFireStore = FirebaseFirestore.getInstance()
    private val usuariosLista = _FirebaseFireStore.collection("usuarios")

    suspend fun login(email: String, password: String, context: Context): Result<Unit> {
        if (email.isBlank() || password.isBlank())
            return Result.failure(Exception("Completá todos los campos"))
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Result.failure(Exception("El email no es válido"))
        if (password.length < 6)
            return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
        return try {
            _FirebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Login exitoso, iniciando listener de alertas")
            (context.applicationContext as EurekaApplication).alertaService.iniciar(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traducirError(e.message)))
        }
    }

    suspend fun register(email: String, password: String, nombre: String, context: Context): Result<Unit> {
        if (email.isBlank() || password.isBlank() || nombre.isBlank())
            return Result.failure(Exception("Completá todos los campos"))
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Result.failure(Exception("El email no es válido"))
        if (password.length < 6)
            return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
        if (nombre.length < 2)
            return Result.failure(Exception("El nombre debe tener al menos 2 caracteres"))
        return try {
            val result = _FirebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("No se pudo obtener el ID del usuario")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()
            val nuevoUsuario = Usuario(id = uid, nombre = nombre, email = email)
            usuariosLista.document(uid).set(nuevoUsuario).await()
            Log.d(TAG, "Registro exitoso, iniciando listener de alertas")
            (context.applicationContext as EurekaApplication).alertaService.iniciar(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(traducirError(e.message)))
        }
    }

    fun logout(context: Context) {
        Log.d(TAG, "Logout, deteniendo listener de alertas")
        (context.applicationContext as EurekaApplication).alertaService.detener()
        _FirebaseAuth.signOut()
    }

    fun getUsuarioActualId(): String? = _FirebaseAuth.currentUser?.uid

    fun estaLogueado(): Boolean = _FirebaseAuth.currentUser != null

    private fun traducirError(mensaje: String?): String {
        return when {
            mensaje == null -> "Ocurrió un error inesperado"
            "email address is already in use" in mensaje -> "El email ya está registrado"
            "password is invalid" in mensaje -> "La contraseña es incorrecta"
            "no user record" in mensaje -> "No existe una cuenta con ese email"
            "network error" in mensaje -> "Sin conexión a internet"
            "blocked all requests" in mensaje -> "Demasiados intentos, esperá unos minutos"
            else -> "Ocurrió un error inesperado"
        }
    }
}