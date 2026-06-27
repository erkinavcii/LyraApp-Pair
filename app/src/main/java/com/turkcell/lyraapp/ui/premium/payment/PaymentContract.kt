package com.turkcell.lyraapp.ui.premium.payment

data class PaymentUiState(
    val planId: String = "",
    val planDisplayName: String = "",
    val planPrice: String = "",
    val cardNumber: String = "",
    val cardHolderName: String = "",
    val expiryDate: String = "",
    val cvc: String = "",
    val isLoading: Boolean = false,
    val isSubmitEnabled: Boolean = false,
)

sealed interface PaymentIntent {
    data class CardNumberChanged(val value: String) : PaymentIntent
    data class CardHolderNameChanged(val value: String) : PaymentIntent
    data class ExpiryDateChanged(val value: String) : PaymentIntent
    data class CvcChanged(val value: String) : PaymentIntent
    data object Submit : PaymentIntent
    data object NavigateBack : PaymentIntent
}

sealed interface PaymentEffect {
    data object NavigateBack : PaymentEffect
    data class ShowError(val message: String) : PaymentEffect
    data object PaymentSuccess : PaymentEffect
}
