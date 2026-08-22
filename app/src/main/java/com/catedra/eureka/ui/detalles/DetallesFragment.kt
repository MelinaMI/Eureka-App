package com.catedra.eureka.ui.detalles

import android.app.AlertDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentDetalleBinding
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class DetallesFragment : Fragment() {

    private var _binding: FragmentDetalleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetallesViewModel by viewModels()

    companion object {
        fun newInstance(reporteId: String) = DetallesFragment().apply {
            arguments = Bundle().apply { putString("reporte_id", reporteId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        _binding = FragmentDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarMapa()
        configurarListeners()
        observarEstado()
        val reporteId = arguments?.getString("reporte_id") ?: return
        viewModel.cargarDetalleReporte(reporteId)
    }

    private fun configurarMapa() {
        binding.mapaDetalle.setMultiTouchControls(true)
        binding.mapaDetalle.controller.setZoom(16.0)
    }

    private fun configurarListeners() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnEditarReporte.setOnClickListener {
            val reporteId = arguments?.getString("reporte_id") ?: return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, EditarReporteFragment.newInstance(reporteId))
                .addToBackStack(null)
                .commit()
        }
        binding.btnEliminarReporte.setOnClickListener {
            val reporteId = arguments?.getString("reporte_id") ?: return@setOnClickListener
            mostrarDialogoEliminar(reporteId)
        }
        binding.btnMarcarEncontrado.setOnClickListener {
            val reporteId = arguments?.getString("reporte_id") ?: return@setOnClickListener
            viewModel.cambiarAEncontrado(reporteId)
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                binding.progressBar.isVisible = estado.cargando

                val tieneError = estado.errorResId != null || !estado.errorMensajeDinamico.isNullOrEmpty()
                binding.textError.isVisible = tieneError
                binding.textError.text = when {
                    estado.errorResId != null -> getString(estado.errorResId)
                    else -> estado.errorMensajeDinamico ?: ""
                }

                binding.contenedorDatos.isVisible =
                    !estado.cargando && !tieneError && estado.nombreMostrado.isNotEmpty()
                binding.textVacio.isVisible =
                    !estado.cargando && !tieneError && estado.nombreMostrado.isEmpty()

                if (estado.nombreMostrado.isNotEmpty()) mostrarDatos(estado)

                if (estado.accionExitosa) {
                    viewModel.accionConsumida()
                    Toast.makeText(requireContext(), getString(R.string.detalle_accion_exitosa), Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun mostrarDatos(estado: DetalleUiState) {
        val valorDesconocido = getString(R.string.detalle_valor_desconocido)

        binding.txtNombreMascota.text = estado.nombreMostrado
        binding.txtDetalleEstado.text = getString(
            R.string.detalle_estado,
            estado.estadoResId?.let { getString(it) } ?: valorDesconocido
        )
        binding.txtAnimal.text = getString(
            R.string.detalle_animal,
            estado.animalResId?.let { getString(it) } ?: valorDesconocido
        )
        binding.txtFecha.text = getString(R.string.detalle_fecha, estado.fechaValor)
        val telMostrado = if (estado.telefonoValor == 0L) valorDesconocido else estado.telefonoValor.toString()
        binding.txtDetalleTelefono.text = getString(R.string.detalle_telefono, telMostrado)
        binding.txtSexo.text = getString(
            R.string.detalle_sexo,
            estado.sexoResId?.let { getString(it) } ?: valorDesconocido
        )
        binding.txtTamanio.text = getString(
            R.string.detalle_tamanio,
            estado.tamanioResId?.let { getString(it) } ?: valorDesconocido
        )
        binding.txtDetalleDescripcion.text = getString(R.string.detalle_descripcion, estado.descripcionValor)

        binding.btnEditarReporte.isVisible = estado.esMioElReporte
        binding.btnEliminarReporte.isVisible = estado.esMioElReporte
        binding.btnMarcarEncontrado.isVisible = estado.mostrarBotonEncontrado
        cargarFoto(estado.fotoUrl)
        mostrarEnMapa(estado.latitud, estado.longitud, estado.nombreMostrado)
    }

    private fun cargarFoto(url: String) {
        if (url.isEmpty()) return
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_account)
            .into(binding.imgDetalleMascota)
    }

    private fun mostrarEnMapa(latitud: Double, longitud: Double, titulo: String) {
        if (latitud == 0.0 && longitud == 0.0) return
        val posicion = GeoPoint(latitud, longitud)
        binding.mapaDetalle.controller.setCenter(posicion)
        binding.mapaDetalle.overlays.clear()
        binding.mapaDetalle.overlays.add(Marker(binding.mapaDetalle).apply {
            position = posicion
            title = titulo
        })
        binding.mapaDetalle.invalidate()
    }

    private fun mostrarDialogoEliminar(reporteId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.detalle_dialogo_eliminar_titulo))
            .setMessage(getString(R.string.detalle_dialogo_eliminar_msg))
            .setPositiveButton(getString(R.string.detalle_dialogo_eliminar_btn)) { _, _ ->
                viewModel.eliminarReporte(reporteId)
            }
            .setNegativeButton(getString(R.string.detalle_dialogo_cancelar_btn), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}