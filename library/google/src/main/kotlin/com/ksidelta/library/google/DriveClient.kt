package com.ksidelta.library.google

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.utils.UrlBuilder

class DriveClient(val httpClient: HttpClient) {
    fun listFiles(token: String, criteria: Criteria): List<DriveFileListDTO> =
        generateSequence<DriveFileListDTO>(
            queryFiles(token, criteria, null),
        ) { files ->
            files.nextPageToken?.let { queryFiles(token, criteria, it) }
        }.toList()

    private fun queryFiles(token: String, criteria: Criteria, pageToken: String?) =
        httpClient.get(
            UrlBuilder.queryUrl(
                "https://www.googleapis.com/drive/v3/files", mapOf(
                    "corpora" to "allDrives",
                    "includeItemsFromAllDrives" to "true",
                    "supportsAllDrives" to "true",
                    "access_token" to token,
                )
                        + (criteria.contains?.let { mapOf("q" to """name contains "$it" """) } ?: mapOf())
                        + (pageToken?.let { mapOf("pageToken" to it) } ?: mapOf())
            ), DriveFileListDTO::class.java
        )

    fun downloadFile(token: String, fileId: String): ByteArray = httpClient.get(
        UrlBuilder.queryUrl(
            "https://www.googleapis.com/drive/v3/files/${fileId}", mapOf(
                "alt" to "media",
                "supportsAllDrives" to "true",
            )
        ), ByteArray::class.java
    ) { headers -> headers.setToken(token) }

    data class DriveFileListDTO(
        val nextPageToken: String?,
        val kind: String,
        val incompleteSearch: Boolean,
        val files: List<DriveFileDTO>
    )

    data class DriveFileDTO(
        val kind: String,
        val mimeType: String,
        val name: String,
        val id: String,
        val driveId: String?,
        val teamDriveId: String?,
    )

    data class Criteria(
        val contains: String?
    )
}

