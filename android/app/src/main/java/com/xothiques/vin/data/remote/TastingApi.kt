package com.xothiques.vin.data.remote

import com.xothiques.vin.data.remote.dto.TastingNoteListItemDto
import com.xothiques.vin.data.remote.dto.UpdateTastingNoteRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface TastingApi {
    @GET("api/vin/tasting-notes")
    suspend fun findAll(): List<TastingNoteListItemDto>

    @PATCH("api/vin/tasting-notes/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateTastingNoteRequest,
    ): TastingNoteListItemDto

    @DELETE("api/vin/tasting-notes/{id}")
    suspend fun remove(@Path("id") id: String)
}
