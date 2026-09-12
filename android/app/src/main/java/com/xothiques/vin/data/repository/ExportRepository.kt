package com.xothiques.vin.data.repository

import com.xothiques.vin.data.remote.ExportApi
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val exportApi: ExportApi,
) {
    /** Downloads the export and writes it to [destination], returning that file. */
    suspend fun downloadCsv(destination: File): File {
        exportApi.exportCsv().byteStream().use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        return destination
    }

    suspend fun downloadPdf(destination: File): File {
        exportApi.exportPdf().byteStream().use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        return destination
    }
}
