package com.woleapp.netpos.qrgenerator.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.qrgenerator.R
import com.woleapp.netpos.qrgenerator.model.ErrorModel
import com.woleapp.netpos.qrgenerator.model.QrModelRequest
import com.woleapp.netpos.qrgenerator.model.Status
import com.woleapp.netpos.qrgenerator.model.checkout.CheckOutResponse
import com.woleapp.netpos.qrgenerator.model.wallet.SendWithTallyNumberResponse
import com.woleapp.netpos.qrgenerator.utils.RandomUtils.observeServerResponse
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import java.text.DecimalFormat
import java.util.*

object RandomUtils {

    fun <T> Fragment.observeServerResponse(
        serverResponse: Single<Resource<T>>,
        loadingDialog: AlertDialog,
        compositeDisposable: CompositeDisposable,
        ioScheduler: Scheduler,
        mainThreadSchedulers: Scheduler,
        successAction: () -> Unit
    ) {
        compositeDisposable.add(
            serverResponse.subscribeOn(ioScheduler).observeOn(mainThreadSchedulers)
                .subscribe { data, error ->
                    data?.let {
                        when (it.status) {
                            Status.SUCCESS -> {
                                loadingDialog.dismiss()
                                if (
                                    it.data is SendWithTallyNumberResponse
                                ) {
                                    successAction()
                                } else {
                                    showSnackBar(
                                        this.requireView(),
                                        getString(R.string.an_error_occurred)
                                    )
                                }
                            }
                            Status.LOADING -> {
                                loadingDialog.show()
                            }
                            Status.ERROR -> {
                                loadingDialog.cancel()
                                loadingDialog.dismiss()
                            }
                            Status.TIMEOUT -> {
                                loadingDialog.cancel()
                                loadingDialog.dismiss()
                                showSnackBar(this.requireView(), getString(R.string.timeOut))
                            }
                        }

                    }
                    error?.let {
                        loadingDialog.cancel()
                        loadingDialog.dismiss()
                        (it as? HttpException).let { httpException ->
                            val errorMessage = httpException?.response()?.errorBody()?.string()
                                ?: "{\"message\":\"Unexpected error\"}"
                            val errorMsg =
                                try {
                                    Gson().fromJson(errorMessage, ErrorModel::class.java).message
                                        ?: "Recipient is not a Tally user"
                                } catch (e: Exception) {
                                    "Error"
                                }
                            showSnackBar(this.requireView(), errorMsg)
                        }
                    }
                }
        )
    }


    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: AlertDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.d("MASTERCARDSUCCESS", "MASTERCARDSUCCESS")
                    loadingDialog.dismiss()
                    successAction()
                }
                Status.LOADING -> {
                    Log.d("LOADING", "LOADINGRESULT")
                    loadingDialog.show()
                }
                Status.ERROR -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                }
                Status.TIMEOUT -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                    showToast(getString(R.string.timeOut))
                }
            }
        }
    }

    fun <T> Fragment.observeServerResponseOnce(
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: AlertDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
                when (it.status) {
                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        successAction()
                    }
                    Status.LOADING -> {
                        loadingDialog.show()
                    }
                    Status.ERROR -> {
                        loadingDialog.cancel()
                        loadingDialog.dismiss()
                    }
                    Status.TIMEOUT -> {
                        loadingDialog.cancel()
                        loadingDialog.dismiss()
                        showToast(getString(R.string.timeOut))
                    }
                }
        }
        serverResponse.observeOnce(this.viewLifecycleOwner, null)
    }

    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: ProgressBar,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loadingDialog.visibility = View.GONE
                    successAction()
                }
                Status.LOADING -> {
                    loadingDialog.visibility = View.VISIBLE
                }
                Status.ERROR -> {
                    loadingDialog.visibility = View.GONE
                }
                Status.TIMEOUT -> {
                    loadingDialog.visibility = View.GONE
                    showToast(getString(R.string.timeOut))
                }
            }
        }
    }

    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>>,
        fragmentManager: FragmentManager,
        successAction: () -> Unit
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    successAction()
                }
                Status.LOADING -> {
                }
                Status.ERROR -> {
                }
                Status.TIMEOUT -> {
                    showToast(getString(R.string.timeOut))
                }
            }
        }
    }


    fun alertDialog(
        context: Context, layout: Int
    ): AlertDialog {
        val dialogView: View = LayoutInflater.from(context).inflate(layout, null)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setView(dialogView)

        return dialogBuilder.create()
    }

    fun getGUID() = UUID.randomUUID().toString().replace("-", "")

    fun stringToBase64(text: String): String {
        val data: ByteArray = text.toByteArray()
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    fun base64ToPlainText(base64String: String): String {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return String(decodedString)
    }

    fun closeSoftKeyboard(context: Context, activity: Activity) {
        activity.currentFocus?.let { view ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @Throws(IndexOutOfBoundsException::class)
    fun customSpannableString(
        text: String,
        startIndex: Int,
        endIndex: Int,
        clickAction: () -> Unit,
    ): SpannableString {
        val spannedText = SpannableString(text)
        val styleSpan = StyleSpan(Typeface.BOLD)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                clickAction()
            }
        }
        if (startIndex < 0) throw IndexOutOfBoundsException("$startIndex must be at least 0")
        if (text.isEmpty()) throw IndexOutOfBoundsException("$text can't be empty")
        if (endIndex > text.length) throw IndexOutOfBoundsException("$endIndex can't be greater than the length of $text")
        spannedText.setSpan(styleSpan, startIndex, endIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        spannedText.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
        return spannedText
    }


    fun createClientDataForNonVerveCard(
        transID: String, cardNumber: String, expiryDate: String, cvv: String
    ): String = "$transID:LIVE:$cardNumber:$expiryDate:$cvv::NGN:QR"

    fun createClientDataForVerveCard(
        qrModelRequest: QrModelRequest, pin: String, transID: String
    ): String {
        val verve = qrModelRequest.let {
            "$transID:LIVE:${it.card_number}:${it.card_expiry}:${it.card_cvv}:$pin:NGN:QR"
        }
        Log.d("VERVEEEE", verve)
        Prefs.putString(SAVE_CUSTOMER_DETAILS, "${qrModelRequest.fullname}===${qrModelRequest.email}")
        return verve
    }

    fun Number.formatCurrency(currencySymbol: String = "\u20A6"): String {
        val format = DecimalFormat("#,###.00")
        return "$currencySymbol${format.format(this)}"
    }

//    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
//        observe(lifecycleOwner) { t ->
//            observer.onChanged(t)
//            removeObservers(lifecycleOwner)
//        }
//    }

    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>?) {
        observe(lifecycleOwner, object : Observer<T> {
            override fun onChanged(t: T?) {
                observer?.onChanged(t)
                removeObserver(this)
            }
        })
    }
}