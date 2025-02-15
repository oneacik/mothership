package com.ksidelta.library.books

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.logger.Logger

class IsbndbClient(val httpClient: HttpClient, val apiKey: String) : BookClient {
    val logger = Logger(IsbndbClient::class.java)

    override fun fetchByIsbn(isbn: String): BookClient.Book? =
        runCatching {
            httpClient.get("https://api2.isbndb.com/book/${isbn}", IsbndbBookResponseDto::class.java) { it.authorize(apiKey) }
                .let {
                    if (it.status != 200) null else it
                }
                ?.data
                ?.let {
                    it.book.run {
                        BookClient.Book(
                            isbn = isbn,
                            title = title,
                            author = authors?.joinToString(", ") ?: "????",
                            year = date_published?.substring(0, 4) ?: "????",
                        )
                    }
                }
        }
            .onFailure { logger.log("IsbndbClient failed to fetch: ${it.message}") }
            .getOrNull()

    data class IsbndbBookResponseDto(val book: Book) {
        data class Book(val title: String, val publisher: String?, val date_published: String?, val authors: List<String>?) {}
    }
}