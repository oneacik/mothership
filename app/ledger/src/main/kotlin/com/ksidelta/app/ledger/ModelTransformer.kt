package com.ksidelta.app.ledger

import com.ksidelta.library.banking.Model
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoField

fun toContributions(
    model: Model,
    since: LocalDate = LocalDate.now().minusYears(1).with(ChronoField.DAY_OF_MONTH, 1),
    predicate: (Model.Entry) -> Boolean,
    mapper: (Model.Entry) -> Model.Entry
): FullSummary =
    model.entries
        .filter { it.date > since }
        .filter(predicate)
        .map { it.copy(date = it.date.with(ChronoField.DAY_OF_MONTH, 1)) }
        .map { it.copy(sender = it.sender.findFullNameOrDie()) } //
        .map(mapper)
        .sumSameDate()
        .groupBy { it.sender + ": " + it.title }
        .values
        .map { GroupedSummary.create(it) }
        .let { FullSummary.create(it) }


fun List<Model.Entry>.sumSameDate() =
    this
        .groupBy { GroupBy(it.sender ?: "PLACEHOLDER", it.title, it.date) }
        .entries
        .map { (_, entries) ->
            entries.sumOf { it.amount }.let {
                entries.first().copy(amount = it)
            }
        }

fun String?.findFullNameOrDie(): String =
    this?.run { Regex("""^([\p{L}\w]+)\s+([\p{L}\w]+)""").find(this) }
        ?.run { groups[1]!!.value + " " + groups[2]!!.value }
        ?: this ?: "UNKNOWN"

data class GroupBy(val sender: String, val title: String, val date: LocalDate)

data class FullSummary(
    val entries: List<GroupedSummary>,
    val sum: BigDecimal,
    val fullRemaining: BigDecimal
) {
    companion object {
        fun create(entries: List<GroupedSummary>) =
            FullSummary(
                entries,
                entries.map { it.sum }.reduce { a, b -> a.plus(b) },
                entries.map { it.remaining }.reduce { a, b -> a.plus(b) }
            )
    }
}

data class GroupedSummary(
    val sender: String, val title: String,
    val sum: BigDecimal, val remaining: BigDecimal,
    val entries: List<Model.Entry>
) {
    companion object {
        fun create(entries: List<Model.Entry>): GroupedSummary {
            val first = entries.first()
            val firstEntry = entries.sortedBy { it.date }.first()

            val monthsBetween = Period.between(
                firstEntry.date,
                LocalDate.now()
            ).months

            val sum = entries.map { it.amount }.reduce { a, b -> a.plus(b) }
            val middleSum = entries.map { it.amount }.sorted()
                .let { it[it.size / 2] }
                .let {
                    if (it > BigDecimal.valueOf(150.00))
                        BigDecimal.valueOf(150.00)
                    else
                        it
                }
            val expected = BigDecimal.valueOf(
                middleSum
                    .multiply(
                        BigDecimal.valueOf(monthsBetween.toLong())
                    ).toLong()
            )

            return GroupedSummary(
                sender = first.sender ?: "???",
                title = first.title,
                sum = sum,
                remaining = (expected - sum)
                    .let { if (it > BigDecimal.ZERO) it else BigDecimal.ZERO },
                entries = entries,
            )
        }
    }
}