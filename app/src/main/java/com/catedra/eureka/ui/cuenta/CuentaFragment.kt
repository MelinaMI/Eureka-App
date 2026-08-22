package com.catedra.eureka.ui.cuenta

import android.app.AlertDialog
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentCuentaBinding
import com.catedra.eureka.ui.login.LoginFragment
import com.catedra.eureka.utils.IdiomaHelper
import kotlinx.coroutines.launch

class CuentaFragment : Fragment() {

    companion object {
        fun newInstance() = CuentaFragment()
    }

    private var _binding: FragmentCuentaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CuentaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                binding.textNombreUsuario.text = estado.nombre
                binding.textEmailUsuario.text = estado.email

                if (binding.switchAlertas.isChecked != estado.alertasActivadas) {
                    binding.switchAlertas.isChecked = estado.alertasActivadas
                }

                if (binding.sliderRadio.value != estado.radioAlertaKm.toFloat()) {
                    binding.sliderRadio.value = estado.radioAlertaKm.toFloat()
                }

                // Distancia de notificacion, cambiamos el idioma desde acá porque no está en el xml
                binding.textRadioKm.text = getString(R.string.cuenta_distancia, estado.radioAlertaKm)
                
                binding.sliderRadio.isEnabled = estado.alertasActivadas
            }
        }

        binding.switchAlertas.setOnCheckedChangeListener { _, isChecked ->
            viewModel.actualizarPreferenciaAlertas(isChecked, binding.sliderRadio.value.toInt())
        }

        binding.sliderRadio.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.actualizarPreferenciaAlertas(binding.switchAlertas.isChecked, value.toInt())
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCerrarSesion.setOnClickListener {
            viewModel.cerrarSesion()
            navegarAlLogin()
        }

        binding.btnIdioma.setOnClickListener {
            val opciones = arrayOf("Español", "English", "Português", "Francés")
            val codigos = arrayOf(IdiomaHelper.IDIOMA_ES, IdiomaHelper.IDIOMA_EN, IdiomaHelper.IDIOMA_PT, IdiomaHelper.IDIOMA_FR)
            val actual = IdiomaHelper.obtenerIdiomaGuardado(requireContext())
            val seleccionado = codigos.indexOf(actual).coerceAtLeast(0)

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.cuenta_idioma))
                .setSingleChoiceItems(opciones, seleccionado) { dialog, which ->
                    Log.d("CuentaFragment", "Idioma seleccionado: ${codigos[which]}")
                    IdiomaHelper.cambiarIdioma(requireContext(), codigos[which])
                    dialog.dismiss()
                    requireActivity().recreate()
                }
                .show()
        }
    }

    private fun navegarAlLogin() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}