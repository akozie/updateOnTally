package com.woleapp.netpos.qrgenerator.network

import com.google.gson.JsonObject
import com.woleapp.netpos.qrgenerator.model.checkout.CheckOutModel
import com.woleapp.netpos.qrgenerator.model.checkout.CheckOutResponse
import com.woleapp.netpos.qrgenerator.model.pay.PayModel
import com.woleapp.netpos.qrgenerator.model.pay.PayResponse
import com.woleapp.netpos.qrgenerator.model.verve.PostQrToServerVerveResponseModel
import com.woleapp.netpos.qrgenerator.model.verve.SendOtpForVerveCardModel
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CheckoutService {

    @GET("v2/checkout")
    fun checkOut(
        @Query("merchantId") merchantId : String,
        @Query("name") name : String,
        @Query("email") email : String,
        @Query("amount") amount : Double,
        @Query("currency") currency : String,
        @Query("orderId") orderId : String,
    ): Single<CheckOutResponse>

    @POST("v2/pay")
    fun pay(
        @Body payModel: PayModel
    ): Single<JsonObject>


    @POST("v2/pay")
    fun sendOtpForVerveCard(@Body sendOtpPayLoad: SendOtpForVerveCardModel): Single<Response<JsonObject>>
}