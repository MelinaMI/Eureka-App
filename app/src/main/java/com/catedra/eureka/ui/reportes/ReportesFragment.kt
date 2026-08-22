package com.catedra.eureka.ui.reportes

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentReportesBinding
import com.catedra.eureka.utils.NavegacionHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportesFragment : Fragment() {

    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportesViewModel by viewModels()
    private lateinit var historialAdapter: ReporteAdapter
    private var fechaSeleccionadaString: String = "TODAS"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        configurarRecyclerView()
        observarEstado()
        configurarFiltros()
    }

    private fun configurarRecyclerView() {
        historialAdapter = ReporteAdapter { reporte ->
            NavegacionHelper.irADetalles(this, R.id.fragmentContainer, reporte.id)
        }

        binding.recyclerReportes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historialAdapter
            setHasFixedSize(true)
        }
    }

    private fun observarEstado() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                binding.progressBar.isVisible = estado.cargando
                binding.textError.isVisible = estado.error != null
                binding.textError.text = estado.error
                binding.textVacio.isVisible = !estado.cargando && estado.error == null && estado.misReportes.isEmpty()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reportesFiltrados.collect { listaFiltrada ->
                historialAdapter.submitList(listaFiltrada)
            }
        }
    }
    private fun configurarFiltros() {

        binding.btnToggleFiltros.setOnClickListener {
            val panel = binding.panelFiltrosDesplegable
            if (panel.visibility == View.VISIBLE) {
                panel.visibility = View.GONE
            } else {
                panel.visibility = View.VISIBLE
            }
        }

        binding.groupMascotaInline.setOnCheckedStateChangeListener { group, checkedIds ->
            val id = checkedIds.firstOrNull() ?: R.id.chipMascotaTodos
            val chip = group.findViewById<com.google.android.material.chip.Chip>(id)
            val seleccion = chip?.text?.toString() ?: "Todos"

            viewModel.filtroMascota.value = if (seleccion == "Todos") "TODOS" else seleccion
        }

        binding.groupEstadoInline.setOnCheckedStateChangeListener { group, checkedIds ->
            val id = checkedIds.firstOrNull() ?: R.id.chipEstadoTodos
            val chip = group.findViewById<com.google.android.material.chip.Chip>(id)
            val seleccion = chip?.text?.toString() ?: "Todos"

            viewModel.filtroEstado.value = if (seleccion == "Todos") "TODOS" else seleccion
        }

        binding.chipSeleccionarFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val año = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val día = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker =
                DatePickerDialog(requireContext(), { _, añoElegido, mesElegido, díaElegido ->
                    val calElegido = Calendar.getInstance().apply {
                        set(Calendar.YEAR, añoElegido)
                        set(Calendar.MONTH, mesElegido)
                        set(Calendar.DAY_OF_MONTH, díaElegido)
                    }

                    val formatoVisual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaVisual = formatoVisual.format(calElegido.time)
                    binding.chipSeleccionarFecha.text = fechaVisual
                    binding.chipSeleccionarFecha.isChecked = true

                    val formatoFiltro = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    fechaSeleccionadaString = formatoFiltro.format(calElegido.time)

                    viewModel.filtroFecha.value = fechaSeleccionadaString

                }, año, mes, día)

            datePicker.show()
        }

        binding.btnLimpiarFiltrosInline.setOnClickListener {
            binding.chipMascotaTodos.isChecked = true
            binding.chipEstadoTodos.isChecked = true
            binding.chipSeleccionarFecha.text = binding.chipSeleccionarFecha.context.getString(R.string.reportes_filtro_todas_fechas)
            binding.chipSeleccionarFecha.isChecked = true
            fechaSeleccionadaString = "TODAS"
            viewModel.limpiarFiltros()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}