package com.turkcell.lyraapp.data.auth

/**
 * Kimlik doğrulama ve oturum yönetimi işlemlerinin tek soyutlama noktası.
 */
interface AuthRepository {

    /**
     * Verilen telefon numarası ve şifreyle giriş dener.
     */
    suspend fun login(phoneNumber: String, password: String): Result<Unit>

    /**
     * Verilen kullanıcı bilgileriyle yeni bir hesap oluşturmayı dener.

     */
    suspend fun register(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        password: String,
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
