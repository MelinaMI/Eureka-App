package com.catedra.eureka.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

object IdiomaHelper {

    private const val TAG = "IdiomaHelper"
    private const val PREFS_NAME = "eureka_prefs"
    private const val KEY_IDIOMA = "idioma_seleccionado"

    const val IDIOMA_ES = "es"
    const val IDIOMA_EN = "en"
    const val IDIOMA_PT = "pt"
    const val IDIOMA_FR = "fr"

    fun aplicarIdioma(context: Context): Context {
        val idioma = obtenerIdiomaGuardado(context)
        Log.d(TAG, "Aplicando idioma: $idioma")
        return aplicarLocale(context, idioma)
    }

    fun cambiarIdioma(context: Context, codigoIdioma: String): Context {
        Log.d(TAG, "Cambiando idioma a: $codigoIdioma")
        guardarIdioma(context, codigoIdioma)
        return aplicarLocale(context, codigoIdioma)
    }

    fun obtenerIdiomaGuardado(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idioma = prefs.getString(KEY_IDIOMA, IDIOMA_ES) ?: IDIOMA_ES
        Log.d(TAG, "Idioma guardado: $idioma")
        return idioma
    }

    private fun guardarIdioma(context: Context, codigoIdioma: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IDIOMA, codigoIdioma)
            .apply()
        Log.d(TAG, "Idioma guardado en SharedPreferences: $codigoIdioma")
    }

    private fun aplicarLocale(context: Context, codigoIdioma: String): Context {
        val locale = Locale(codigoIdioma)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        Log.d(TAG, "Locale aplicado: $locale")
        return context.createConfigurationContext(config)
    }
}