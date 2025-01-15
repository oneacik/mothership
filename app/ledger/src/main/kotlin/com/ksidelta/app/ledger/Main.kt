package com.ksidelta.app.ledger

import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.OAuthClient
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.google.ProfileClient
import com.ksidelta.library.google.ProfileService
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.session.KtorSessionRepository
import com.ksidelta.library.store.FileStore
import com.ksidelta.library.store.Store
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking
import java.util.UUID

object Main {
    val storage: Store = FileStore("./storage/")
    val sessions: KtorSessionRepository = KtorSessionRepository(storage)
    val oAuthService = OAuthService(
        Configuration(
            clientId = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_ID")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_ID"),
            clientSecret = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_SECRET")
                ?: throw IllegalStateException("GDZIE JEST MOTHERSHIP_GOOGLE_CLIENT_SECRET"),
            redirectUrl = "http://localhost:8080/oauth/handle"
        ),
        OAuthClient(KtorHttpClient())
    )
    val profileService: ProfileService = ProfileService(ProfileClient(KtorHttpClient()))

    val logger: Logger = Logger(Main::class.java)
    val applicationService: ApplicationService = ApplicationService()

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    storage.keys()
                        .filter { it.startsWith("credentials-") }
                        .map { it.replace("credentials-", "") }
                        .let { HtmlFiles.createIndex(it) }
                        .also { call.respondText(it, ContentType.Text.Html) }
                }
                get("/cookie") {
                    call.response.cookies.append("CRINGE", UUID.randomUUID().toString())
                    call.respondRedirect("/cookie", false)
                }



                route("/oauth") {
                    get("/handle") {
                        handleErrors {
                            oAuthService.handleReturn(call.parameters.toMap(), call.redirectFun())
                                .let {
                                    sessions.fetch(call)
                                        .store(OAuthService.StoredToken::class.java, it)
                                }
                        }
                    }
                }
                route("/ledger") {
                    get("/") {
                        handleErrors {
                            sessions.fetch(call)
                                .fetch(OAuthService.StoredToken::class.java)
                                .let { oAuthService.ensureFresh(it, call.redirectFun(), "/ledger/") }
                                ?.let { token ->
                                    applicationService.calculateLedgerForWholeOrganisation(token,
                                        call.parameters.entries()
                                            .map { (key, value) -> Pair(key, value.joinToString("")) }
                                            .toMap()
                                    )
                                        .let { call.respondText(it) }
                                } ?: call.respondRedirect("/oauth/init", false)
                        }
                    }

                    route("/shared") {
                        get("/") {
                            call.parameters["email"]!!
                                .let { email ->
                                    storage.get(
                                        "credentials-${email}",
                                        OAuthService.StoredToken::class.java
                                    )
                                }
                                ?.let { token -> oAuthService.ensureFresh(token, { throw IllegalStateException("Non Interactive") }, "", true) }
                                ?.let { token ->
                                    applicationService.calculateLedgerForWholeOrganisation(
                                        token,
                                        call.parameters.entries()
                                            .map { (key, value) -> Pair(key, value.joinToString("")) }
                                            .toMap() + mapOf("anonimized" to "true")
                                    )
                                }
                                ?.let { output -> call.respondText(output) }
                                ?: call.respondText("Gdzie token?", status = HttpStatusCode.Unauthorized)

                        }
                        get("/share") {
                            handleErrors {
                                sessions.fetch(call)
                                    .fetch(OAuthService.StoredToken::class.java)
                                    .let { token ->
                                        oAuthService.ensureFresh(
                                            token,
                                            call.redirectFun(),
                                            "/ledger/shared/share",
                                            true
                                        )
                                            ?.let { token ->
                                                profileService.me(token.accessToken)
                                                    .also { user ->
                                                        storage.store(
                                                            "credentials-${user.email}",
                                                            token
                                                        )
                                                    }.let { user ->
                                                        call.respondText(
                                                            """
                                                                <html>
                                                                <body>
                                                                    <a href="/ledger/shared/${user.email}/">Shared Path</a> 
                                                                </body>
                                                                </html>
                                                            """.trimIndent(),
                                                            contentType = ContentType.Text.Html
                                                        )
                                                    }
                                            }
                                    }
                            }
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