package com.catedra.eureka.data.model

import com.google.firebase.Timestamp


data class Reporte(
    val id: String = "",
    val estado: String = "",
    val animal: String = "",
    val nombre: String = "",
    val sexo: String = "",
    val tamanio: String = "",
    val descripcion: String = "",
    val telefono: Long = 0L,
    val fotoUrl: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val fechaCreacion: Timestamp = Timestamp.now(),
    val usuarioId: String = ""
)