package com.woleapp.netpos.qrgenerator.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.databinding.FragmentSignUpBinding
import com.woleapp.netpos.qrgenerator.model.RegisterRequest
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponse
import com.woleapp.netpos.qrgenerator.utils.showToast
import com.woleapp.netpos.qrgenerator.viewmodels.QRViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {


    private lateinit var _binding: FragmentSignUpBinding
    private val binding get() = _binding
    private lateinit var fullName: TextInputEditText
    private lateinit var emailAddress: TextInputEditText
    private lateinit var phoneNumber: TextInputEditText
    private lateinit var passwordView: TextInputEditText
    private lateinit var signupButton: Button
    private val qrViewModel by viewModels<QRViewModel>()
    private lateinit var loader: ProgressBar
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrViewModel.registerMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                showToast(message)
                signupButton.isEnabled = true
            }
        }
        loader = binding.signUpProgressBar
        initViews()
        signupButton.setOnClickListener {
            signup()
            signupButton.isEnabled = false
        }
    }

    private fun initViews() {
        with(binding) {
            fullName = signUpFullName
            phoneNumber = signUpMobileNumber
            passwordView = signUpPassword
            emailAddress = signUpEmail
            signupButton = signUpButton
        }
    }

    private fun signup() {
        when {
            emailAddress.text.toString().isEmpty() -> {
                binding.fragmentEnterEmail.error =
                    getString(R.string.all_please_enter_email)
                binding.fragmentEnterEmail.errorIconDrawable = null
            }
            passwordView.text.toString().isEmpty() -> {
                binding.fragmentEnterPassword.error =
                    getString(R.string.all_please_enter_password)
                binding.fragmentEnterPassword.errorIconDrawable = null
            }
            else -> {
                if (validateSignUpFieldsOnTextChange()) {
                    register()
                }
            }
        }
    }

    private fun validateSignUpFieldsOnTextChange(): Boolean {
        var isValidated = true

        fullName.doOnTextChanged { _, _, _, _ ->
            when {
                fullName.text.toString().trim().isEmpty() -> {
                    binding.fragmentEnterFullName.error =
                        getString(R.string.all_please_enter_full_name)
                    isValidated = false
                }
                emailAddress.text.toString().trim().isNotEmpty() -> {
                    binding.fragmentEnterFullName.error = ""
                    isValidated = true
                }
                else -> {
                    binding.fragmentEnterFullName.error = null
                    isValidated = true
                }
            }
        }
        emailAddress.doOnTextChanged { _, _, _, _ ->
            when {
                emailAddress.text.toString().trim().isEmpty() -> {
                    binding.fragmentEnterEmail.error =
                        getString(R.string.all_please_enter_email)
                    isValidated = false
                }
                emailAddress.text.toString().trim().isNotEmpty() -> {
                    binding.fragmentEnterEmail.error = ""
                    isValidated = true
                }
                else -> {
                    binding.fragmentEnterEmail.error = null
                    isValidated = true
                }
            }
        }
        phoneNumber.doOnTextChanged { _, _, _, _ ->
            when {
                phoneNumber.text.toString().trim().isEmpty() -> {
                    binding.fragmentEnterMobileNumber.error =
                        getString(R.string.all_please_enter_phone_number)
                    isValidated = false
                }
                emailAddress.text.toString().trim().isNotEmpty() -> {
                    binding.fragmentEnterMobileNumber.error = ""
                    isValidated = true
                }
                else -> {
                    binding.fragmentEnterMobileNumber.error = null
                    isValidated = true
                }
            }
        }
        passwordView.doOnTextChanged { _, _, _, _ ->
            when {
                passwordView.text.toString().trim().isEmpty() -> {
                    binding.fragmentEnterPassword.error =
                        getString(R.string.all_please_enter_password)
                    isValidated = false
                }
                passwordView.text.toString().trim().isEmpty() -> {
                    binding.fragmentEnterPassword.error = ""
                    isValidated = true
                }
                else -> {
                    binding.fragmentEnterPassword.error = null
                    isValidated = true
                }
            }
        }
        return isValidated
    }

    private fun register() {
        val registerRequest = RegisterRequest(
            fullname = fullName.text.toString().trim(),
            email = emailAddress.text.toString().trim(),
            mobile_phone = phoneNumber.text.toString().trim(),
            password = passwordView.text.toString().trim()
        )
        qrViewModel.register(
            registerRequest
        )
        observeServerResponse(
            qrViewModel.registerResponse,
            loader,
            requireActivity().supportFragmentManager
        ) {
            val action = SignUpFragmentDirections.actionSignUpFragmentToSignInFragment()
            findNavController().navigate(action)
        }
    }

}