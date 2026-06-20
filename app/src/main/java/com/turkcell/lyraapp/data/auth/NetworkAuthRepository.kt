package com.turkcell.lyraapp.data.auth

import android.content.Context
import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.data.network.OtpRequest
import com.turkcell.lyraapp.data.network.OtpVerifyRequest
import com.turkcell.lyraapp.data.network.UpdateInfoRequest
import com.turkcell.lyraapp.data.network.LogoutRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAuthRepository @Inject constructor(
    private val apiService: LyraApiService,
    private val tokenStorage: TokenStorage,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("lyra_prefs", Context.MODE_PRIVATE)

    override suspend fun requestOtp(phone: String): Result<Boolean> {
        return try {
            val response = apiService.requestOtp(OtpRequest(phone))
            Result.success(response.data.firstTime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<Boolean> {
        return try {
            val response = apiService.verifyOtp(OtpVerifyRequest(phone, code))
            val verifyData = response.data
            
            tokenStorage.saveAccessToken(verifyData.accessToken)
            tokenStorage.saveRefreshToken(verifyData.refreshToken)
            tokenStorage.saveUserPhone(verifyData.user.phone)
            val displayName = verifyData.user.displayName ?: "${verifyData.user.firstName ?: ""} ${verifyData.user.lastName ?: ""}".trim()
            tokenStorage.saveUserName(displayName)
            tokenStorage.setLoggedIn(true)
            
            Result.success(verifyData.firstTime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        birthDate: String
    ): Result<Unit> {
        return try {
            val response = apiService.updateInformations(
                UpdateInfoRequest(
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate
                )
            )
            val user = response.data
            val displayName = user.displayName ?: "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
            tokenStorage.saveUserName(displayName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken != null) {
                apiService.logout(LogoutRequest(refreshToken))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        } finally {
            tokenStorage.clear()
            tokenStorage.setLoggedIn(false)
        }
    }

    override fun getLoggedInUserName(): String? {
        return tokenStorage.getUserName()
    }

    override fun isUserLoggedIn(): Boolean {
        return tokenStorage.isLoggedIn()
    }

    override fun setThemeDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_THEME_DARK, isDark).apply()
    }

    override fun isDarkModePreferred(): Boolean {
        return prefs.getBoolean(KEY_THEME_DARK, true)
    }

    private companion object {
        const val KEY_THEME_DARK = "theme_dark"
    }
}
