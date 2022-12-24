package com.woleapp.netpos.qrgenerator.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.databinding.FragmentSignInOrGenerateQRBinding


class SignInOrGenerateQRFragment : Fragment() {

    private var _binding: FragmentSignInOrGenerateQRBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding =  FragmentSignInOrGenerateQRBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener {
            val action = SignInOrGenerateQRFragmentDirections.actionSignInOrGenerateQRFragmentToSignInFragment()
            findNavController().navigate(action)
        }
        binding.generateQrButton.setOnClickListener {
            val action = SignInOrGenerateQRFragmentDirections.actionSignInOrGenerateQRFragmentToGenerateQrFragment()
            findNavController().navigate(action)
        }
    }
}