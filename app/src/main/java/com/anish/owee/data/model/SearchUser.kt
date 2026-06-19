package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchUser(

    val id: String,

    val username: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("photo_url")
    val photoUrl: String? = null
)