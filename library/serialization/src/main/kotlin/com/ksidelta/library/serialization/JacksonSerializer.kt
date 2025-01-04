package com.ksidelta.library.serialization

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class JacksonSerializer : Serializer {
    val jsonFactory = JsonFactory
        .builder()
        .build()

    val objectMapper = ObjectMapper(jsonFactory).apply {
        registerKotlinModule()
        registerModule(Jdk8Module())

        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        activateDefaultTyping(LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS)
    }

    override fun encode(obj: Any): String =
        objectMapper.writeValueAsString(obj)

    override fun <T> decode(obj: String, klass: Class<T>): T =
        objectMapper.readValue<T>(obj, klass)
}