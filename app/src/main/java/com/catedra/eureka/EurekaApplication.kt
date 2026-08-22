package com.catedra.eureka

import android.app.Application
import android.util.Log
import com.catedra.eureka.data.services.AlertaService

class EurekaApplication : Application() {

    private val TAG = "EurekaApplication"

    val alertaService: AlertaService by lazy { AlertaService() }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application creada")
    }
}