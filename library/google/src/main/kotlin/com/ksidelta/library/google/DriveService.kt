package com.ksidelta.library.google

class DriveService(val driveClient: DriveClient) {
    fun listFiles(token: String): List<DriveFile> =
        driveClient.listFiles(token)
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
