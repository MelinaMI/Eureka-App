package com.catedra.eureka.data.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val alertasActivadas: Boolean = true,
    val radioAlertaKm: Int = 5,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)