package com.catedra.eureka.ui.reportes

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.catedra.eureka.R
import com.catedra.eureka.data.model.EstadoReporte
import com.catedra.eureka.data.model.TipoAnimal
import com.catedra.eureka.databinding.FragmentReportePerdidoBinding

class PerdiUnaMascotaFragment : BaseCrearReporteFragment(R.layout.fragment_reporte_perdido) {

    private var _binding: FragmentReportePerdidoBinding? = null
    private val binding get() = _binding!!

    override val estadoReporte = EstadoReporte.PERDIDO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReportePerdidoBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun configurarVistasEspecificas(){
        with(binding) {
            dropdownAnimal.setupOptions(TipoAnimal.entries.map { getString(it.nombreResId) })
            dropdownSexo.setupOptions(listOf( getString(R.string.sexo_macho), getString(R.string.sexo_hembra), getString(R.string.sexo_desconocido)))
            dropdownTamanio.setupOptions(listOf( getString(R.string.tamanio_pequeno), getString(R.string.tamanio_mediano), getString(R.string.tamanio_grande)))
            editTextDescripcion.doAfterTextChanged { actualizarEstadoBoton() }
            editTextTelefono.doAfterTextChanged { actualizarEstadoBoton() }

            fabUbicacion.isEnabled = true
            fabUbicacion.alpha = 1.0f
            textUbicacion.text = getString(R.string.reporte_ubicacion_defecto)
            textUbicacion.alpha = 1.0f
            fabUbicacion.setOnClickListener {capturarUbicacion()}
            toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
            btnEnviarReporte.text = getString(R.string.reporte_btn_publicar)
            fabFoto.setOnClickListener { fotoService.abrirGaleria() }
            btnEnviarReporte.setOnClickListener { procesarReporte() }
            uriImagenLocal?.let { mostrarVistaPreviaFoto(it) }
        }
    }

    override fun obtenerDatosUI() = with(binding) {
        DatosReporteUI(
            animalLabel = dropdownAnimal.text.toString(),
            nombre = editTextNombre.text.toString().trim(),
            sexo = dropdownSexo.text.toString(),
            tamanio = dropdownTamanio.text.toString(),
            descripcion = editTextDescripcion.text.toString().trim(),
            telefono = editTextTelefono.text.toString().toLongOrNull() ?: 0L
        )
    }

    override fun mostrarVistaPreviaFoto(uri: Uri) = with(binding) {
        imgFotoPreview.isVisible = true
        imgFotoPreview.setImageURI(uri)
        fabFoto.alpha = 1.0f
    }


    override fun mostrarTextoUbicacion(direccion: String) {
        binding.textUbicacion.text = direccion
    }

    override fun formularioCompleto(): Boolean = with(binding) {
        dropdownAnimal.text.isNotBlank() &&
                dropdownSexo.text.isNotBlank() &&
                dropdownTamanio.text.isNotBlank() &&
                editTextNombre.text.toString().isNotBlank() &&
                editTextDescripcion.text.toString().isNotBlank() &&
                editTextTelefono.text.toString().isNotBlank() &&
                longitudSeleccionada != 0.0
    }
    override fun obtenerBotonEnviar(): Button? = _binding?.btnEnviarReporte

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}