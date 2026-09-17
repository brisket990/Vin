package com.xothiques.vin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImportRowErrorDto(
    val row: Int,
    val message: String,
)

/** Response of POST import/cellar/csv -- a summary rather than a hard
 *  failure, since a handful of bad rows shouldn't block the rest of the
 *  file. See ImportService on the backend for the exact import rules
 *  (bottles land without a cellar location; "consumed" rows are skipped). */
@Serializable
data class ImportCsvResultDto(
    val imported: Int,
    val skippedConsumed: Int,
    val errors: List<ImportRowErrorDto> = emptyList(),
)
