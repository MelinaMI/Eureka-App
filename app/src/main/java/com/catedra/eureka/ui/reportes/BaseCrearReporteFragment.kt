package com.catedra.eureka.ui.reportes

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.catedra.eureka.R
import com.catedra.eureka.data.model.EstadoReporte
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.model.TipoAnimal
import com.catedra.eureka.data.services.CloudinaryService
import com.catedra.eureka.utils.FotoUiHelper
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
data class DatosReporteUI(
    val animalLabel: String,
    val nombre: String,
    val sexo: String,
    val tamanio: String,
    val descripcion: String,
    val telefono: Long
)
abstract class BaseCrearReporteFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {

    protected val viewModel: CrearReporteViewModel by viewModels()
    protected lateinit var fotoService: FotoUiHelper
    protected var uriImagenLocal: Uri? = null
    protected abstract val estadoReporte: EstadoReporte

    protected var latitudSeleccionada: Double = 0.0
    protected var longitudSeleccionada: Double = 0.0
    protected var direccionSeleccionada: String = ""
    
    private var dialogoCarga: AlertDialog? = null

    protected abstract fun obtenerDatosUI(): DatosReporteUI
    protected abstract fun configurarVistasEspecificas()
    protected abstract fun mostrarVistaPreviaFoto(uri: Uri)
    protected abstract fun formularioCompleto(): Boolean
    protected abstract fun obtenerBotonEnviar(): Button?
    protected abstract fun mostrarTextoUbicacion(direccion: String)

    private val abrirMapaSelector = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult

        val data = result.data ?: return@registerForActivityResult
        val lat = data.getDoubleExtra("lat", 0.0)
        val lng = data.getDoubleExtra("lng", 0.0)

        if (lat == 0.0 || lng == 0.0) return@registerForActivityResult

        latitudSeleccionada = lat
        longitudSeleccionada = lng
        direccionSeleccionada = getString(R.string.coordenadas_formato, lat, lng)

        try {
            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
            val direcciones = geocoder.getFromLocation(lat, lng, 1)
            if (direcciones != null && direcciones.size > 0) {
                val primeraDireccion = direcciones.get(0)
                val lineaCalle = primeraDireccion.getAddressLine(0)
                if (lineaCalle != null) {
                    direccionSeleccionada = lineaCalle
                }
            }
        } catch (e: Exception) {
            // Error de red en Geocoder, mantiene el texto de coordenadas por defecto
        }

        actualizarTextoUbicacion(direccionSeleccionada)
    }

    private val pedirPermisoUbicacion = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        val fineConcedido = permisos.get(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseConcedido = permisos.get(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineConcedido == true || coarseConcedido == true) {
            ejecutarObtencionUbicacion()
        } else {
            Toast.makeText(requireContext(),  getString(R.string.error_permiso_ubicacion), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fotoService = FotoUiHelper(this) { uri ->
            uriImagenLocal = uri
            mostrarVistaPreviaFoto(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarDialogoCarga()
        configurarVistasEspecificas()
        obtenerBotonEnviar()?.isEnabled = false
        actualizarAparienciaBoton(obtenerBotonEnviar())
        observarEstado()
    }
    protected fun actualizarAparienciaBoton(boton: Button?) {
        boton ?: return
        val colorRes = if (boton.isEnabled) R.color.boton_activo else R.color.boton_deshabilitado
        val color = ContextCompat.getColor(requireContext(), colorRes)
        boton.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }
    protected fun actualizarEstadoBoton() {
        val boton = obtenerBotonEnviar()
        obtenerBotonEnviar()?.isEnabled = formularioCompleto()
        actualizarAparienciaBoton(boton)
    }
    private fun bloquearBotonPorCarga(cargando: Boolean) {
        val boton = obtenerBotonEnviar()
        obtenerBotonEnviar()?.isEnabled = if (cargando) false else formularioCompleto()
        actualizarAparienciaBoton(boton)
    }
    fun actualizarTextoUbicacion(direccion: String) {
        mostrarTextoUbicacion(direccion)
        actualizarEstadoBoton()
    }
    private fun configurarDialogoCarga() {
        val progressBar = android.widget.ProgressBar(requireContext()).apply {
            setPadding(40, 40, 40, 40)
        }

        dialogoCarga = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialogo_carga_titulo))
            .setMessage(R.string.dialogo_carga_mensaje)
            .setView(progressBar)
            .setCancelable(false)
            .create()
    }
    private fun gestionarVisibilidadLoading(mostrar: Boolean) {
        if (mostrar) {
            if (dialogoCarga?.isShowing == false) dialogoCarga?.show()
        } else {
            if (dialogoCarga?.isShowing == true) dialogoCarga?.dismiss()
        }
    }
    protected fun capturarUbicacion() {
        val fineLoc = android.Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLoc = android.Manifest.permission.ACCESS_COARSE_LOCATION
        val estadoPermiso = androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), fineLoc)
        if (estadoPermiso == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ejecutarObtencionUbicacion()
        } else {
            val listaPermisos = arrayOf(fineLoc, coarseLoc)
            pedirPermisoUbicacion.launch(listaPermisos)
        }
    }
    private fun ejecutarObtencionUbicacion() {
        val intent = Intent(requireContext(), SeleccionarUbicacionActivity::class.java)
        abrirMapaSelector.launch(intent)
    }
    protected fun procesarReporte() {
        val datos = obtenerDatosUI()

        val animalSeleccionado = TipoAnimal.entries
            .find { getString(it.nombreResId) == datos.animalLabel }?.name ?: TipoAnimal.PERRO.name

        val sexoNormalizado = when (datos.sexo.uppercase()) {
            getString(R.string.sexo_macho).uppercase() -> "MACHO"
            getString(R.string.sexo_hembra).uppercase() -> "HEMBRA"
            else -> "DESCONOCIDO"
        }

        val tamanioNormalizado = when (datos.tamanio.uppercase()) {
            getString(R.string.tamanio_pequeno).uppercase() -> "PEQUENO"
            getString(R.string.tamanio_mediano).uppercase() -> "MEDIANO"
            getString(R.string.tamanio_grande).uppercase() -> "GRANDE"
            else -> datos.tamanio
        }

        val nuevoReporte = Reporte(
            estado = estadoReporte.name,
            animal = animalSeleccionado,
            nombre = datos.nombre,
            sexo = sexoNormalizado,
            tamanio = tamanioNormalizado,
            descripcion = datos.descripcion,
            telefono = datos.telefono,
            fechaCreacion = Timestamp.now()
        )
        subirYPublicar(nuevoReporte)
    }
    private fun subirYPublicar(reporte: Reporte) {
        val uri = uriImagenLocal
        if (uri != null) {
            bloquearBotonPorCarga(true)
            gestionarVisibilidadLoading(true)
            CloudinaryService.subirFoto(
                uriLocal = uri,
                onExito = { urlPublicaCloudinary ->
                    viewModel.crearReporte(reporte.copy(fotoUrl = urlPublicaCloudinary), latitudSeleccionada, longitudSeleccionada, direccionSeleccionada)
                },
                onFallo = { error ->
                    bloquearBotonPorCarga(false)
                    gestionarVisibilidadLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.error_subir_imagen, error), Toast.LENGTH_LONG).show()
                }
            )
        } else {
            gestionarVisibilidadLoading(true)
            viewModel.crearReporte(reporte, latitudSeleccionada, longitudSeleccionada, direccionSeleccionada)
        }
    }
    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                bloquearBotonPorCarga(estado.cargando)
                gestionarVisibilidadLoading(estado.cargando)
                
                estado.error?.let { mensaje ->
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    viewModel.errorConsumido()
                }
                if (estado.exito) {
                    viewModel.exitoConsumido()
                    mostrarDialogoExito()
                }
            }
        }
    }
    private fun mostrarDialogoExito() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialogo_exito_titulo))
            .setMessage(getString(R.string.dialogo_exito_mensaje))
            .setPositiveButton(getString(R.string.dialogo_exito_boton)) { dialog, _ ->
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }
    override fun onDestroyView() {
        if (dialogoCarga?.isShowing == true) {
            dialogoCarga?.dismiss()
        }
        dialogoCarga = null
        super.onDestroyView()
    }
    protected fun AutoCompleteTextView.setupOptions(opciones: List<String>) {
        setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, opciones))
    }
}

