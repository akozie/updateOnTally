package com.woleapp.netpos.qrgenerator.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.chaos.view.PinView
import com.google.gson.Gson
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.databinding.FragmentEnterOtpBinding
import com.woleapp.netpos.qrgenerator.model.verve.VerveOTPResponse
import com.woleapp.netpos.qrgenerator.utils.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.qrgenerator.utils.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.qrgenerator.utils.RESPONSE_FROM_WEBVIEW_FRAGMENT_BK
import com.woleapp.netpos.qrgenerator.utils.RESPONSE_FROM_WEBVIEW_FRAGMENT_RK
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.alertDialog
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.closeSoftKeyboard
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponse
import com.woleapp.netpos.qrgenerator.viewmodels.QRViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EnterOtpFragment @Inject constructor() : Fragment() {
    private lateinit var resendCode: TextView
    private lateinit var otpView: PinView
    private lateinit var otpResentConfirmationText: TextView
    private lateinit var binding: FragmentEnterOtpBinding
    private lateinit var loader: android.app.AlertDialog

    @Inject
    lateinit var gson: Gson
    private val viewModel by activityViewModels<QRViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_otp, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loader = alertDialog(requireContext(), R.layout.layout_loading_dialog)
        initViews()
        otpView.requestFocus()
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(
            InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
        )
        otpView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.length == 6) {
                        closeSoftKeyboard(requireContext(), requireActivity())
                        viewModel.sendOtpForVerveCard(s.toString())
                        observeServerResponse(
                            viewModel.transactionResponseFromVerve,
                            loader,
                            requireActivity().supportFragmentManager
                        ) {
                            val transactionResponseFromVerve =
                                viewModel.transactionResponseFromVerve.value!!.data!! as VerveOTPResponse
                            if (transactionResponseFromVerve.code == "00" || transactionResponseFromVerve.code == "90" || transactionResponseFromVerve.code == "80") {
                                parentFragmentManager.setFragmentResult(
                                    QR_TRANSACTION_RESULT_REQUEST_KEY,
                                    bundleOf(QR_TRANSACTION_RESULT_BUNDLE_KEY to transactionResponseFromVerve)
                                )
                                Log.d("VERVERESPONSE", transactionResponseFromVerve.toString())
                                requireActivity().supportFragmentManager.setFragmentResult(
                                    RESPONSE_FROM_WEBVIEW_FRAGMENT_RK,
                                    bundleOf(RESPONSE_FROM_WEBVIEW_FRAGMENT_BK to true)
                                )
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
//        val resendCodeText = "Didn't Receive Code? Resend it" /*getString(R.string.resend_code)*/
//        val spannableText = customSpannableString(
//            resendCodeText,
//            resendCodeText.indexOf("Resend it"),
//            resendCodeText.length
//        ) {
//            val qrDataToResend = viewModel.retrieveQrPostPayloadFromSharedPrefs()
//            qrDataToResend?.let {
//                viewModel.setScannedQrIsVerveCard(true)
//                viewModel.postScannedQrRequestToServer(it)
//                observeServerResponse(
//                    viewModel.sendQrToServerResponse,
//                    loader,
//                    requireActivity().supportFragmentManager
//                ) {
//                    otpResentConfirmationText.visibility = View.VISIBLE
//                }
//            }
//        }
//        resendCode.text = spannableText
//        resendCode.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun initViews() {
        with(binding) {
            resendCode = resendOtp
            otpView = pinView
            otpResentConfirmationText = otpResent
        }
    }
}
