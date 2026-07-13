package com.anish.owee.domain

import com.anish.owee.data.model.FriendRequest
import com.anish.owee.data.model.SettlementAllocation

object FriendBalanceCalculator {

    fun calculate(
        currentUserId: String,
        requests: List<FriendRequest>,
        allocations: List<SettlementAllocation>
    ): Double {
        var netFromRequests = 0.0
        
        requests.forEach {
            if (it.creatorId == currentUserId) {
                netFromRequests += it.amount
            } else {
                netFromRequests -= it.amount
            }
        }

        // Apply Allocations
        var totalAllocated = 0.0
        allocations.forEach { allocation ->
            if (allocation.receiverId == currentUserId) {
                // I received money, my credit decreases or my debt increases
                totalAllocated += allocation.amount
            } else if (allocation.payerId == currentUserId) {
                // I paid money, my debt decreases or my credit increases
                totalAllocated -= allocation.amount
            }
        }

        val net = netFromRequests - totalAllocated
        return kotlin.math.round(net * 100.0) / 100.0
    }
}
