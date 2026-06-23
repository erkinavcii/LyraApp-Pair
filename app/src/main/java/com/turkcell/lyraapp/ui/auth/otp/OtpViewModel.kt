package com.turkcell.lyraapp.ui.auth.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val phoneNumber: String = checkNotNull(savedStateHandle["phoneNumber"])

    private val _uiState = MutableStateFlow(OtpUiState(phoneNumber = phoneNumber))
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OtpEffect>(Channel.BUFFERED)
    val effect: Flow<OtpEffect> = _effect.receiveAsFlow()

    init {
        startResendCountdown()
    }

    fun onIntent(intent: OtpIntent) {
        when (intent) {
            is OtpIntent.CodeChanged -> _uiState.update {
                val trimmed = intent.code.filter { c -> c.isDigit() }.take(6)
                it.copy(code = trimmed, isVerifyEnabled = trimmed.length == 6)
            }
            is OtpIntent.Submit -> verify()
            is OtpIntent.ResendOtp -> resend()
        }
    }

    private fun verify() {
        val state = _uiState.value
        if (!state.isVerifyEnabled || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.verifyOtp("+90${state.phoneNumber}", state.code)
            _uiState.update { it.copy(isLoading = false) }
            result
                .onSuccess { firstTime ->
                    if (firstTime) {
                        _effect.send(OtpEffect.NavigateToCompleteProfile)
                    } else {
                        _effect.send(OtpEffect.NavigateToHome)
                    }
                }
                .onFailure { _effect.send(OtpEffect.ShowError(it.message ?: "Kod hatalı.")) }
        }
    }

    private fun resend() {
        if (_uiState.value.resendCooldownSeconds > 0) return
        viewModelScope.launch {
            val result = authRepository.requestOtp("+90${_uiState.value.phoneNumber}")
            result
                .onSuccess { startResendCountdown() }
                .onFailure { _effect.send(OtpEffect.ShowError(it.message ?: "Kod gönderilemedi.")) }
        }
    }

    private fun startResendCountdown() {
        viewModelScope.launch {
            _uiState.update { it.copy(resendCooldownSeconds = 60) }
            repeat(60) {
                delay(1000L)
                _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }
}
