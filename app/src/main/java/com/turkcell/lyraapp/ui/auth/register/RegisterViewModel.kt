package com.turkcell.lyraapp.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Register ekranının MVI ViewModel'i.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect: Flow<RegisterEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.FirstNameChanged -> updateForm { it.copy(firstName = intent.value) }
            is RegisterIntent.LastNameChanged -> updateForm { it.copy(lastName = intent.value) }
            is RegisterIntent.PhoneNumberChanged -> updateForm {
                val filtered = intent.value.filter { c -> c.isDigit() }.take(10)
                it.copy(phoneNumber = filtered)
            }
            is RegisterIntent.PasswordChanged -> updateForm { it.copy(password = intent.value) }
            is RegisterIntent.ConfirmPasswordChanged -> updateForm { it.copy(confirmPassword = intent.value) }
            is RegisterIntent.TermsAcceptedChanged -> updateForm { it.copy(isTermsAccepted = intent.value) }
            is RegisterIntent.TogglePasswordVisibility -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is RegisterIntent.Submit -> submit()
            is RegisterIntent.LoginClicked -> viewModelScope.launch { _effect.send(RegisterEffect.NavigateToLogin) }
        }
    }

    private fun updateForm(transform: (RegisterUiState) -> RegisterUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(
                isRegisterEnabled = updated.isFormValid(),
                passwordStrength = updated.password.calculateStrength()
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isRegisterEnabled || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.updateProfile(
                firstName = state.firstName,
                lastName = state.lastName,
                birthDate = "2000-01-01"
            )
            _uiState.update { it.copy(isLoading = false) }

            result
                .onSuccess { _effect.send(RegisterEffect.NavigateToLogin) }
                .onFailure { error ->
                    _effect.send(RegisterEffect.ShowError(error.message ?: "Kayıt başarısız."))
                }
        }
    }
//
    private fun String.calculateStrength(): Int {
        if (isEmpty()) return 0
        var score = 0
        if (length >= 8) score++
        if (any { it.isDigit() }) score++
        if (any { !it.isLetterOrDigit() }) score++
        return score.coerceAtMost(3)
    }
}

private val phoneRegex = Regex("^[0-9]{10}$")

private fun RegisterUiState.isFormValid(): Boolean =
    firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            phoneRegex.matches(phoneNumber) &&
            password.length >= 6 &&
            password == confirmPassword &&
            isTermsAccepted
