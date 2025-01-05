import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.http.KtorHttpClient
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

object Main {
    val OAuthService = OAuthService(
        Configuration(
            clientId = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_ID")
                ?: throw IllegalStateException("CO JEST KURWA"),
            clientSecret = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_SECRET")
                ?: throw IllegalStateException("CO JEST KURWA"),
            redirectUrl = "http://localhost:8080/oauth/handle"
        ),
        KtorHttpClient()
    )

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8080) {
            routing {
                route("/oauth") {
                    get("/") {
                        OAuthService.initiate { url -> runBlocking { call.respondRedirect(url, false) } }
                    }
                    get("/handle") {
                        OAuthService.handleReturn(
                            call.parameters["code"] ?: throw IllegalStateException("GDZIE KURWA KOD")
                        )
                        call.respondText("SPOKO")
                    }
                }
            }
        }.start(wait = true)
    }
}