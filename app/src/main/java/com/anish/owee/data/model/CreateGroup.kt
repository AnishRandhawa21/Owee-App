package com.anish.owee.data.model

data class CreateGroup(
    val name: String,
    val memberIds: List<String>
)