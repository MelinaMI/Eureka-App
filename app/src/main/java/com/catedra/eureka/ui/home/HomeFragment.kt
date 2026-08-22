package com.catedra.eureka.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentHomeBinding
import com.catedra.eureka.ui.reportes.EncontreUnaMascotaFragment
import com.catedra.eureka.ui.reportes.PerdiUnaMascotaFragment
import com.catedra.eureka.ui.reportes.ReporteAdapter
import com.catedra.eureka.utils.NavegacionHelper
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var misReportesAdapter: ReporteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()

        observarEstado()

        binding.btnPerdiMascota.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PerdiUnaMascotaFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnEncontreMascota.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EncontreUnaMascotaFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun configurarRecyclerView() {
        misReportesAdapter = ReporteAdapter { reporte ->
            NavegacionHelper.irADetalles(this, R.id.fragmentContainer, reporte.id)
        }

        binding.recyclerViewMisReportes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = misReportesAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                binding.progressBar.isVisible = estado.cargando
                misReportesAdapter.submitList(estado.misReportes)
                val mostrarVacio = !estado.cargando && estado.error == null && estado.misReportes.isEmpty()
                binding.textReportes.isVisible = mostrarVacio

                estado.error?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}