package com.turkcell.lyraapp.data.network

import com.google.gson.Gson
import com.turkcell.lyraapp.data.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val refreshClient = OkHttpClient.Builder()
        .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

        val accessToken = tokenStorage.getAccessToken()
        val urlString = originalRequest.url.toString()

        if (!urlString.contains("/api/v1/auth/") && accessToken != null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401 && !urlString.contains("/api/v1/auth/")) {
            response.close()
            
            synchronized(this) {
                val currentAccessToken = tokenStorage.getAccessToken()
                val isTokenChanged = currentAccessToken != accessToken

                val finalAccessToken = if (isTokenChanged && currentAccessToken != null) {
                    currentAccessToken
                } else {
                    performTokenRefresh()
                }

                if (finalAccessToken != null) {
                    val newRequest = originalRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Authorization", "Bearer $finalAccessToken")
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    tokenStorage.clear()
                    tokenStorage.setLoggedIn(false)
                }
            }
        }

        return response
    }

    private fun performTokenRefresh(): String? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        val refreshRequest = RefreshRequest(refreshToken = refreshToken)
        val requestBodyString = gson.toJson(refreshRequest)

        val request = Request.Builder()
            .url("https://streaming-api.halitkalayci.com/api/v1/auth/refresh")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .post(requestBodyString.toRequestBody(jsonMediaType))
            .build()

        try {
            refreshClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return null
                    val refreshResponse = gson.fromJson(bodyString, RefreshResponse::class.java)
                    val newAccessToken = refreshResponse.data.accessToken
                    val newRefreshToken = refreshResponse.data.refreshToken
                    tokenStorage.saveAccessToken(newAccessToken)
                    tokenStorage.saveRefreshToken(newRefreshToken)
                    tokenStorage.setLoggedIn(true)
                    return newAccessToken
                } else {
                    return null
                }
            }
        } catch (e: IOException) {
            return null
        }
    }
}
