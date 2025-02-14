package com.ksidelta.library.books

import com.ksidelta.library.http.KtorHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals

class IsbndbClientTest {

    @Test
    fun sendMail() {
        val service = IsbndbClient(KtorHttpClient(), System.getenv("MOTHERSHIP_ISBNDB_APIKEY") ?: throw IllegalStateException("WHERE KEY"))
        requireNotNull(service.fetchByIsbn("9788328330306")).apply {
            assertEquals("Doktryna jakosci", title)
            assertEquals("Andrzej Jacek Blikle", author)
            assertEquals("2021", year)
        }
    }
}