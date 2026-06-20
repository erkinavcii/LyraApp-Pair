package com.turkcell.lyraapp.data.auth

/**
 * Kimlik doğrulama ve oturum yönetimi işlemlerinin tek soyutlama noktası.
 */
interface AuthRepository {

    /**
     * Telefon numarasına OTP kodu talep eder.
     * @return Başarılı ise kullanıcının ilk kez mi kayıt olduğunu (firstTime) belirten Boolean değeri döner.
     */
    suspend fun requestOtp(phone: String): Result<Boolean>

    /**
     * Gönderilen OTP kodunu doğrular.
     * @return Başarılı ise kullanıcının profilinin tamamlanması gerekip gerekmediğini (firstTime) belirten Boolean değeri döner.
     */
    suspend fun verifyOtp(phone: String, code: String): Result<Boolean>

    /**
     * Kullanıcının profil bilgilerini günceller.
     */
    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        birthDate: String
    ): Result<Unit>

    /**
     * Mevcut oturumu kapatır ve temizler.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Giriş yapmış kullanıcının tam adını döner.
     */
    fun getLoggedInUserName(): String?

    /**
     * Kullanıcının halihazırda giriş yapıp yapmadığını kontrol eder.
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Tercih edilen tema modunu (Dark/Light) kaydeder.
     */
    fun setThemeDarkMode(isDark: Boolean)

    /**
     * Kaydedilmiş tema tercihini döner.
     */
    fun isDarkModePreferred(): Boolean
}
