package com.ksidelta.library.store

import com.ksidelta.library.serialization.JacksonSerializer
import com.ksidelta.library.serialization.Serializer
import java.io.File
import java.nio.file.Path


class FileStore(val folderPath: String, val serializer: Serializer = JacksonSerializer()) : Store {
    init {
        Path.of(folderPath).toFile().mkdirs()
    }

    override fun store(id: String, obj: Any) =
        Path.of(folderPath, id).toFile()
            .run { writeText(serializer.encode(obj)) }

    override fun <T> get(id: String, klass: Class<T>): T? =
        Path.of(folderPath, id).toFile()
            .let { if (it.exists()) it else null }
            ?.readText()
            ?.let { serializer.decode(it, klass) }

    override fun keys(): List<String> =
        File(folderPath)
            .listFiles()!!
            .map { it.name }
            .sorted()
}