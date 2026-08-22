package com.catedra.eureka.ui.detalles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentEditarReporteBinding
import kotlinx.coroutines.launch

class EditarReporteFragment : Fragment() {

    private var _binding: FragmentEditarReporteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditarReporteViewModel by viewModels<EditarReporteViewModel>()

    companion object {
        fun newInstance(reporteId: String) = EditarReporteFragment().apply {
            arguments = Bundle().apply { putString("reporte_id", reporteId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditarReporteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reporteId = arguments?.getString("reporte_id") ?: return

        viewModel.cargarReporte(reporteId)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGuardar.setOnClickListener {
            viewModel.guardarCambios(
                reporteId = reporteId,
                nombre = binding.editTextNombre.text.toString(),
                descripcion = binding.editTextDescripcion.text.toString(),
                telefono = binding.editTextTelefono.text.toString()
            )
        }

        observarEstado()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    binding.progressBar.isVisible = estado.cargando || estado.guardando
                    binding.contenedorFormulario.isVisible = !estado.cargando
                    binding.btnGuardar.isEnabled = !estado.guardando

                    if (estado.nombre.isNotEmpty() && binding.editTextNombre.text.isNullOrEmpty()) {
                        binding.editTextNombre.setText(estado.nombre)
                        binding.editTextDescripcion.setText(estado.descripcion)

                        // Conversión de Long a String para evitar el error de tipado.
                        val telefonoStr = if (estado.telefono > 0L) estado.telefono.toString() else ""
                        binding.editTextTelefono.setText(telefonoStr)
                    }

                    val tieneError = estado.errorResId != null || !estado.errorMensajeDinamico.isNullOrEmpty()
                    if (tieneError) {
                        val msg = when {
                            estado.errorResId != null -> getString(estado.errorResId)
                            else -> estado.errorMensajeDinamico ?: ""
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        viewModel.errorConsumido()
                    }

                    if (estado.guardadoExitoso) {
                        Toast.makeText(requireContext(), getString(R.string.editar_exito_guardado), Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}