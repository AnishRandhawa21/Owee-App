package com.anish.owee.domain

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.data.model.Settlement

object GroupBalanceCalculator {

    fun calculateBalances(
        currentUserId: String,
        expenses: List<Expense>,
        participantsByExpense: Map<String, List<ExpenseParticipant>>,
        settlements: List<Settlement>
    ): List<GroupMemberBalance> {

        val memberBalances =
            mutableMapOf<String, Double>()

        // EXPENSES

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

        // SETTLEMENTS

        settlements.forEach { settlement ->

            when {

                settlement.receiverId == currentUserId -> {

                    memberBalances[settlement.payerId] =
                        memberBalances.getOrDefault(
                            settlement.payerId,
                            0.0
                        ) - settlement.amount
                }

                settlement.payerId == currentUserId -> {

                    memberBalances[settlement.receiverId] =
                        memberBalances.getOrDefault(
                            settlement.receiverId,
                            0.0
                        ) + settlement.amount
                }
            }
        }

        return memberBalances
            .filterValues { kotlin.math.abs(it) > 0.01 }
            .map { (userId, amount) ->

                GroupMemberBalance(
                    userId = userId,
                    amount = kotlin.math.round(amount * 100.0) / 100.0
                )
            }
    }
}