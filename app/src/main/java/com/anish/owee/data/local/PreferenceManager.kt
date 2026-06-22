package com.anish.owee.data.local

import android.content.Context
import android.content.SharedPreferences
import com.anish.owee.data.model.Friendship
import com.anish.owee.viewmodel.state.FriendRequestUiState
import com.anish.owee.viewmodel.state.GroupDetailUiState
import com.anish.owee.viewmodel.state.GroupWithMetadata
import com.anish.owee.viewmodel.state.UserTotalBalance
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("owee_prefs", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun saveHomeBalance(total: Double, groups: Double, friends: Double, userBalances: List<UserTotalBalance>) {
        prefs.edit().apply {
            putFloat("total_balance", total.toFloat())
            putFloat("groups_balance", groups.toFloat())
            putFloat("friends_balance", friends.toFloat())
            putString("user_balances", json.encodeToString(userBalances))
            apply()
        }
    }

    fun getHomeBalance(): HomeCacheData {
        val total = prefs.getFloat("total_balance", 0.0f).toDouble()
        val groups = prefs.getFloat("groups_balance", 0.0f).toDouble()
        val friends = prefs.getFloat("friends_balance", 0.0f).toDouble()
        val balancesJson = prefs.getString("user_balances", null)
        val userBalances = if (balancesJson != null) {
            try {
                json.decodeFromString<List<UserTotalBalance>>(balancesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
        
        return HomeCacheData(total, groups, friends, userBalances)
    }

    data class HomeCacheData(
        val total: Double,
        val groups: Double,
        val friends: Double,
        val userBalances: List<UserTotalBalance>
    )

    fun saveFriends(friends: List<Friendship>, balances: Map<String, Double> = emptyMap()) {
        prefs.edit().apply {
            putString("cached_friends", json.encodeToString(friends))
            putString("cached_friend_balances", json.encodeToString(balances))
            apply()
        }
    }

    fun getFriends(): List<Friendship> {
        val friendsJson = prefs.getString("cached_friends", null) ?: return emptyList()
        return try {
            json.decodeFromString(friendsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getFriendBalances(): Map<String, Double> {
        val balancesJson = prefs.getString("cached_friend_balances", null) ?: return emptyMap()
        return try {
            json.decodeFromString(balancesJson)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveGroups(groups: List<GroupWithMetadata>) {
        prefs.edit().apply {
            putString("cached_groups", json.encodeToString(groups))
            apply()
        }
    }

    fun getGroups(): List<GroupWithMetadata> {
        val groupsJson = prefs.getString("cached_groups", null) ?: return emptyList()
        return try {
            json.decodeFromString(groupsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGroupDetail(groupId: String, detail: GroupDetailUiState) {
        prefs.edit().apply {
            putString("cached_group_detail_$groupId", json.encodeToString(detail))
            apply()
        }
    }

    fun getGroupDetail(groupId: String): GroupDetailUiState? {
        val detailJson = prefs.getString("cached_group_detail_$groupId", null) ?: return null
        return try {
            json.decodeFromString(detailJson)
        } catch (e: Exception) {
            null
        }
    }

    fun saveFriendDetail(friendId: String, detail: FriendRequestUiState) {
        prefs.edit().apply {
            putString("cached_friend_detail_$friendId", json.encodeToString(detail))
            apply()
        }
    }

    fun getFriendDetail(friendId: String): FriendRequestUiState? {
        val detailJson = prefs.getString("cached_friend_detail_$friendId", null) ?: return null
        return try {
            json.decodeFromString(detailJson)
        } catch (e: Exception) {
            null
        }
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString("fcm_token", token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString("fcm_token", null)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}