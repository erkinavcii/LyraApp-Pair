package com.turkcell.lyraapp.ui.premium.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.premium.PremiumRepository
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

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val planId: String = checkNotNull(savedStateHandle["planId"])

    private val _uiState = MutableStateFlow(PaymentUiState(planId = planId))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _effect = Channel<PaymentEffect>(Channel.BUFFERED)
    val effect: Flow<PaymentEffect> = _effect.receiveAsFlow()

    init {
        loadPlanDetails()
    }

    private fun loadPlanDetails() {
        viewModelScope.launch {
            premiumRepository.getPlans().onSuccess { plans ->
                val plan = plans.find { it.type == planId } ?: return@onSuccess
                val displayName = if (plan.type == "recurring") "Aylık abonelik" else "30 gun erisim"
                val price = if (plan.type == "recurring") "₺${plan.price.toInt()} / ay" else "₺${plan.price.toInt()}"
                _uiState.update { it.copy(planDisplayName = displayName, planPrice = price) }
            }
        }
    }

    fun onIntent(intent: PaymentIntent) {
        when (intent) {
            is PaymentIntent.CardNumberChanged -> updateForm { it.copy(cardNumber = formatCardNumber(intent.value)) }
            is PaymentIntent.CardHolderNameChanged -> updateForm { it.copy(cardHolderName = intent.value) }
            is PaymentIntent.ExpiryDateChanged -> updateForm { it.copy(expiryDate = formatExpiry(intent.value)) }
            is PaymentIntent.CvcChanged -> updateForm { it.copy(cvc = intent.value.take(3)) }
            is PaymentIntent.Submit -> submit()
            is PaymentIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(PaymentEffect.NavigateBack)
            }
        }
    }

    private fun updateForm(transform: (PaymentUiState) -> PaymentUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(isSubmitEnabled = updated.isFormValid())
        }
    }

    private fun PaymentUiState.isFormValid(): Boolean {
        val rawNumber = cardNumber.replace(" ", "")
        val expiryParts = expiryDate.split("/")
        return rawNumber.length == 16 &&
                cardHolderName.isNotBlank() &&
                expiryParts.size == 2 &&
                expiryParts[0].length == 2 &&
                expiryParts[1].length == 2 &&
                cvc.length == 3
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val expiryParts = state.expiryDate.split("/")
            val expMonth = expiryParts[0].toIntOrNull() ?: 0
            val expYear = ("20" + expiryParts[1]).toIntOrNull() ?: 0
            premiumRepository.checkout(
                planId = state.planId,
                cardNumber = state.cardNumber,
                cardHolderName = state.cardHolderName,
                expMonth = expMonth,
                expYear = expYear,
                cvc = state.cvc,
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(PaymentEffect.PaymentSuccess)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                val msg = when {
                    error.message?.contains("402") == true -> "Odeme reddedildi. Kart bilgilerinizi kontrol edin."
                    else -> error.message ?: "Odeme basarisiz oldu."
                }
                _effect.send(PaymentEffect.ShowError(msg))
            }
        }
    }

    private fun formatCardNumber(raw: String): String {
        val digits = raw.replace(" ", "").take(16)
        return digits.chunked(4).joinToString(" ")
    }

    private fun formatExpiry(raw: String): String {
        val digits = raw.replace("/", "").take(4)
        return if (digits.length >= 2) "${digits.take(2)}/${digits.drop(2)}" else digits
    }
}
