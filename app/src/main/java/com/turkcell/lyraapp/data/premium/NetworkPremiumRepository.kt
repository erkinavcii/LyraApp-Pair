package com.turkcell.lyraapp.data.premium

import com.turkcell.lyraapp.data.network.CardDetailsDto
import com.turkcell.lyraapp.data.network.CheckoutRequest
import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.data.network.MembershipDto
import com.turkcell.lyraapp.data.network.MembershipPlanDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPremiumRepository @Inject constructor(
    private val apiService: LyraApiService,
) : PremiumRepository {

    override suspend fun getPlans(): Result<List<MembershipPlanDto>> {
        return try {
            val response = apiService.getPlans()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkout(
        planId: String,
        cardNumber: String,
        cardHolderName: String,
        expMonth: Int,
        expYear: Int,
        cvc: String,
    ): Result<MembershipDto> {
        return try {
            val response = apiService.checkout(
                CheckoutRequest(
                    plan = planId,
                    card = CardDetailsDto(
                        number = cardNumber.replace(" ", ""),
                        expMonth = expMonth,
                        expYear = expYear,
                        cvc = cvc,
                        holderName = cardHolderName,
                    ),
                )
            )
            Result.success(response.data.membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
