package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.repository.*
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.viewmodel.state.DebtSource
import com.anish.owee.viewmodel.state.HomeUiState
import com.anish.owee.viewmodel.state.UserTotalBalance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository = GroupRepositoryImpl()
    private val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl()
    private val settlementRepository: SettlementRepository = SettlementRepositoryImpl()
    private val friendshipRepository: FriendshipRepository = FriendshipRepositoryImpl()
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()
    private val authRepository: AuthRepository = AuthRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCachedData()
        // If we have cached data, the first load should be silent to avoid skeleton flicker
        val hasData = _uiState.value.userBalances.isNotEmpty() || 
                     _uiState.value.totalBalance != 0.0 ||
                     _uiState.value.groupBalance != 0.0 ||
                     _uiState.value.friendBalance != 0.0
        
        loadHomeData(isSilent = hasData)
        observeChanges()
    }

    private fun loadCachedData() {
        val cache = preferenceManager.getHomeBalance()
        _uiState.value = _uiState.value.copy(
            totalBalance = cache.total,
            groupBalance = cache.groups,
            friendBalance = cache.friends,
            userBalances = cache.userBalances
        )
    }

    private fun observeChanges() {
        viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            merge(
                groupRepository.groupChanges(),
                friendshipRepository.friendshipChanges(),
                friendRequestRepository.requestChanges(),
                settlementRepository.settlementChanges(),
                expenseRepository.expenseChanges()
            ).collect {
                android.util.Log.d("OWEE_REALTIME", "Home data change detected")
                loadHomeData(isSilent = true)
            }
        }
    }

    fun loadHomeData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            try {
                coroutineScope {
                    val currentUserId = groupRepository.getCurrentUserId()
                    if (currentUserId == null) {
                        if (!isSilent) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Session expired. Please login again."
                            )
                        }
                        return@coroutineScope
                    }

                    val userMap = mutableMapOf<String, com.anish.owee.data.model.User>()
                    val balanceMap = mutableMapOf<String, Double>()
                    val sourcesMap = mutableMapOf<String, MutableList<DebtSource>>()

                    // 1. Parallelize Group Fetching
                    val groups = groupRepository.getGroupsWithMetadata()
                    val groupResultsDeferred = groups.map { groupMetadata ->
                        async {
                            val groupId = groupMetadata.group.id
                            val expenses = expenseRepository.getGroupExpenses(groupId)
                            val settlements = settlementRepository.getSettlements("GROUP", groupId)
                            val allParticipants = expenseRepository.getGroupExpenseParticipants(groupId)
                            val participantsByExpense = allParticipants.groupBy { it.expenseId }

                            val groupBalances = GroupBalanceCalculator.calculateBalances(
                                currentUserId = currentUserId,
                                expenses = expenses,
                                participantsByExpense = participantsByExpense,
                                settlements = settlements
                            )
                            
                            groupBalances to groupMetadata
                        }
                    }

                    // 3. Parallelize Friend Fetching
                    val friendships = friendshipRepository.getFriendships()
                    val friendResultsDeferred = friendships.map { friendship ->
                        async {
                            val friendId = if (friendship.senderId == currentUserId) friendship.receiverId else friendship.senderId
                            val friendUser = if (friendship.senderId == currentUserId) friendship.receiver else friendship.sender
                            
                            val requests = friendRequestRepository.getRequestsForFriend(friendId)
                            val friendSettlements = settlementRepository.getSettlements("FRIEND", friendship.id)

                            val friendNet = FriendBalanceCalculator.calculate(
                                currentUserId = currentUserId,
                                requests = requests,
                                settlements = friendSettlements
                            )
                            
                            Triple(friendId, friendUser, friendNet) to friendship
                        }
                    }

                    // Collect results and identify missing users
                    val missingUserIds = mutableSetOf<String>()

                    // Collect Group Results
                    var totalGroupBalance = 0.0
                    groupResultsDeferred.awaitAll().forEach { (groupBalances, groupMetadata) ->
                        totalGroupBalance += groupBalances.sumOf { it.amount }
                        groupBalances.forEach { gb ->
                            balanceMap[gb.userId] = balanceMap.getOrDefault(gb.userId, 0.0) + gb.amount
                            val userInGroup = groupMetadata.members.find { it.id == gb.userId }
                            if (userInGroup != null) {
                                userMap[userInGroup.id] = userInGroup
                            } else {
                                missingUserIds.add(gb.userId)
                            }
                            
                            val source = DebtSource(
                                sourceType = "GROUP",
                                sourceId = groupMetadata.group.id,
                                amount = gb.amount,
                                createdAt = groupMetadata.group.createdAt
                            )
                            sourcesMap.getOrPut(gb.userId) { mutableListOf() }.add(source)
                        }
                    }

                    // Collect Friend Results
                    var totalFriendBalance = 0.0
                    friendResultsDeferred.awaitAll().forEach { (data, friendship) ->
                        val (friendId, friendUser, friendNet) = data
                        totalFriendBalance += friendNet

                        if (kotlin.math.abs(friendNet) > 0.01) {
                            balanceMap[friendId] = balanceMap.getOrDefault(friendId, 0.0) + friendNet
                            if (friendUser != null) {
                                userMap[friendId] = friendUser
                            } else {
                                missingUserIds.add(friendId)
                            }
                            
                            val source = DebtSource(
                                sourceType = "FRIEND",
                                sourceId = friendship.id,
                                amount = friendNet,
                                createdAt = friendship.createdAt
                            )
                            sourcesMap.getOrPut(friendId) { mutableListOf() }.add(source)
                        }
                    }

                    // Fetch missing users if any
                    if (missingUserIds.isNotEmpty()) {
                        missingUserIds.forEach { userId ->
                            if (!userMap.containsKey(userId)) {
                                try {
                                    val user = authRepository.getUserById(userId)
                                    if (user != null) {
                                        userMap[userId] = user
                                    }
                                } catch (_: Exception) {
                                    // Ignore if user can't be fetched
                                }
                            }
                        }
                    }

                    val finalUserBalances = balanceMap.mapNotNull { (userId, total) ->
                        val user = userMap[userId] ?: return@mapNotNull null
                        if (kotlin.math.abs(total) < 0.01) return@mapNotNull null
                        
                        UserTotalBalance(
                            user = user,
                            balance = total,
                            sources = (sourcesMap[userId] ?: emptyList()).sortedBy { it.createdAt }
                        )
                    }.sortedByDescending { kotlin.math.abs(it.balance) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groupBalance = totalGroupBalance,
                        friendBalance = totalFriendBalance,
                        totalBalance = totalGroupBalance + totalFriendBalance,
                        userBalances = finalUserBalances
                    )

                    // Cache the new data
                    preferenceManager.saveHomeBalance(
                        total = totalGroupBalance + totalFriendBalance,
                        groups = totalGroupBalance,
                        friends = totalFriendBalance,
                        userBalances = finalUserBalances
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}