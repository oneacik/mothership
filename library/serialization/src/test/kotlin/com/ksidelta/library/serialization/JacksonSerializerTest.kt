package com.ksidelta.library.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonSerializerTest {
    val jacksonSerializer: Serializer = JacksonSerializer()

    @Test
    fun encodeAndDecodeWorks() {
        val entity = Base(Cringe("xD"))

        val returned = entity
            .let { jacksonSerializer.encode(it) }
            .let { jacksonSerializer.decode(it, Base::class.java) }

        assertEquals(entity, returned)
    }

    data class Base(val obj: Any)
    data class Cringe(val text: String)
}