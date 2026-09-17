package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.ImportApi
import com.xothiques.vin.data.remote.dto.ImportCsvResultDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    private val importApi: ImportApi,
) {
    suspend fun importCsv(file: File): ImportCsvResultDto {
        val body = file.asRequestBody("text/csv".toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        return importApi.importCsv(part)
    }
}
