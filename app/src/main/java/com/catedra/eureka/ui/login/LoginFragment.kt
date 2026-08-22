package com.catedra.eureka.ui.login

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.lifecycleScope
import com.catedra.eureka.MainActivity
import com.catedra.eureka.R
import com.catedra.eureka.ui.home.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.catedra.eureka.databinding.FragmentLoginBinding
import com.catedra.eureka.ui.register.RegisterFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            viewModel.login(email,password)
        }

        binding.buttonRegistrar.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            // Navega al registro pasando los datos como argumentos
            val fragment = RegisterFragment.newInstance(email, password)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null) // para poder volver con el back
                .commit()
        }
        

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                // binding.root.isVisible = estado.cargando
                binding.textError.isVisible=estado.error!=null
                binding.textError.text=estado.error ?: ""

                if (estado.navegarAlHome)
                {
                    viewModel.navegacionConsumida()
                    navegarAlHome()
                }
            }

        }
    }
    //ESCONDE LA NAVBAR CUANDO SE MUESTRA EL LOGIN
    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.mostrarNavbar(false)
    }

    //MUESTRA LA NAVBAR CUANDO SE DIRECCIONA AL HOME
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

