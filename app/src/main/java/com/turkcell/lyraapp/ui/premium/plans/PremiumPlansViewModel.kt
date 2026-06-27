package com.turkcell.lyraapp.ui.premium.plans

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
class PremiumPlansViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumPlansUiState())
    val uiState: StateFlow<PremiumPlansUiState> = _uiState.asStateFlow()

    private val _effect = Channel<PremiumPlansEffect>(Channel.BUFFERED)
    val effect: Flow<PremiumPlansEffect> = _effect.receiveAsFlow()

    init {
        loadPlans()
    }

    fun onIntent(intent: PremiumPlansIntent) {
        when (intent) {
            is PremiumPlansIntent.SelectPlan -> _uiState.update { it.copy(selectedPlanId = intent.planId) }
            is PremiumPlansIntent.Proceed -> proceed()
            is PremiumPlansIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(PremiumPlansEffect.NavigateBack)
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            premiumRepository.getPlans()
                .onSuccess { plans ->
                    val defaultSelected = plans.find { it.type == "recurring" }?.id ?: plans.firstOrNull()?.id
                    _uiState.update {
                        it.copy(isLoading = false, plans = plans, selectedPlanId = defaultSelected)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Planlar yuklenemedi.")
                    }
                }
        }
    }

    private fun proceed() {
        val state = _uiState.value
        val plan = state.plans.find { it.id == state.selectedPlanId } ?: return
        viewModelScope.launch {
            _effect.send(PremiumPlansEffect.NavigateToPayment(plan.type))
        }
    }
}
