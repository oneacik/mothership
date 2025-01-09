import com.ksidelta.library.banking.Model
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoField

fun transformModel(model: Model, since: LocalDate = LocalDate.now().minusYears(1)) =
    model.entries
        .filter { it.date > since }
        .filter { it.amount > BigDecimal.ZERO }
        .map { it.copy(date = it.date.with(ChronoField.DAY_OF_MONTH, 1)) }
        .map { it.copy(sender = it.sender.findFullNameOrDie()) } //
        .sumSameDate()
        .groupBy { it.sender + ": " + it.title }


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
        ?: "PLACEHOLDER"

data class GroupBy(val sender: String, val title: String, val date: LocalDate)

// data class Grouped(val key: String, val transactions: List<Transaction>) {}
data class Transaction(val amount: BigDecimal, val date: LocalDate)

