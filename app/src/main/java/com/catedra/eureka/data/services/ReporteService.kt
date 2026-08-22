
package com.catedra.eureka.data.services

import com.catedra.eureka.data.model.EstadoReporte
import com.catedra.eureka.data.model.Reporte
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.catedra.eureka.data.model.ReporteError
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ReporteService {
    private val _FirebaseFireStore = FirebaseFirestore.getInstance()
    private val reportesLista = _FirebaseFireStore.collection("reportes")

    suspend fun obtenerPorId(reporteId: String): Result<Reporte?> {
        return try {
            val doc = reportesLista.document(reporteId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(Reporte::class.java)?.copy(id = doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class ReporteException(val error: ReporteError) : Exception()

    suspend fun crearReporte(reporte: Reporte): Result<Unit> {
        if (reporte.animal.isBlank())
            return Result.failure(ReporteException(ReporteError.AnimalRequerido))
        if (reporte.sexo.isBlank())
            return Result.failure(ReporteException(ReporteError.SexoRequerido))
        if (reporte.tamanio.isBlank())
            return Result.failure(ReporteException(ReporteError.TamanioRequerido))
        if (reporte.descripcion.isBlank())
            return Result.failure(ReporteException(ReporteError.DescripcionRequerida))
        if (reporte.descripcion.length > 150)
            return Result.failure(ReporteException(ReporteError.DescripcionDemasiaoLarga))
        if (reporte.telefono <= 0)
            return Result.failure(ReporteException(ReporteError.TelefonoRequerido))
        if (reporte.telefono.toString().length > 10)
            return Result.failure(ReporteException(ReporteError.TelefonoDemasiaoLargo))
        if (reporte.fotoUrl.isBlank())
            return Result.failure(ReporteException(ReporteError.FotoRequerida))
        if (reporte.latitud == 0.0 && reporte.longitud == 0.0)
            return Result.failure(ReporteException(ReporteError.UbicacionRequerida))
        if (reporte.direccion.isBlank())
            return Result.failure(ReporteException(ReporteError.DireccionRequerida))

        return try {
            val docRef = if (reporte.id.isEmpty()) reportesLista.document()
            else reportesLista.document(reporte.id)
            docRef.set(reporte.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerTodos(): Result<List<Reporte>> {
        return try {
            val snapshot = reportesLista
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .await()
            Result.success(snapshot.documents.map { doc ->
                doc.toObject(Reporte::class.java)!!.copy(id = doc.id)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerDelUsuario(usuarioId: String): Result<List<Reporte>> {
        return try {
            val snapshot = reportesLista
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()

            val listaOrdenada = snapshot.documents
                .map { doc -> doc.toObject(Reporte::class.java)!!.copy(id = doc.id) }
                .sortedByDescending { it.fechaCreacion }

            Result.success(listaOrdenada)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun escucharReportePorIdFlow(reporteId: String): Flow<Reporte?> = callbackFlow {
        val listener = reportesLista.document(reporteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reporte = snapshot?.toObject(Reporte::class.java)?.copy(id = snapshot.id)
                trySend(reporte)
            }
        awaitClose { listener.remove() }
    }

    fun obtenerListaReportesUsuarioFlow(usuarioId: String): Flow<List<Reporte>> = callbackFlow {
        val listener = reportesLista
            .whereEqualTo("usuarioId", usuarioId)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReporteService", "Error en listener usuario: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.map { doc ->
                    doc.toObject(Reporte::class.java)!!.copy(id = doc.id)
                } ?: emptyList()
                Log.d("ReporteService", "Reportes usuario recibidos: ${lista.size}")
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }

    fun obtenerReportesMapaFlow(): Flow<List<Reporte>> = callbackFlow {
        Log.d("ReporteService", "Iniciando listener en tiempo real de Firestore")

        val listener: ListenerRegistration = reportesLista
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReporteService", "Error en listener de reportes: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val lista = snapshot.documents.map { doc ->
                        doc.toObject(Reporte::class.java)!!.copy(id = doc.id)
                    }
                    Log.d("ReporteService", "Snapshot recibido: ${lista.size} reportes")
                    trySend(lista)
                }
            }

        awaitClose {
            Log.d("ReporteService", "Cerrando listener en tiempo real de Firestore")
            listener.remove()
        }
    }

    suspend fun marcarComoEncontrado(reporteId: String): Result<Unit> {
        return try {
            reportesLista.document(reporteId)
                .update(
                    "estado", EstadoReporte.ENCONTRADO.name,
                    "latitud", 0.0,
                    "longitud", 0.0,
                    "direccion", ""
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarReporte(reporteId: String): Result<Unit> {
        return try {
            reportesLista.document(reporteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editarReporte(reporteId: String, nombre: String, descripcion: String, telefono: Long): Result<Unit> {
        return try {
            reportesLista.document(reporteId)
                .update(
                    "nombre", nombre,
                    "descripcion", descripcion,
                    "telefono", telefono
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
