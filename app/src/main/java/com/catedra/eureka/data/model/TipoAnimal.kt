package com.catedra.eureka.data.model

import androidx.annotation.StringRes
import com.catedra.eureka.R

enum class TipoAnimal(@StringRes val nombreResId: Int) {
    PERRO(R.string.animal_perro),
    GATO(R.string.animal_gato),
    CONEJO(R.string.animal_conejo),
    TORTUGA(R.string.animal_tortuga)
}