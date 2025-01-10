package com.ksidelta.library.google

import com.ksidelta.library.google.DriveClient.Criteria

class DriveService(val driveClient: DriveClient) {
    fun listFiles(token: String, criteria: (Criteria) -> Criteria = { it }): List<DriveFile> =
        driveClient.listFiles(token, criteria(Criteria(null)))
            .flatMap {
                it.files.map { file -> file.run { DriveFile(name, id, driveId) } }
            }.toList()

    fun download(token: String, fileId: String) = driveClient.downloadFile(token, fileId)


    data class DriveFile(
        val name: String,
        val id: String,
        val driveId: String?
    )
}
