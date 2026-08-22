package com.catedra.eureka.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.catedra.eureka.MainActivity
import com.catedra.eureka.R
import com.catedra.eureka.databinding.FragmentRegisterBinding
import com.catedra.eureka.ui.home.HomeFragment
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    companion object {
        private const val CLAVE_EMAIL = "email"
        private const val CLAVE_PASSWORD = "password"

        fun newInstance(email: String, password: String): RegisterFragment {
            return RegisterFragment().apply {
                arguments = Bundle().apply {
                    putString(CLAVE_EMAIL, email)
                    putString(CLAVE_PASSWORD, password)
                }
            }
        }
    }

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    private var captchaResultadoCorrecto = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = arguments?.getString(CLAVE_EMAIL) ?: ""
        val password = arguments?.getString(CLAVE_PASSWORD) ?: ""
        
        binding.editTextEmail.setText(email)
        binding.editTextPassword.setText(password)

        generarCaptcha()

        binding.buttonConfirmarRegistro.setOnClickListener {
            val nombre = binding.editTextNombre.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()      
            val password = binding.editTextPassword.text.toString()     
            val repetirPassword = binding.editTextRepeatPassword.text.toString()
            val captchaUsuario = binding.editTextCaptchaRespuesta.text.toString().trim()

            if (password != repetirPassword) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.register_error_passwords)
                return@setOnClickListener
            }

            if (captchaUsuario.isEmpty() || captchaUsuario.toIntOrNull() != captchaResultadoCorrecto) {
                binding.textError.isVisible = true
                binding.textError.text = getString(R.string.register_error_captcha)
                generarCaptcha() 
                return@setOnClickListener
            }

            viewModel.registrar(email, password, nombre)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                binding.progressBar.isVisible = estado.cargando
                binding.buttonConfirmarRegistro.isEnabled = !estado.cargando

                binding.textError.isVisible = estado.error != null
                binding.textError.text = estado.error ?: ""

                if (estado.navegarAlHome) {
                    viewModel.navegacionConsumida()
                    navegarAlHome()
                }
            }
        }
    }

    private fun generarCaptcha() {
        val captchaA = (1..9).random()
        val captchaB = (1..9).random()
        captchaResultadoCorrecto = captchaA + captchaB
        binding.textCaptchaPregunta.text = getString(R.string.register_captcha_pregunta, captchaA, captchaB)
        binding.editTextCaptchaRespuesta.text?.clear()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.mostrarNavbar(false)
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.mostrarNavbar(true)
    }

    private fun navegarAlHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}