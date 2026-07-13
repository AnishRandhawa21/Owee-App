package com.anish.owee.domain

import com.anish.owee.data.model.CreateSettlementSessionRequest
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

data class SettlementSource(
    val sourceType: String,
    val sourceId: String?,
    val amount: Double,
    val createdAt: String
)

object SettlementPlanner {

    /**
     * Plans a settlement session and its corresponding directional allocations.
     * 
     * @param currentUserId The ID of the user performing the settlement.
     * @param targetUserId The ID of the other user in the relationship.
     * @param cashAmount The actual cash/UPI amount being moved.
     * @param sources All relevant balance sources between the two users.
     *                Positive amount means targetUserId owes currentUserId.
     *                Negative amount means currentUserId owes targetUserId.
     * @param sessionType The context of the settlement ('HOME', 'GROUP', or 'FRIEND').
     */
    fun plan(
        currentUserId: String,
        targetUserId: String,
        cashAmount: Double,
        sources: List<SettlementSource>,
        sessionType: String
    ): SettlementPlan {
        
        // 1. Determine Direction of the real cash movement based on totalNet
        val totalNet = sources.sumOf { it.amount }
        val isMePayingCash = totalNet < -0.01
        
        val realPayerId = if (isMePayingCash) currentUserId else targetUserId
        val realReceiverId = if (isMePayingCash) targetUserId else currentUserId

        val allocations = mutableListOf<CreateSettlementAllocationPlan>()
        
        // 2. Step A: Internal Netting (Set-off)
        // Settle all debts that are in the OPPOSITE direction of the cash movement.
        // This clears 'stuck' balances using the credit from other sources.
        val oppositeSources = sources.filter { 
            if (isMePayingCash) it.amount > 0.01 else it.amount < -0.01 
        }
        
        var totalNettingValue = 0.0
        oppositeSources.forEach { source ->
            val sourceAmountAbs = abs(source.amount)
            val sourcePayerId = if (source.amount > 0) targetUserId else currentUserId
            val sourceReceiverId = if (source.amount > 0) currentUserId else targetUserId
            
            allocations.add(
                CreateSettlementAllocationPlan(
                    sourceType = source.sourceType,
                    sourceId = source.sourceId ?: "",
                    payerId = sourcePayerId,
                    receiverId = sourceReceiverId,
                    amount = sourceAmountAbs
                )
            )
            totalNettingValue += sourceAmountAbs
        }

        // 3. Step B: Distribution Pool (Cash + Netting Value)
        // Total money available to clear debts = Cash + Netting Value
        val sameSources = sources.filter { 
            if (isMePayingCash) it.amount < -0.01 else it.amount > 0.01 
        }.sortedBy { it.createdAt }

        var remainingPool = round((cashAmount + totalNettingValue) * 100.0) / 100.0
        
        for (source in sameSources) {
            if (remainingPool <= 0.001) break
            
            val debtAbs = abs(source.amount)
            val toSettle = min(remainingPool, debtAbs)
            val roundedToSettle = round(toSettle * 100.0) / 100.0
            
            if (roundedToSettle > 0.001) {
                allocations.add(
                    CreateSettlementAllocationPlan(
                        sourceType = source.sourceType,
                        sourceId = source.sourceId ?: "",
                        payerId = realPayerId,
                        receiverId = realReceiverId,
                        amount = roundedToSettle
                    )
                )
                remainingPool -= roundedToSettle
            }
        }
        
        // 4. Step C: Overpayment (Remainder)
        if (remainingPool > 0.001) {
            val primarySource = sources.find { it.sourceType == "FRIEND" } ?: sources.firstOrNull()
            allocations.add(
                CreateSettlementAllocationPlan(
                    sourceType = primarySource?.sourceType ?: "FRIEND",
                    sourceId = primarySource?.sourceId ?: targetUserId,
                    payerId = realPayerId,
                    receiverId = realReceiverId,
                    amount = round(remainingPool * 100.0) / 100.0
                )
            )
        }

        return SettlementPlan(
            session = CreateSettlementSessionRequest(
                payerId = realPayerId,
                receiverId = realReceiverId,
                totalAmount = cashAmount,
                type = sessionType
            ),
            allocations = allocations
        )
    }
}
