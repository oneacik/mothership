package com.ksidelta.app.libruch

import com.ksidelta.library.books.BookClient
import com.ksidelta.library.books.EIsbnPLClient
import com.ksidelta.library.books.FallbackClient
import com.ksidelta.library.books.GoogleBookClient
import com.ksidelta.library.books.IsbndbClient
import com.ksidelta.library.cache.passthrough
import com.ksidelta.library.email.EmailService
import com.ksidelta.library.email.SendGridEmailService
import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.OAuthClient
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.google.SpreadsheetClient
import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.session.KtorSessionRepository
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

    val PROD = System.getenv("MOTHERSHIP_PROD") != null
    val storagePath = System.getenv("STORAGE_PATH") ?: "./storage"
    val baseUrl = System.getenv("BASE_URL") ?: "http://localhost"
    val isbndbKey = System.getenv("MOTHERSHIP_ISBNDB_APIKEY") ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_ISBNDB_APIKEY")
    val ktorHttpClient = KtorHttpClient()
    val booksClient: BookClient = FallbackClient(
        listOf(
            GoogleBookClient.unauthenticated(),
            EIsbnPLClient(ktorHttpClient),
            IsbndbClient(ktorHttpClient, isbndbKey)
        )
    )
    val logger: Logger = Logger(Main::class.java)
    val isbnCache: Store = FileStore("${storagePath}/cache/isbns")
    val bookStorage: Store = FileStore("${storagePath}/books/")
    val stateStorage: Store = FileStore("${storagePath}/state/")
    val authenticationStorage: Store = FileStore("${storagePath}/state/")
    val sessions: KtorSessionRepository = KtorSessionRepository(FileStore("${storagePath}/sessions/"))
    val oAuthService = OAuthService(
        Configuration(
            clientId = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_ID")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_ID"),
            clientSecret = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_SECRET")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_SECRET"),
            redirectUrl = baseUrl.let { "$it/api/auth/handle" }
        ),
        OAuthClient(KtorHttpClient())
    )
    val spreadsheetClient: SpreadsheetClient = SpreadsheetClient(KtorHttpClient())
    val emailService: EmailService = SendGridEmailService(
        System.getenv("MOTHERSHIP_SENDGRID_APIKEY")
            ?: throw IllegalStateException("GDZIE JEST SENDGRID")
    );


    val bookService: BooksService = BooksService(
        stateStorage,
        bookStorage,
        authenticationStorage,
        oAuthService,
        spreadsheetClient,
        emailService,
        baseUrl
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

                    route("/login") {
                        post("/challenge/{email}") {
                            val email = call.pathParameters["email"]!!
                            bookService.createLogin(email)
                            call.respond("OK")
                        }
                        get("/{challenge}") {
                            val challenge = call.pathParameters["challenge"]!!
                            bookService.login(challenge)?.let { email ->
                                sessions.fetch(call).store(AuthenticatedEmail::class.java, AuthenticatedEmail(email))
                                call.respondRedirect("/app")
                            } ?: call.respond(HttpStatusCode.BadRequest, "Challenge Failed")
                        }
                        get("/email") {
                            sessions.fetch(call).fetch(AuthenticatedEmail::class.java)?.let {
                                call.respond(HttpStatusCode.OK, it.email)
                            } ?: call.respond(HttpStatusCode.Unauthorized)
                        }
                        if (!PROD) {
                            get("/email/{email}") {
                                val email = call.pathParameters["email"]!!
                                sessions.fetch(call).store(AuthenticatedEmail::class.java, AuthenticatedEmail(email))
                                call.respond("OK!")
                            }
                        }
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
                                            stateStorage.store("spreadsheet", it)
                                            bookService.sync()
                                        }
                                    }
                                    .also { call.respond(it) }
                            }
                        }
                    }


                    route("/library") {
                        get("/") {
                            val session = sessions.fetch(call).fetch(AuthenticatedEmail::class.java)
                            session?.email?.let { email ->
                                bookStorage.keys().filter { it.startsWith(email) }
                                    .map { bookStorage.get(it, BookClient.Book::class.java) }
                                    .let { call.respond(mapOf("books" to it)) }
                            } ?: call.respond(HttpStatusCode.Unauthorized)
                        }

                        put("/{isbn}") {
                            val session = sessions.fetch(call).fetch(AuthenticatedEmail::class.java)
                            session?.email?.let { email ->
                                val email = session.email
                                val isbn = call.parameters["isbn"]!!

                                isbnCache.passthrough(isbn) { booksClient.fetchByIsbn(isbn) }
                                    ?.let {
                                        bookStorage.store("${email}#${isbn}", it)
                                            .also { call.respond(HttpStatusCode.OK) }
                                    }
                                    ?: call.respondText("Book Not Found", status = HttpStatusCode.NotFound)
                            } ?: call.respond(HttpStatusCode.Unauthorized)
                        }
                    }

                    route("/books") {
                        get("/{isbn}") {
                            val isbn = call.parameters["isbn"]!!
                            isbnCache.passthrough(isbn) { booksClient.fetchByIsbn(isbn) }
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

data class AuthenticatedEmail(val email: String)

// infix fun <T, U> T.`*`(func: (T) -> U): U = func(this)