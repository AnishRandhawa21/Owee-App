package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("username")
    val username: String,

    @SerialName("photo_url")
    val photoUrl: String? = null,

    @SerialName("upi_id")
    val upiId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
