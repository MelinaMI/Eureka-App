package com.catedra.eureka.data.services

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback


object CloudinaryService {

    private var inicializado = false

    fun inicializar(context: Context) {
        if (!inicializado) {
            val config = mapOf(
                "cloud_name" to "dwqopizlp"
            )
            MediaManager.init(context, config)
            inicializado = true
        }
    }
    fun subirFoto(
        uriLocal: Uri,
        onExito: (String) -> Unit,
        onFallo: (String) -> Unit
    ) {
        MediaManager.get().upload(uriLocal)
            .unsigned("eureka_preset")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val urlSegura = resultData["secure_url"] as? String ?: ""
                    onExito(urlSegura)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onFallo(error.description ?: "Error desconocido al subir a Cloudinary")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}