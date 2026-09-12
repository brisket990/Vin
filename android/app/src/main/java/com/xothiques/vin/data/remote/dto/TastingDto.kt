package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TastingNoteListItemDto(
    val id: String,
    val bottleId: String,
    val bottleName: String,
    val bottleVintage: Int? = null,
    val userId: String,
    val rating: Int? = null,
    val comment: String? = null,
    val consumedDate: String,
    val createdAt: String,
)

@Serializable
data class UpdateTastingNoteRequest(
    val rating: Int? = null,
    val comment: String? = null,
)
