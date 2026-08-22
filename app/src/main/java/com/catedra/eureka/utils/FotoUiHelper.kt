package com.catedra.eureka.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

class FotoUiHelper(private val fragment: Fragment, private val onFotoSeleccionada: (Uri) -> Unit){
    private val context: Context get() = fragment.requireContext()
    private var uriFotoCamara: Uri? = null
    private val buscarEnGaleriaLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onFotoSeleccionada(it) }
    }
    private val tomarFotoLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exitoso: Boolean ->
        if (exitoso) {
            uriFotoCamara?.let { onFotoSeleccionada(it) }
        }
    }
    private val pedirPermisoCamaraLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { aprobado: Boolean ->
        if (aprobado) {
            ejecutarCamara()
        } else {
            Toast.makeText(context, "Se necesita el permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirGaleria() {
        buscarEnGaleriaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            ejecutarCamara()
        } else {
            pedirPermisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun ejecutarCamara() {
        val archivoTemporal = File.createTempFile("foto_reporte_", ".jpg", context.cacheDir)
        uriFotoCamara = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            archivoTemporal
        )
        tomarFotoLauncher.launch(uriFotoCamara!!)
    }
}