package com.anish.owee.domain

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.GroupMemberBalance

object GroupBalanceCalculator {

    fun calculateBalances(
        currentUserId: String,
        expenses: List<Expense>,
        participantsByExpense: Map<String, List<ExpenseParticipant>>
    ): List<GroupMemberBalance> {

        val memberBalances =
            mutableMapOf<String, Double>()

        expenses.forEach { expense ->

            val participants =
                participantsByExpense[expense.id]
                    ?: emptyList()

            if (expense.payerId == currentUserId) {

                participants.forEach { participant ->

                    if (participant.userId != currentUserId) {

                        memberBalances[participant.userId] =
                            memberBalances.getOrDefault(
                                participant.userId,
                                0.0
                            ) + participant.shareAmount
                    }
                }
            } else {

                val currentUserShare =
                    participants.firstOrNull {
                        it.userId == currentUserId
                    }

                if (currentUserShare != null) {

                    memberBalances[expense.payerId] =
                        memberBalances.getOrDefault(
                            expense.payerId,
                            0.0
                        ) - currentUserShare.shareAmount
                }
            }
        }

        return memberBalances.map { (userId, amount) ->

            GroupMemberBalance(
                userId = userId,
                amount = amount
            )
        }
    }
}