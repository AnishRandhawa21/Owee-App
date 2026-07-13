package com.anish.owee.domain

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.data.model.SettlementAllocation

object GroupBalanceCalculator {

    fun calculateBalances(
        currentUserId: String,
        expenses: List<Expense>,
        participantsByExpense: Map<String, List<ExpenseParticipant>>,
        allocations: List<SettlementAllocation>
    ): List<GroupMemberBalance> {

        val memberBalances = mutableMapOf<String, Double>()

        // 1. Process Expenses
        expenses.forEach { expense ->
            val participants = participantsByExpense[expense.id] ?: emptyList()

            if (expense.payerId == currentUserId) {
                participants.forEach { participant ->
                    if (participant.userId != currentUserId) {
                        memberBalances[participant.userId] =
                            memberBalances.getOrDefault(participant.userId, 0.0) + participant.shareAmount
                    }
                }
            } else {
                val currentUserShare = participants.firstOrNull { it.userId == currentUserId }
                if (currentUserShare != null) {
                    memberBalances[expense.payerId] =
                        memberBalances.getOrDefault(expense.payerId, 0.0) - currentUserShare.shareAmount
                }
            }
        }

        // 2. Process Allocations
        allocations.forEach { allocation ->
            val otherUserId = if (allocation.payerId == currentUserId) allocation.receiverId else allocation.payerId
            if (otherUserId != null) {
                if (allocation.receiverId == currentUserId) {
                    memberBalances[otherUserId] = memberBalances.getOrDefault(otherUserId, 0.0) - allocation.amount
                } else if (allocation.payerId == currentUserId) {
                    memberBalances[otherUserId] = memberBalances.getOrDefault(otherUserId, 0.0) + allocation.amount
                }
            }
        }

        return memberBalances.filterValues { kotlin.math.abs(it) > 0.01 }
            .map { (userId, amount) ->
                GroupMemberBalance(userId, kotlin.math.round(amount * 100.0) / 100.0)
            }
    }
}
