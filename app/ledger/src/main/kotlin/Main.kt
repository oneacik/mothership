import com.ksidelta.library.google.Configuration
import com.ksidelta.library.google.DriveClient
import com.ksidelta.library.google.DriveService
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.memoize.Memoize
import com.ksidelta.library.memoize.StoreMemoize
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.banking.Model
import com.ksidelta.library.mt940.Mt940Reader
import com.ksidelta.library.banking.mergeModels
import com.ksidelta.library.cache.Cache
import com.ksidelta.library.cache.StoreCache
import com.ksidelta.library.google.OAuthClient
import com.ksidelta.library.memoize.execute
import com.ksidelta.library.session.KtorSessionRepository
import com.ksidelta.library.store.FileStore
import com.ksidelta.library.store.Store
import com.ksidelta.library.table.AsciiRenderer
import com.ksidelta.library.table.Table
import com.ksidelta.library.table.Table.Pos
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.UUID

object Main {
    val storage: Store = FileStore("./storage/")
    val sessions: KtorSessionRepository = KtorSessionRepository(storage)
    val oAuthService = OAuthService(
        Configuration(
            clientId = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_ID")
                ?: throw IllegalStateException("CO JEST KURWA"),
            clientSecret = System.getenv("MOTHERSHIP_GOOGLE_CLIENT_SECRET")
                ?: throw IllegalStateException("CO JEST KURWA"),
            redirectUrl = "http://localhost:8080/oauth/handle"
        ),
        OAuthClient(KtorHttpClient())
    )
    val mt940Reader = Mt940Reader.createForBNPParibas()
    val driveService: DriveService = DriveService(DriveClient(KtorHttpClient()))

    val cache: Cache = StoreCache(FileStore("./storage/cache/"), 3600)
    val memoize: Memoize = StoreMemoize(cache);
    val eternalCache: Cache = StoreCache(FileStore("./storage/cache/"), 30 * 24 * 60 * 60)
    val eternalMemoize: Memoize = StoreMemoize(eternalCache);

    val logger: Logger = Logger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, port = 8080) {
            routing {
                get("/") { call.respondRedirect("/ledger/", false) }
                get("/cookie") {
                    call.response.cookies.append("CRINGE", UUID.randomUUID().toString())
                    call.respondRedirect("/cookie", false)
                }
                route("/oauth") {
//                    get("/init") {
//                        oAuthService.initiate(call.redirectFun())
//                    }
                    get("/handle") {
                        runCatching {
                            oAuthService.handleReturn(call.parameters.toMap(), call.redirectFun())
                                .let {
                                    sessions.fetch(call)
                                        .store(OAuthService.StoredToken::class.java, it)
                                }
                        }.onFailure {
                            logger.log(it.message.toString())
                        }
                    }
                }
                route("/ledger") {
                    get("/") {
                        runCatching {
                            sessions.fetch(call)
                                .fetch(OAuthService.StoredToken::class.java)
                                .let { oAuthService.ensureFresh(it, call.redirectFun(), "/ledger/") }
                                ?.let { token ->
                                    downloadAllFilesFromGoogleDrive(token.accessToken)
                                        .convertFilesToModel()
                                        .convertToPersonalLedger()
                                        .toTable()
                                        .render(AsciiRenderer())
                                        .let {
                                            call.respondText(it)
                                        }
                                } ?: call.respondRedirect("/oauth/init", false)
                        }
                            .onFailure {
                                call.respondText(it.toString())
                            }
                    }
                }
            }
        }.start(wait = true)
    }

    fun downloadAllFilesFromGoogleDrive(token: String): List<Pair<String, String>> =
        memoize.execute("listFiles-" + UUID.randomUUID().toString()) {
            driveService.listFiles(
                token
            ) { it.copy(contains = "mt940") }
        }
            .let {
                it.filter { it.name.endsWith(".mt940") }
            }
            .map { file ->
                Pair(file.name,
                    eternalMemoize.execute("download-${file.name}-${file.id}") {
                        driveService.download(
                            token,
                            file.id
                        ).encodeToString()
                    }
                )
            }

    fun List<Pair<String, String>>.convertFilesToModel() =
        this.map { (fileName, contents) ->
            contents
                .let {
                    runCatching { mt940Reader.read(it) }
                        .onFailure {
                            logger.log(
                                "Failed on {} because {}",
                                fileName,
                                it.message ?: "???"
                            )

                        }
                        .getOrElse { Model(sortedSetOf()) }
                }
        }
            .mergeModels()
            .also { storage.store("mt940-" + UUID.randomUUID().toString(), it) }

    fun Model.convertToPersonalLedger(
        monthsFromNow: Long = 12L,
    ): Map<String, List<Model.Entry>> =
        this.let {
            transformModel(
                it,
                since = LocalDate.now().minusMonths(monthsFromNow).with(ChronoField.DAY_OF_MONTH, 1)
            )
        }.also {
            storage.store("grouped-" + UUID.randomUUID().toString(), it)
        }


    fun Map<String, List<Model.Entry>>.toTable(): Table =
        this.entries.flatMap { (key, entries) ->
            entries.map { Pair(key, it) }
        }.let { summaries ->
            listOf(
                summaries
                    .map { (key, entry) ->
                        Table.Cell(
                            Pos(entry.date.toString()),
                            Pos(key),
                            entry.amount.toString()
                        )
                    },
                summaries
                    .groupBy { (key, _) -> key }
                    .map { (key, entries) ->
                        Table.Cell(
                            Pos("SUM", 1),
                            Pos(key),
                            entries.map { it.second.amount }.reduce { a, b -> a + b }.toString()
                        )
                    },
                summaries
                    .groupBy { (_, value) -> value.date.toString() }
                    .map { (date, entries) ->
                        Table.Cell(
                            Pos(date),
                            Pos("SUM", 1),
                            entries.map { it.second.amount }.reduce { a, b -> a + b }.toString()
                        )
                    },
                summaries
                    .map { it.second.amount }
                    .reduce { a, b -> a + b }
                    .let {
                        Table.Cell(
                            Pos("SUM", 1),
                            Pos("SUM", 1),
                            it.toString()
                        )
                    }
                    .let { listOf(it) }
            ).flatten()
        }
            .let { Table(it) }

    fun ByteArray.encodeToString(): String =
        String(this, Charset.defaultCharset()).let {
            if (it.contains("�"))
                String(this, Charset.forName("WINDOWS-1250"))
                    .also { logger.log("WINDOWS-1250 File Found") }
            else
                it
        }

    fun RoutingCall.redirectFun(): (String) -> Unit =
        { url -> runBlocking { this@redirectFun.respondRedirect(url, false) } }
}


// infix fun <T, U> T.`*`(func: (T) -> U): U = func(this)