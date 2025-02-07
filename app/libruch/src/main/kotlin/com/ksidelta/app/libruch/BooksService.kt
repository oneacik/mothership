package com.ksidelta.app.libruch

import com.ksidelta.library.books.BookClient
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.google.SpreadsheetClient
import com.ksidelta.library.store.Store
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BooksService(
    val stateStorage: Store,
    val bookStorage: Store,
    val oAuthService: OAuthService,
    val spreadsheetClient: SpreadsheetClient
) {
    val SPLITTER: String = "#"

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            {
                runCatching { sync() }
                    .onFailure { it.printStackTrace() }
            },
            0,
            5, TimeUnit.SECONDS
        )
    }


    fun sync() {
        val token = stateStorage.get("token", OAuthService.StoredToken::class.java)
            .let { oAuthService.ensureFresh(it, { throw IllegalStateException() }, "", true) }
            .also { stateStorage.store("token", it!!) }
            .let { it!!.accessToken }

        val spreadsheetId = stateStorage.get("spreadsheet", SpreadsheetClient.NewSpreadsheetResponse::class.java)
            .let { requireNotNull(it) }
            .let { it.spreadsheetId }


        val books = bookStorage.keys()
            .map { key ->
                bookStorage.get(key, BookClient.Book::class.java)
                    .let { book ->
                        BookWithOwner(key.split("#")[0], requireNotNull(book))
                    }
            }.sortedWith(compareBy({ it.owner }, { it.book.title }, { it.book.isbn }))

        val listOfFields = books.map { bookWithOwner ->
            bookWithOwner.book.run { listOf(bookWithOwner.owner, title, author, year, isbn) }
        }

        spreadsheetClient.updateValues(
            token,
            spreadsheetId,
            "B2:F${2 + listOfFields.size - 1}",
            listOfFields
        )
    }

    data class BookWithOwner(val owner: String, val book: BookClient.Book)
}