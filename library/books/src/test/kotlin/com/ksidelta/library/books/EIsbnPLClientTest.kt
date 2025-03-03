package com.ksidelta.library.books

import com.ksidelta.library.http.KtorHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EIsbnPLClientTest {

    @Test
    fun testExistingBook() {
        val service = EIsbnPLClient(KtorHttpClient())

        requireNotNull(service.fetchByIsbn("9788381884105")).apply {
            assertEquals("Kroniki Diuny", title)
            assertEquals("Frank Herbert", author)
            assertEquals("2022", year)
        }
    }

    @Test
    fun testNonExistingBook() {
        val service = EIsbnPLClient(KtorHttpClient())

        assertNull(service.fetchByIsbn("9999999999999"))
    }
}