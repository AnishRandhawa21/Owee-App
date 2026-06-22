package com.anish.owee.domain

import com.anish.owee.data.model.FriendRequest
import com.anish.owee.data.model.Settlement

object FriendBalanceCalculator {

    fun calculate(
        currentUserId: String,
        requests: List<FriendRequest>,
        settlements: List<Settlement>
    ): Double {
        var totalRequestedByMe = 0.0
        var totalRequestedByFriend = 0.0
        
        requests.forEach {
            if (it.creatorId == currentUserId) {
                totalRequestedByMe += it.amount
            } else {
                totalRequestedByFriend += it.amount
            }
        }

        var totalPaidByMe = 0.0
        var totalReceivedByMe = 0.0
        
        settlements.forEach {
            if (it.payerId == currentUserId) {
                totalPaidByMe += it.amount
            } else {
                totalReceivedByMe += it.amount
            }
        }

        val net = (totalRequestedByMe - totalRequestedByFriend) + (totalPaidByMe - totalReceivedByMe)
        return kotlin.math.round(net * 100.0) / 100.0
    }
}
