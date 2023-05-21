package com.woleapp.netpos.qrgenerator.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.databinding.LayoutQrReceiptPdfBinding
import com.woleapp.netpos.qrgenerator.databinding.TransactionStatusModalBinding
import com.woleapp.netpos.qrgenerator.model.pay.ModalData
import com.woleapp.netpos.qrgenerator.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.qrgenerator.model.verve.VerveOTPResponse
import com.woleapp.netpos.qrgenerator.utils.QR_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.qrgenerator.utils.QR_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.formatCurrency
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponse
import com.woleapp.netpos.qrgenerator.utils.Singletons
import com.woleapp.netpos.qrgenerator.utils.initViewsForPdfLayout
import com.woleapp.netpos.qrgenerator.viewmodels.QRViewModel
import com.woleapp.netpos.qrgenerator.viewmodels.TransactionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResponseModal : DialogFragment() {
    private lateinit var binding: TransactionStatusModalBinding
    private lateinit var lottieIcon: LottieAnimationView
    private lateinit var statusTv: TextView
    private lateinit var cancelBtn: ImageView
    private lateinit var downloadReceipt: Button
    private lateinit var viewGeneratedQR: Button
    private lateinit var amountTv: TextView
    private var modalData: ModalData? = null
    private lateinit var pdfView: LayoutQrReceiptPdfBinding
    private var responseFromWebView: Any? = null
    private val transactionViewModel by activityViewModels<TransactionViewModel>()
    private val qrViewModel by activityViewModels<QRViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(QR_TRANSACTION_RESULT_REQUEST_KEY) { _, bundle ->
            responseFromWebView = bundle.getParcelable(QR_TRANSACTION_RESULT_BUNDLE_KEY)
            val webViewResponse = responseFromWebView
            modalData = if (webViewResponse != null) {
                when (webViewResponse) {
                    is QrTransactionResponseModel -> {
                        if (webViewResponse.code == "00") {
                            viewGeneratedQR.visibility = View.VISIBLE
                            val getQrModel = Singletons().getSavedQrModelRequest()
                            getQrModel?.let {
                                qrViewModel.generateQR(it, requireContext())
                            }
                        }
                        transactionViewModel.saveQrTransaction(webViewResponse)
                        ModalData(
                            webViewResponse.code == "00", webViewResponse.amount.toDouble()
                        )
                    }
                    is VerveOTPResponse -> {
                        if (webViewResponse.code == "00") {
                            viewGeneratedQR.visibility = View.VISIBLE
                            val getQrModel = Singletons().getSavedQrModelRequest()
                            getQrModel?.let {
                                qrViewModel.generateQR(it, requireContext())
                            }
                        }
                        transactionViewModel.saveQrTransaction(webViewResponse.mapTOQrTransactionModel())
                        ModalData(
                            webViewResponse.code == "00", webViewResponse.amount.toDouble()
                        )
                    }
                    else -> {
                        ModalData(
                            false, 0.0
                        )
                    }
                }
            } else {
                ModalData(
                    false, 0.0
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        pdfView =
            DataBindingUtil.inflate(inflater, R.layout.layout_qr_receipt_pdf, container, false)
        binding =
            DataBindingUtil.inflate(inflater, R.layout.transaction_status_modal, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.drawable.curve_bg)
            isCancelable = false
        }
        initViewsForPdfLayout(
            pdfView, responseFromWebView
        )
    }

    override fun onResume() {
        super.onResume()
        setData()
        cancelBtn.setOnClickListener {
            if (qrViewModel.displayQrStatus == 0) {
                dialog?.dismiss()
//                Prefs.remove(PREF_GENERATE_QR)
//                startActivity(Intent(requireContext(), AuthenticationActivity::class.java))
//                requireActivity().finish()
            } else {
                dialog?.dismiss()
//                Prefs.remove(PREF_GENERATE_QR)
//                startActivity(Intent(requireContext(), MainActivity::class.java))
//                requireActivity().finish()
            }
        }
        downloadReceipt.setOnClickListener {
            if (responseFromWebView is QrTransactionResponseModel) {
                responseFromWebView?.let { qrTransResponse ->
                    qrViewModel.setQrTransactionResponse(qrTransResponse as QrTransactionResponseModel)
                    qrViewModel.showReceiptDialogForQrPayment()
                }
            } else {
                responseFromWebView?.let { qrTransResponse ->
                    qrViewModel.setQrTransactionResponse((qrTransResponse as VerveOTPResponse).mapTOQrTransactionModel())
                    qrViewModel.showReceiptDialogForQrPayment()
                }
            }

        }
        viewGeneratedQR.setOnClickListener {
            dialog?.dismiss()
            viewQr()
        }
    }

    private fun initViews() {
        with(binding) {
            lottieIcon = statusIconLAV
            statusTv = successFailed
            cancelBtn = cancelButton
            amountTv = qrAmount
            downloadReceipt = printReceipt
            viewGeneratedQR = viewGeneratedQr
        }
    }

    private fun viewQr() {
        observeServerResponse(
            qrViewModel.generateQrResponse, requireActivity().supportFragmentManager
        ) {
            if (qrViewModel.displayQrStatus == 0) {
                //        Prefs.remove(PREF_GENERATE_QR)
                findNavController().navigate(R.id.showQrFragment)
            } else {
                //        Prefs.remove(PREF_GENERATE_QR)
                findNavController().navigate(R.id.displayQrFragment2)
            }
        }
    }

    private fun setData() {
        modalData?.let {
            if (it.status) {
                lottieIcon.setAnimation(R.raw.lottiesuccess)
                statusTv.text = getString(R.string.success)
                statusTv.setTextColor(resources.getColor(R.color.success))
            } else {
                lottieIcon.setAnimation(R.raw.failed)
                statusTv.text = getString(R.string.failed)
                statusTv.setTextColor(resources.getColor(R.color.failed))
            }
            amountTv.text = it.amount.toLong().formatCurrency("\u20A6")
        }
    }
}
