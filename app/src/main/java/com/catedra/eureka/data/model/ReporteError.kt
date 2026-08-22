package com.catedra.eureka.data.model

sealed class ReporteError {
    object AnimalRequerido : ReporteError()
    object SexoRequerido : ReporteError()
    object TamanioRequerido : ReporteError()
    object DescripcionRequerida : ReporteError()
    object DescripcionDemasiaoLarga : ReporteError()
    object TelefonoRequerido : ReporteError()
    object TelefonoDemasiaoLargo : ReporteError()
    object FotoRequerida : ReporteError()
    object UbicacionRequerida : ReporteError()
    object DireccionRequerida : ReporteError()
    data class Desconocido(val mensaje: String?) : ReporteError()
}