package com.woleapp.netpos.qrgenerator.ui.webview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.databinding.FragmentWebViewBinding
import com.woleapp.netpos.qrgenerator.di.customDependencies.JavaScriptInterface
import com.woleapp.netpos.qrgenerator.di.customDependencies.WebViewCallBack
import com.woleapp.netpos.qrgenerator.utils.RESPONSE_FROM_WEBVIEW_FRAGMENT_BK
import com.woleapp.netpos.qrgenerator.utils.RESPONSE_FROM_WEBVIEW_FRAGMENT_RK
import com.woleapp.netpos.qrgenerator.utils.STRING_TAG_JAVASCRIPT_INTERFACE_TAG
import com.woleapp.netpos.qrgenerator.viewmodels.QRViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebViewFragment : Fragment() {

    private lateinit var binding: FragmentWebViewBinding
    private lateinit var webView: WebView
    private lateinit var webSettings: WebSettings
    private val qrViewModel by activityViewModels<QRViewModel>()
    private lateinit var javaScriptInterface: JavaScriptInterface

    @Inject
    lateinit var customWebViewClient: WebViewCallBack



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_web_view, container, false)

        // Handle Back Press
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        findNavController().popBackStack()
                    }
                }
            }
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = binding.webView
            qrViewModel.payResponse.observe(viewLifecycleOwner) { response ->
                Log.d("NEWRESULT", response.data.toString())
                response.data?.let {
                    javaScriptInterface = JavaScriptInterface(
                        parentFragmentManager,
                        it.TermUrl,
                        it.MD,
                        it.PaReq,
                        it.ACSUrl,
                        it.transId,
                        it.redirectHtml
                    ){
                        requireActivity().runOnUiThread {
                            requireActivity().supportFragmentManager.setFragmentResult(RESPONSE_FROM_WEBVIEW_FRAGMENT_RK,
                            bundleOf(RESPONSE_FROM_WEBVIEW_FRAGMENT_BK to true)
                            )
                            findNavController().popBackStack()
                        }
                    }
                }
                setUpWebView(webView)
            }
    }

    private fun setUpWebView(webView: WebView) {
        webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.apply {
            webViewClient = customWebViewClient
            webChromeClient = WebChromeClient()
            addJavascriptInterface(javaScriptInterface, STRING_TAG_JAVASCRIPT_INTERFACE_TAG)
            loadUrl("file:///android_asset/3ds_pay.html")
        }
    }
}