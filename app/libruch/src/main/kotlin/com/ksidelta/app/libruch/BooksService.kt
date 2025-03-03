package com.ksidelta.app.libruch

import com.ksidelta.library.books.BookClient
import com.ksidelta.library.email.EmailService
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.google.SpreadsheetClient
import com.ksidelta.library.store.Store
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BooksService(
    val stateStorage: Store,
    val bookStorage: Store,
    val authenticationStorage: Store,
    val oAuthService: OAuthService,
    val spreadsheetClient: SpreadsheetClient,
    val emailService: EmailService,
    val baseUrl: String,
) {
    val SPLITTER: String = "#"

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            {
                runCatching { sync() }
                    .onFailure { it.printStackTrace() }
            },
            0,
            100, TimeUnit.SECONDS
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

    fun createLogin(forEmail: String) {
        val uuid = UUID.randomUUID()
        authenticationStorage.store(uuid.toString(), Authentication(forEmail))
        emailService.sendHTML(
            "no-reply@libruch.hsp.sh", forEmail, "Login Link", """
            <html>
                <body>
                    Click here to <a href="${baseUrl}/api/login/${uuid}">authenticate</a> to libruch app.
                    
                    You can use this link to log in as many times as you like.
                    Though, it is ok to resend it.
                </body>
            </html>
        """.trimIndent(), "Libruch"
        )
    }

    fun login(uuid: String): String? =
        authenticationStorage.get(uuid, Authentication::class.java)?.email

    data class Authentication(val email: String)
}