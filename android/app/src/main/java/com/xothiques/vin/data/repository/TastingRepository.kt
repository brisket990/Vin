package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.TastingApi
import com.xothiques.vin.data.remote.dto.TastingNoteListItemDto
import com.xothiques.vin.data.remote.dto.UpdateTastingNoteRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TastingRepository @Inject constructor(
    private val tastingApi: TastingApi,
) {
    suspend fun findAll(): List<TastingNoteListItemDto> = tastingApi.findAll()

    suspend fun update(id: String, rating: Int?, comment: String?): TastingNoteListItemDto =
        tastingApi.update(id, UpdateTastingNoteRequest(rating, comment))

    suspend fun remove(id: String) = tastingApi.remove(id)
}
