package com.anish.owee.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OweeNotification(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val type: String,
    val title: String,
    val body: String,
    val data: JsonElement? = null,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at")
    val createdAt: String? = null
)
