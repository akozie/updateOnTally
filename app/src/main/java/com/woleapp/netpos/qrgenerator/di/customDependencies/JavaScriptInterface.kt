package com.woleapp.netpos.qrgenerator.di.customDependencies

import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.qrgenerator.ui.dialog.ResponseModal
import com.woleapp.netpos.qrgenerator.utils.*


class JavaScriptInterface(
    private val fragmentManager: FragmentManager,
    private val termUrl: String,
    private val md: String,
    private val cReq: String,
    private val acsUrl: String,
    private val transId: String,
    private val redirectHtml: String,
    private val popBackFragment: () -> Unit
) {
    private val context = fragmentManager.fragments.first().requireContext()
    private val loader = RandomUtils.alertDialog(context, R.layout.layout_loading_dialog)
    private val webViewBaseUrl =
        UtilityParam.STRING_WEB_VIEW_BASE_URL + UtilityParam.STRING_CHECKOUT_MERCHANT_ID + "/"

    @JavascriptInterface
    fun sendValueToWebView() =
        "$termUrl<======>$md<======>$cReq<======>$acsUrl<======>$transId<======>$webViewBaseUrl<======>$redirectHtml"

    @JavascriptInterface
    fun webViewCallback(webViewResponse: String) {
      val responseFromWebView = Gson().fromJson(
            webViewResponse,
            QrTransactionResponseModel::class.java
        )
        val registeredUserResponseFromWebView = Gson().fromJson(
            webViewResponse,
            QrTransactionResponseModel::class.java
        )
        if (responseFromWebView.code == "00" || responseFromWebView.code == "90" || responseFromWebView.code == "80"){
            if (loader.isShowing){
                fragmentManager.fragments.first().requireActivity().runOnUiThread {
                    loader.dismiss()
                }
            }
            fragmentManager.setFragmentResult(
                QR_TRANSACTION_RESULT_REQUEST_KEY,
                bundleOf(QR_TRANSACTION_RESULT_BUNDLE_KEY to responseFromWebView)
            )
            popBackFragment.invoke()
        }else{
            fragmentManager.fragments.first().requireActivity().runOnUiThread {
                loader.show()
            }
        }
    }
}
