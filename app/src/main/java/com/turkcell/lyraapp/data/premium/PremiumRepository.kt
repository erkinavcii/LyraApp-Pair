package com.turkcell.lyraapp.data.premium

import com.turkcell.lyraapp.data.network.MembershipPlanDto
import com.turkcell.lyraapp.data.network.MembershipDto

interface PremiumRepository {

    suspend fun getPlans(): Result<List<MembershipPlanDto>>

    suspend fun checkout(
        planId: String,
        cardNumber: String,
        cardHolderName: String,
        expMonth: Int,
        expYear: Int,
        cvc: String,
    ): Result<MembershipDto>
}
