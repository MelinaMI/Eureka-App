package com.catedra.eureka.data.model

import androidx.annotation.StringRes
import com.catedra.eureka.R

enum class EstadoReporte(@StringRes val textoResId: Int) {
    PERDIDO(R.string.estado_perdido),
    ENCONTRADO(R.string.estado_encontrado),
    PUBLICADO(R.string.estado_publicado)
}