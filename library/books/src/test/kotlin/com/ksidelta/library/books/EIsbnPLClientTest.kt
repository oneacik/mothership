package com.ksidelta.library.books

import com.ksidelta.library.books.EIsbnPLClient
import com.ksidelta.library.http.KtorHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals

class EIsbnPLClientTest {

    @Test
    fun sendMail() {
        val service = EIsbnPLClient(KtorHttpClient())
        requireNotNull(service.fetchByIsbn("9788381884105")).apply {
            assertEquals("Doktryna jakosci", title)
            assertEquals("Andrzej Jacek Blikle", author)
            assertEquals("2021", year)
        }
    }
}