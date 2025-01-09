package com.ksidelta.library.serialization

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator

class JacksonSerializerTest : Serializer {
    val jsonFactory = JsonFactory
        .builder()
        .build()

    val objectMapper = ObjectMapper(jsonFactory).apply {
        ObjectMapper.setPolymorphicTypeValidator = LaissezFaireSubTypeValidator()
    }

    override fun encode(obj: Any): String =
        objectMapper.writeValueAsString(obj)
    
    override fun <T> decode(obj: String, klass: Class<T>): T =
        objectMapper.readValue<T>(obj, klass)
}