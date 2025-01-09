import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.DriveClient
import com.ksidelta.library.google.DriveService
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.http.Memoize
import com.ksidelta.library.http.StoreMemoize
import com.ksidelta.library.http.execute
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.banking.Model
import com.ksidelta.library.mt940.Mt940Reader
import com.ksidelta.library.banking.mergeModels
import com.ksidelta.library.store.FileStore
import com.ksidelta.library.store.Store
import com.ksidelta.library.table.AsciiRenderer
import com.ksidelta.library.table.Table
import com.ksidelta.library.table.Table.Pos
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import java.util.UUID

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
    val mt940Reader = Mt940Reader.createForBNPParibas()
    val storage: Store = FileStore("./storage/")
    val driveService: DriveService = DriveService(DriveClient(KtorHttpClient()))
    val memoize: Memoize = StoreMemoize(storage);
    val logger: Logger = Logger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8080) {
            routing {
                route("/oauth") {
                    get("/") {
                        OAuthService.initiate { url -> runBlocking { call.respondRedirect(url, false) } }
                    }
                    get("/handle") {
                        runCatching {
                            OAuthService.handleReturn(
                                call.parameters["code"] ?: throw IllegalStateException("GDZIE KURWA KOD")
                            ).let { token ->
                                memoize.execute("listFiles-" + "profile.name"/*token.access_token*/) {
                                    driveService.listFiles(
                                        token.access_token
                                    )
                                }
                                    .let {
                                        it.filter { it.name.endsWith(".mt940") }
                                    }
                                    .let {
                                        it.map { file ->
                                            println("PLIK: ${file.name}")
                                            memoize.execute("download-${file.name}-${file.id}") {
                                                driveService.download(
                                                    token.access_token,
                                                    file.id
                                                ).encodeToString()
                                            }
                                                .let {
                                                    runCatching { mt940Reader.read(it) }
                                                        .onFailure {
                                                            logger.log(
                                                                "Failed on {} because {}",
                                                                file.name,
                                                                it.message ?: "???"
                                                            )

                                                        }
                                                        .getOrElse { Model(sortedSetOf()) }
                                                }
                                        }.mergeModels()
                                    }
                                    .also { storage.store("mt940-" + UUID.randomUUID().toString(), it) }
                                    .let {
                                        transformModel(it)
                                    }.also {
                                        storage.store("grouped-" + UUID.randomUUID().toString(), it)
                                    }.let { transfers ->
                                        transfers.entries
                                            .flatMap { (key, entries) ->
                                                entries.map {
                                                    Table.Cell(
                                                        Pos(it.date.toString()),
                                                        Pos(key),
                                                        it.amount.toString()
                                                    )
                                                } + listOf(
                                                    Table.Cell(
                                                        Pos("SUM", 1),
                                                        Pos(key),
                                                        entries.map { it.amount }.reduce { a, b -> a + b }.toString()
                                                    )
                                                )
                                            }
                                            .let {
                                                transfers.entries
                                                    .flatMap { (_, entries) -> entries }
                                                    .groupBy { it.date }
                                                    .entries
                                                    .map { (date, value) ->
                                                        Pair(
                                                            date,
                                                            value.map { it.amount }.reduce { a, b -> a + b }
                                                        )
                                                    }
                                                    .map { (date, value) ->
                                                        Table.Cell(
                                                            Pos(date.toString()),
                                                            Pos("SUM", 1),
                                                            value.toString()
                                                        )
                                                    } + it

                                            }
                                            .let { Table(it) }
                                            .let { it.render(AsciiRenderer()) }
                                            .let {
                                                call.respondText(it)
                                            }

                                    }
                            }
                        }
                            .onFailure {
                                it.printStackTrace()
                            }
                            .getOrThrow()

                    }
                }
            }
        }.start(wait = true)
    }

    fun ByteArray.encodeToString(): String =
        String(this, Charset.defaultCharset()).let {
            if (it.contains("�"))
                String(this, Charset.forName("WINDOWS-1250"))
                    .also { logger.log("WINDOWS-1250 File Found") }
            else
                it
        }

}


// infix fun <T, U> T.`*`(func: (T) -> U): U = func(this)