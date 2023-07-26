package com.Meditation.Sounds.frequencies.lemeor.data.api

import android.content.Context
import com.Meditation.Sounds.frequencies.BuildConfig
import com.Meditation.Sounds.frequencies.lemeor.data.api.ApiConfig.TIME_OUT
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class RetrofitBuilder(val context: Context) {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if(BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else
            HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = ApiInterceptor(context)

    private val client: OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
        .readTimeout(TIME_OUT, TimeUnit.SECONDS)
        .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .retryOnConnectionFailure(true)
        .addInterceptor(ChuckerInterceptor.Builder(context).build())
        .build()

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }


//    private fun provideUnsafeOkhttpClient(context: Context): OkHttpClient {
//        /* Trust anything*/
//        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//            override fun getAcceptedIssuers(): Array<X509Certificate> {
//                return emptyArray()
//            }
//
//            @Throws(CertificateException::class)
//            override fun checkClientTrusted(
//                chain: Array<X509Certificate>,
//                authType: String
//            ) {
//            }
//
//            @Throws(CertificateException::class)
//            override fun checkServerTrusted(
//                chain: Array<X509Certificate>,
//                authType: String
//            ) {
//            }
//        })
//
//        val sslContext = SSLContext.getInstance("SSL")
//        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
//        val sslSocketFactory = sslContext.socketFactory
//
//        val client = OkHttpClient.Builder()
//        client.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//        client.hostnameVerifier { _, _ -> true }
//        /* Rest of config*/
//        client.connectTimeout(TIME_OUT, TimeUnit.SECONDS)
//            .readTimeout(TIME_OUT, TimeUnit.SECONDS)
//            .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
//            .addInterceptor(loggingInterceptor)
//            .addInterceptor(authInterceptor)
//            .addInterceptor(ChuckerInterceptor.Builder(context).build())
//        if (!BuildConfig.DEBUG) {
//            throw RuntimeException("You fool. Do not use this in production!!!")
//        }
//
//        return client.build()
//    }

    val apiService: ApiService = getRetrofit().create(ApiService::class.java)
}