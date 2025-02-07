package com.ksidelta.app.libruch

import com.ksidelta.library.books.BookClient
import com.ksidelta.library.books.GoogleBookClient
import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.OAuthClient
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.google.SpreadsheetClient
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.store.FileStore
import com.ksidelta.library.store.Store
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking

object Main {
    val booksClient: GoogleBookClient = GoogleBookClient.unauthenticated()
    val logger: Logger = Logger(Main::class.java)
    val bookStorage: Store = FileStore("./storage/books/")
    val stateStorage: Store = FileStore("./storage/state/")
    val oAuthService = OAuthService(
        Configuration(
            clientId = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_ID")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_ID"),
            clientSecret = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_SECRET")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_SECRET"),
            redirectUrl = "http://localhost".let { "$it/api/auth/handle" }
        ),
        OAuthClient(KtorHttpClient())
    )
    val spreadsheetClient: SpreadsheetClient = SpreadsheetClient(KtorHttpClient())

    val bookService: BooksService = BooksService(
        stateStorage,
        bookStorage,
        oAuthService,
        spreadsheetClient
    )

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                jackson()
            }

            routing {
                route("/api") {
                    get("/spreadsheet") {
                        stateStorage.get("spreadsheet", SpreadsheetClient.NewSpreadsheetResponse::class.java)
                            ?.let { call.respondRedirect(it.spreadsheetUrl, permanent = false) }
                            ?: call.respond(HttpStatusCode.NotFound, "Service is not initiated yet")
                    }

                    route("/auth") {
                        get("/handle") {
                            oAuthService.handleReturn(call.parameters.toMap(), call.redirectFun())
                                .let { stateStorage.store("token", it) }
                        }

                        get("/init") {
                            stateStorage.get("token", OAuthService.StoredToken::class.java)
                                .let { oAuthService.ensureFresh(it, call.redirectFun(), "/api/auth/init", offline = true) }
                                .also { token -> token?.let { stateStorage.store("token", it) } }
                                .let { call.respond("Token Assigned") }
                        }

                        get("/test") {
                            handleErrors {
                                stateStorage.get("token", OAuthService.StoredToken::class.java)
                                    .let { oAuthService.ensureFresh(it, call.redirectFun(), "/api/auth/init", offline = true) }
                                    .also { token -> token?.let { stateStorage.store("token", it) } }
                                    .let { token ->
                                        spreadsheetClient.create(token!!.accessToken).also {
                                            // spreadsheetClient.createSheet(token.accessToken, it.spreadsheetId, "JAJEC", 1)
                                            // spreadsheetClient.updateValues(
                                            //     token!!.accessToken, it.spreadsheetId, "A1:B3", listOf(
                                            //         listOf("N", "I"),
                                            //         listOf("G", "G"),
                                            //         listOf("E", "R")
                                            //     )
                                            // )
                                            stateStorage.store("spreadsheet", it)
                                            bookService.sync()
                                        }
                                    }
                                    .also { call.respond(it) }
                            }
                        }
                    }


                    route("/library") {
                        route("/{email}") {
                            get("/") {
                                val email = call.parameters["email"]!!

                                bookStorage.keys().filter { it.startsWith(email) }
                                    .map { bookStorage.get(it, BookClient.Book::class.java) }
                                    .let { call.respond(mapOf("books" to it)) }
                            }

                            put("/{isbn}") {
                                val email = call.parameters["email"]!!
                                val isbn = call.parameters["isbn"]!!

                                booksClient.fetchByIsbn(isbn)
                                    ?.let {
                                        bookStorage.store("${email}#${isbn}", it)
                                            .also { call.respond(HttpStatusCode.OK) }
                                    }
                                    ?: call.respondText("Book Not Found", status = HttpStatusCode.NotFound)
                            }
                        }
                    }

                    route("/books") {
                        get("/{isbn}") {
                            booksClient.fetchByIsbn(call.parameters["isbn"]!!)
                                ?.let { call.respond(it) }
                                ?: call.respondText("Book Not Found", status = HttpStatusCode.NotFound)

                        }
                    }
                }
            }
        }.start(wait = true)
    }

    fun RoutingCall.redirectFun(): (String) -> Unit =
        { url -> runBlocking { this@redirectFun.respondRedirect(url, false) } }


    suspend fun RoutingContext.handleErrors(runnable: suspend RoutingContext.() -> Unit) {
        runCatching {
            runnable()
        }
            .onFailure {
                call.respondText(it.toString(), status = HttpStatusCode.InternalServerError)
                logger.log(it.toString())
            }
    }
}

// infix fun <T, U> T.`*`(func: (T) -> U): U = func(this)