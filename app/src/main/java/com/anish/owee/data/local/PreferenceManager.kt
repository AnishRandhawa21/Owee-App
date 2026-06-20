package com.anish.owee.data.local

import android.content.Context
import android.content.SharedPreferences
import com.anish.owee.data.model.Friendship
import com.anish.owee.viewmodel.state.GroupWithMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("owee_prefs", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun saveHomeBalance(total: Double, groups: Double, friends: Double) {
        prefs.edit().apply {
            putFloat("total_balance", total.toFloat())
            putFloat("groups_balance", groups.toFloat())
            putFloat("friends_balance", friends.toFloat())
            apply()
        }
    }

    fun getHomeBalance(): Triple<Double, Double, Double> {
        val total = prefs.getFloat("total_balance", 0.0f).toDouble()
        val groups = prefs.getFloat("groups_balance", 0.0f).toDouble()
        val friends = prefs.getFloat("friends_balance", 0.0f).toDouble()
        return Triple(total, groups, friends)
    }

    fun saveFriends(friends: List<Friendship>) {
        prefs.edit().apply {
            putString("cached_friends", json.encodeToString(friends))
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
}