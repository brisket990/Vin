package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.ScanApi
import com.xothiques.vin.data.remote.dto.LinkScanBottleRequest
import com.xothiques.vin.data.remote.dto.ScanResultDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val scanApi: ScanApi,
) {
    suspend fun scan(photoFile: File, provider: String? = null): ScanResultDto {
        val mimeType = if (photoFile.extension.equals("png", ignoreCase = true)) {
            "image/png"
        } else {
            "image/jpeg"
        }
        val photoBody = photoFile.asRequestBody(mimeType.toMediaType())
        val photoPart = MultipartBody.Part.createFormData("photo", photoFile.name, photoBody)
        val providerPart = provider?.toRequestBody("text/plain".toMediaType())
        return scanApi.scan(photoPart, providerPart)
    }

    suspend fun findAll(): List<ScanResultDto> = scanApi.findAll()

    suspend fun findOne(id: String): ScanResultDto = scanApi.findOne(id)

    suspend fun linkBottle(id: String, bottleId: String): ScanResultDto =
        scanApi.linkBottle(id, LinkScanBottleRequest(bottleId))
}
