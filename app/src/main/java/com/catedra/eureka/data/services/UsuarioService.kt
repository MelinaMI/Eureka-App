package com.catedra.eureka.data.services

import android.util.Log
import com.catedra.eureka.data.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioService {
    private val _FirebaseFireStore = FirebaseFirestore.getInstance()
    private val usuariosLista = _FirebaseFireStore.collection("usuarios")
    
    

    suspend fun obtenerUsuario(id: String): Result<Usuario> {
        return try {
            val doc = usuariosLista.document(id).get().await()
            val usuario = doc.toObject(Usuario::class.java) ?: throw Exception("Usuario no encontrado")
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    

    suspend fun actualizarUbicacion(id: String, latitud: Double, longitud: Double): Result<Unit> {
        return try {
            Log.d("UsuarioService", "Actualizando ubicación de usuario $id: lat=$latitud, lng=$longitud")
            usuariosLista.document(id).update(
                mapOf(
                    "latitud" to latitud,
                    "longitud" to longitud
                )
            ).await()
            Log.d("UsuarioService", "Ubicación actualizada correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UsuarioService", "Error actualizando ubicación: ${e.message}")
            Result.failure(e)
        }
    }
    

    suspend fun actualizarAlertas(id: String, activadas: Boolean, radioKm: Int): Result<Unit> {
        return try {
            usuariosLista.document(id).update(
                mapOf(
                    "alertasActivadas" to activadas,
                    "radioAlertaKm" to radioKm
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}