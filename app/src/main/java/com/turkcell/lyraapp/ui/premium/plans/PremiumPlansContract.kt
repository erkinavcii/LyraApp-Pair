package com.turkcell.lyraapp.ui.premium.plans

import com.turkcell.lyraapp.data.network.MembershipPlanDto

data class PremiumPlansUiState(
    val isLoading: Boolean = false,
    val plans: List<MembershipPlanDto> = emptyList(),
    val selectedPlanId: String? = null,
    val errorMessage: String? = null,
)

sealed interface PremiumPlansIntent {
    data class SelectPlan(val planId: String) : PremiumPlansIntent
    data object Proceed : PremiumPlansIntent
    data object NavigateBack : PremiumPlansIntent
}

sealed interface PremiumPlansEffect {
    data class NavigateToPayment(val planId: String) : PremiumPlansEffect
    data object NavigateBack : PremiumPlansEffect
}
