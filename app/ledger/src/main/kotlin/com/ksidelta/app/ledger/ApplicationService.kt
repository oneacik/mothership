package com.ksidelta.app.ledger

import com.ksidelta.library.banking.Model
import com.ksidelta.library.banking.mergeModels
import com.ksidelta.library.cache.Cache
import com.ksidelta.library.cache.StoreCache
import com.ksidelta.library.google.DriveClient
import com.ksidelta.library.google.DriveService
import com.ksidelta.library.google.OAuthService
import com.ksidelta.library.http.KtorHttpClient
import com.ksidelta.library.logger.Logger
import com.ksidelta.library.memoize.Memoize
import com.ksidelta.library.memoize.StoreMemoize
import com.ksidelta.library.memoize.execute
import com.ksidelta.library.mt940.Mt940Reader
import com.ksidelta.library.store.FileStore
import com.ksidelta.library.table.AsciiRenderer
import com.ksidelta.library.table.Table
import com.ksidelta.library.table.Table.Pos
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.UUID

class ApplicationService(val storagePath: String) {
    private val mt940Reader = Mt940Reader.createForBNPParibas()
    private val driveService: DriveService = DriveService(DriveClient(KtorHttpClient()))

    private val debugStorage: Cache = StoreCache(FileStore("${storagePath}/debug/"), 60)
    private val cache: Cache = StoreCache(FileStore("${storagePath}/cache/"), 3600)
    private val memoize: Memoize = StoreMemoize(cache);
    private val eternalCache: Cache = StoreCache(FileStore("${storagePath}/cache/eternal/"), 30 * 24 * 60 * 60)
    private val eternalMemoize: Memoize = StoreMemoize(eternalCache);

    private val logger: Logger = Logger(ApplicationService::class.java)

    fun calculateLedgerForWholeOrganisation(token: OAuthService.StoredToken, query: Map<String, String>) =
        downloadAllFilesFromGoogleDrive(token.accessToken)
            .convertFilesToModel()
            .convertToLedger(
                monthsFromNow = (query["since"] ?: "12").toLong(),
                contributionsOnly = query["contributions"] == "true",
                anonimized = query["anonimized"] == "true",
                senderOnly = query["short"] == "true"
            )
            .toTable()
            .render(AsciiRenderer())


    private fun downloadAllFilesFromGoogleDrive(token: String): List<Pair<String, String>> =
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

    private fun List<Pair<String, String>>.convertFilesToModel() =
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
    // .also { debugStorage.store("mt940-" + UUID.randomUUID().toString(), it) }

    private fun Model.convertToLedger(
        monthsFromNow: Long = 12L,
        contributionsOnly: Boolean,
        anonimized: Boolean,
        senderOnly: Boolean
    ): FullSummary =
        this.let {
            val keywords = listOf("SKŁADKA", "SKLADKA", "OPLATY", "OPŁATY", "PRZELEW", "DAROWIZNA")

            toContributions(
                it,
                since = LocalDate.now().minusMonths(monthsFromNow).with(ChronoField.DAY_OF_MONTH, 1),
                predicate = { entry ->
                    (!contributionsOnly || entry.amount > BigDecimal.ZERO)
                            && (!contributionsOnly || keywords.find { keyword -> entry.title.contains(keyword, true) } != null)
                },
                mapper = {
                    it
                        .let {
                            if (anonimized && it.amount > BigDecimal.ZERO)
                                it.copy(sender = "DARCZYŃCA", title = "PRZELEW DO NAS")
                            else
                                it
                        }.let {
                            if (senderOnly)
                                it.copy(title = ">>")
                            else
                                it
                        }
                }
            )
        }.also {
            debugStorage.store("grouped-" + UUID.randomUUID().toString(), it)
        }


    private fun FullSummary.toTable(): Table {
        val li = this

        val allEntries = this.entries.flatMap { it.entries }
        val dates = allEntries.map { it.date }.distinct()

        val entries = this.entries.flatMap {
            val name = "${it.sender}: ${it.title}"

            val entries = listOf(
                Table.Cell(
                    Pos(2, "SUM"),
                    Pos(0, name),
                    it.sum.toString()
                ),
                Table.Cell(
                    Pos(1, "REMAINING"),
                    Pos(0, name),
                    it.remaining.toString()
                )
            ) + it.entries.map {
                Table.Cell(
                    Pos(0, it.date.toString()),
                    Pos(0, name),
                    it.amount.toString()
                )
            }



            entries
        }

        val summedByDates = allEntries
            .map { Pair(it.date, it.amount) }
            .groupBy { it.first }
            .map { (date, entries) ->
                Table.Cell(
                    Pos(0, date.toString()),
                    Pos(1, "SUM"),
                    entries.fold(BigDecimal.ZERO) { left, entry -> left + entry.second }.toString()
                )
            }
        val summary = listOf(
            Table.Cell(
                Pos(0, "SUM"),
                Pos(1, "SUM"),
                li.sum.toString()
            ),
            Table.Cell(
                Pos(0, "REMAINING"),
                Pos(1, "SUM"),
                li.fullRemaining.toString()
            ),
        )

        return Table(entries + summedByDates + summary)
    }

    private fun Map<String, List<Model.Entry>>.toTable(): Table =
        this.entries.flatMap { (key, entries) ->
            entries.map { Pair(key, it) }
        }.let { summaries ->
            listOf(
                summaries
                    .map { (key, entry) ->
                        Table.Cell(
                            Pos(0, entry.date.toString()),
                            Pos(0, key),
                            entry.amount.toString()
                        )
                    },
                summaries
                    .groupBy { (key, _) -> key }
                    .map { (key, entries) ->
                        Table.Cell(
                            Pos(1, "SUM"),
                            Pos(0, key),
                            entries.map { it.second.amount }.reduce { a, b -> a + b }.toString()
                        )
                    },
                summaries
                    .groupBy { (_, value) -> value.date.toString() }
                    .map { (date, entries) ->
                        Table.Cell(
                            Pos(0, date),
                            Pos(1, "SUM"),
                            entries.map { it.second.amount }.reduce { a, b -> a + b }.toString()
                        )
                    },
                summaries
                    .map { it.second.amount }
                    .reduce { a, b -> a + b }
                    .let {
                        Table.Cell(
                            Pos(1, "SUM"),
                            Pos(1, "SUM"),
                            it.toString()
                        )
                    }
                    .let { listOf(it) }
            ).flatten()
        }
            .let { Table(it) }

    private fun ByteArray.encodeToString(): String =
        String(this, Charset.defaultCharset()).let {
            if (it.contains("�"))
                String(this, Charset.forName("WINDOWS-1250"))
                    .also { logger.log("WINDOWS-1250 File Found") }
            else
                it
        }

}