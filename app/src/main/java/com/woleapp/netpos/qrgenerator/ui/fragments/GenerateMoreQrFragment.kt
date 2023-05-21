package com.woleapp.netpos.qrgenerator.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.adapter.BankCardAdapter
import com.woleapp.netpos.qrgenerator.adapter.CardSchemeAdapter
import com.woleapp.netpos.qrgenerator.databinding.FragmentGenerateMoreQrBinding
import com.woleapp.netpos.qrgenerator.databinding.LayoutQrReceiptPdfBinding
import com.woleapp.netpos.qrgenerator.model.QrModelRequest
import com.woleapp.netpos.qrgenerator.model.Row
import com.woleapp.netpos.qrgenerator.model.RowX
import com.woleapp.netpos.qrgenerator.model.checkout.CheckOutModel
import com.woleapp.netpos.qrgenerator.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.qrgenerator.utils.*
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.alertDialog
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponse
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponseOnce
import com.woleapp.netpos.qrgenerator.viewmodels.QRViewModel
import java.io.File


class GenerateMoreQrFragment : Fragment() {

    private lateinit var binding: FragmentGenerateMoreQrBinding
    private val generateQrViewModel by activityViewModels<QRViewModel>()
    private lateinit var userFullName: TextInputEditText
    private lateinit var emailAddress: TextInputEditText
    private lateinit var phoneNumber: TextInputEditText
    private lateinit var qrIssuingBank: AutoCompleteTextView
    private lateinit var qrCardScheme: AutoCompleteTextView
    private lateinit var cardExpiryNumber: TextInputEditText
    private lateinit var cardExpiryDate: TextInputEditText
    private lateinit var cardExpiryCvv: TextInputEditText
    private lateinit var loader: AlertDialog
    private lateinit var submitBtn: Button
    private lateinit var bankCard: String
    private lateinit var cardScheme: String
    private lateinit var userEmail: String
    private lateinit var userName: String
    private lateinit var userPhoneNumber: String
    private var userId: Int? = 0
    private lateinit var receiptPdf: File
    private lateinit var pdfView: LayoutQrReceiptPdfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loader = alertDialog(requireContext(), R.layout.layout_loading_dialog)
        generateQrViewModel.getCardSchemes()
        generateQrViewModel.getCardBanks()
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PIN_BLOCK_RK, this
        ) { _, bundle ->
            val data = bundle.getString(PIN_BLOCK_BK)
            data?.let {
                val checkOutModel = getCheckOutModel()
                val qrModelRequest = getQrRequestModel()
                generateQrViewModel.displayQrStatus = 1
                generateQrViewModel.payQrChargesForVerve(checkOutModel, qrModelRequest, it)
                observeServerResponseOnce(
                    generateQrViewModel.payVerveResponse,
                    loader,
                    requireActivity().supportFragmentManager
                ) {
                    if (generateQrViewModel.payVerveResponse.value?.data?.code == "90") {
                        //      showToast(generateQrViewModel.payVerveResponse.value?.data?.result.toString())
                    } else {
                        Prefs.putString(PREF_GENERATE_QR, Gson().toJson(getQrRequestModel()))
                        if (findNavController().currentDestination?.id == R.id.generateMoreQrFragment) {
                            val action =
                                GenerateMoreQrFragmentDirections.actionGenerateMoreQrFragmentToEnterOtpFragment2()
                            findNavController().navigate(action)
                        } else {
                            findNavController().navigate(R.id.enterOtpFragment2)
                        }
//                        parentFragmentManager.beginTransaction()
//                            .replace(R.id.fragmentContainerView, EnterOtpFragment())
//                            .addToBackStack(null)
//                            .commit()
                    }
                }
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            RESPONSE_FROM_WEBVIEW_FRAGMENT_RK, this
        ) { _, bundle ->
            val data = bundle.getBoolean(RESPONSE_FROM_WEBVIEW_FRAGMENT_BK)
            if (data) {
                val action =
                    GenerateMoreQrFragmentDirections.actionGenerateMoreQrFragmentToResponseModal2()
                findNavController().navigate(action)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        pdfView = LayoutQrReceiptPdfBinding.inflate(layoutInflater)
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_generate_more_qr, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateQrViewModel.showQrPrintDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                val qrTransaction = Gson().fromJson(it, QrTransactionResponseModel::class.java)
                printQrTransactionUtil(qrTransaction)
            }
        }
        generateQrViewModel.generateQrMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                showToast(message)
            }
        }
        generateQrViewModel.payMessage.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                showToast(message)
            }
        }
        initViews()
        generateQrViewModel.cardSchemeResponse.observe(viewLifecycleOwner) {
            val cardSchemeAdapter = CardSchemeAdapter(
                generateQrViewModel.cardSchemes,
                requireContext(),
                android.R.layout.simple_expandable_list_item_1
            )
            qrCardScheme.setAdapter(cardSchemeAdapter)
        }
        generateQrViewModel.issuingBankResponse.observe(viewLifecycleOwner) {
            val bankCardAdapter = BankCardAdapter(
                generateQrViewModel.issuingBank,
                requireContext(),
                android.R.layout.simple_expandable_list_item_1
            )
            qrIssuingBank.setAdapter(bankCardAdapter)
        }
        userEmail = Singletons().getCurrentlyLoggedInUser()?.email.toString()
        binding.email.setText(userEmail)
        userName = Singletons().getCurrentlyLoggedInUser()?.fullname.toString()
        binding.fullName.setText(userName)
        userPhoneNumber = Singletons().getCurrentlyLoggedInUser()?.mobile_phone.toString()
        binding.mobileNumber.setText(userPhoneNumber)
        userId = Singletons().getCurrentlyLoggedInUser()?.id


//        submitBtn.setOnClickListener {
//            if (findNavController().currentDestination?.id == R.id.generateMoreQrFragment){
//                clearLiveData()
//            generateQr()
//            }else{
//                findNavController().popBackStack()
//            }
//        }
        submitBtn.setOnClickListener {
            generateQr()
        }

        cardExpiryDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().length == 2 && !s.toString().contains("/")) {
                    s!!.append("/")
                }
            }
        })

        qrCardScheme.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val category = adapterView?.getItemAtPosition(p2) as Row
                cardScheme = category.card_scheme
            }
        }
        qrIssuingBank.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val category = adapterView?.getItemAtPosition(p2) as RowX
                bankCard = category.bank_id
            }
        }


    }


    private fun initViews() {
        with(binding) {
            userFullName = fullName
            emailAddress = email
            phoneNumber = mobileNumber
            qrIssuingBank = issuingBank
            qrCardScheme = cardScheme
            cardExpiryNumber = cardNumber
            cardExpiryDate = expiryDate
            cardExpiryCvv = cardCvv
            submitBtn = btnGenerateQr
        }
    }

    private fun generateQr() {
        when {
            userFullName.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_full_name))
            }
            emailAddress.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_email))
            }
            phoneNumber.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_phone_number))
            }
            qrIssuingBank.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_issuing_bank))
            }
            qrIssuingBank.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_issuing_bank))
            }
            qrCardScheme.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_issuing_card_scheme))
            }
            cardExpiryNumber.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_card_expiry_number))
            }
            cardExpiryDate.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_card_date))
            }
            cardExpiryCvv.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_card_cvv))
            }
            else -> {
                if (validateSignUpFieldsOnTextChange()) {
                    checkOut()
                }
            }
        }
    }

    private fun validateSignUpFieldsOnTextChange(): Boolean {
        var isValidated = true

        userFullName.doOnTextChanged { _, _, _, _ ->
            when {
                userFullName.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_full_name))
                    isValidated = false
                }
                else -> {
                    binding.fragmentFullName.error = null
                    isValidated = true
                }
            }
        }
        emailAddress.doOnTextChanged { _, _, _, _ ->
            when {
                emailAddress.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_email))
                    isValidated = false
                }
                else -> {
                    binding.fragmentEmail.error = null
                    isValidated = true
                }
            }
        }
        phoneNumber.doOnTextChanged { _, _, _, _ ->
            when {
                phoneNumber.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_phone_number))
                    isValidated = false
                }
                else -> {
                    binding.fragmentMobileNumber.error = null
                    isValidated = true
                }
            }
        }
        qrIssuingBank.doOnTextChanged { _, _, _, _ ->
            when {
                qrIssuingBank.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_issuing_bank))
                    isValidated = false
                }
                else -> {
                    binding.fragmentSelectBank.error = null
                    isValidated = true
                }
            }
        }
        qrCardScheme.doOnTextChanged { _, _, _, _ ->
            when {
                qrCardScheme.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_issuing_card_scheme))
                    isValidated = false
                }
                else -> {
                    binding.fragmentSelectCardScheme.error = null
                    isValidated = true
                }
            }
        }
        cardExpiryNumber.doOnTextChanged { _, _, _, _ ->
            when {
                cardExpiryNumber.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_card_expiry_number))
                    isValidated = false
                }
                else -> {
                    binding.fragmentCardNumber.error = null
                    isValidated = true
                }
            }
        }
        cardExpiryDate.doOnTextChanged { _, _, _, _ ->
            when {
                cardExpiryDate.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_card_date))
                    isValidated = false
                }
                else -> {
                    binding.fragmentCardExpiryDate.error = null
                    isValidated = true
                }
            }
        }
        cardExpiryCvv.doOnTextChanged { _, _, _, _ ->
            when {
                cardExpiryCvv.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_card_cvv))
                    isValidated = false
                }
                else -> {
                    binding.fragmentCardCvv.error = null
                    isValidated = true
                }
            }
        }
        return isValidated
    }

    private fun checkOut() {
        val checkOutModel = getCheckOutModel()
        val qrRequest = getQrRequestModel()
        if (qrRequest.card_scheme.contains("verve", true)) {
            generateQrViewModel.setIsVerveCard(true)
            if (findNavController().currentDestination?.id == R.id.generateMoreQrFragment) {
                val action =
                    GenerateMoreQrFragmentDirections.actionGenerateMoreQrFragmentToQrPasswordPinBlockDialog2()
                findNavController().navigate(action)
            } else {
                findNavController().popBackStack()
            }
        } else {
            generateQrViewModel.setIsVerveCard(false)
            generateQrViewModel.displayQrStatus = 1
            generateQrViewModel.payQrCharges(checkOutModel, qrRequest)
            observeServerResponseOnce(
                generateQrViewModel.payResponse, loader, requireActivity().supportFragmentManager
            ) {
                if (generateQrViewModel.payResponse.value?.data?.code == "90") {
                    //  showToast(generateQrViewModel.payResponse.value?.data?.result.toString())
                } else {
                    Prefs.putString(PREF_GENERATE_QR, Gson().toJson(getQrRequestModel()))
                    if (findNavController().currentDestination?.id == R.id.generateMoreQrFragment) {
                        val action =
                            GenerateMoreQrFragmentDirections.actionGenerateMoreQrFragmentToWebViewFragment2()
                        findNavController().navigate(action)
                    } else {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun getQrRequestModel(): QrModelRequest = QrModelRequest(
        fullname = null,
        email = null,
        card_cvv = cardExpiryCvv.text.toString().trim(),
        card_expiry = cardExpiryDate.text.toString().trim(),
        card_number = cardExpiryNumber.text.toString().trim(),
        card_scheme = qrCardScheme.text.toString().trim(),
        issuing_bank = bankCard,
        mobile_phone = null,
        user_id = userId
    )

    private fun getCheckOutModel(): CheckOutModel = CheckOutModel(
        merchantId = UtilityParam.STRING_CHECKOUT_MERCHANT_ID,
        name = userFullName.text.toString().trim(),
        email = emailAddress.text.toString().trim(),
        amount = CHARGE_AMOUNT,
        currency = "NGN"
    )


    private fun printQrTransactionUtil(qrTransaction: QrTransactionResponseModel) {
        receiptPdf = createPdf(binding, this)
        downloadPflImplForQrTransaction(qrTransaction)
        showSnackBar(
            binding.root, "File downloaded"
        )
    }

    private fun downloadPflImplForQrTransaction(qrTransaction: QrTransactionResponseModel) {
        initViewsForPdfLayout(
            pdfView, qrTransaction
        )
        getPermissionAndCreatePdf(pdfView)
    }

    private fun getPermissionAndCreatePdf(view: ViewDataBinding) {
        genericPermissionHandler(
            requireActivity(),
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            115,
            "You need to grant permission to save this file"
        ) {
            receiptPdf = createPdf(view, this)
        }
    }

    private fun clearLiveData() {
        generateQrViewModel.payVerveResponse.removeObservers(viewLifecycleOwner)
        //    generateQrViewModel.payResponse.removeObservers(viewLifecycleOwner)
        generateQrViewModel.transactionResponseFromVerve.removeObservers(viewLifecycleOwner)
    }

}