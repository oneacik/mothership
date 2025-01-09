package com.ksidelta.library.mt940

class Tokenizer(
    schema: List<TokenSchema> = listOf(
        TokenSchema(":20:", "1"),
        TokenSchema(":25:", "/2!a26!n"),
        TokenSchema(":28C:", "3n/4!n/3a"),    // DATA
        TokenSchema(":60F:", "1!a6!n3!a15d"), // SALDO POCZĄTKOWE
        TokenSchema(":62F:", "1!a6!n3!a15d"), // KOŃCOWE SALDO

        // JEBANA KWOTA PRZELEWU W JAKIEJ PRZYSTĘPNEJ FORMIE JA PIERDOLĘ
        TokenSchema(":61:", "6n4n2a15d1!a3!c50x"),
        TokenSchema(":86:", "3!x"),
        TokenSchema("\\^00", "35x"),
        TokenSchema("\\^34", "3!n"),
        TokenSchema("\\^30", "10x"),

        // zasrana nazwa przelewu
        TokenSchema("\\^20", "27x"),
        TokenSchema("\\^21", "27x"),
        TokenSchema("\\^22", "27x"),
        TokenSchema("\\^23", "27x"),
        TokenSchema("\\^24", "27x"),
        TokenSchema("\\^25", "27x"),
        TokenSchema("\\^26", "27x"),
        TokenSchema("\\^27", "27x"),

        // nazwa kontrahenta - TODO
        TokenSchema("\\^32", "30x"),
        TokenSchema("\\^33", "35x"),
        TokenSchema("\\^62", "35x"),
        TokenSchema("\\^63", "35x"),

        // kod operacji
        TokenSchema("\\^34", "3!x"),

        // Numer Rachunku IBAN
        TokenSchema("\\^38", "34x"),

        // TokenSchema(":61:", "6n     4n   2a 15d             1!a6!c  6n       20x"),
        // :61:                 230102 0102 CN 000000000100,00 N723112 20230101 51//CEN2301020646240
        // :61:2301060109DN000000000007,00                     NO26NONREF//PSD3643920834580
    )
) {
    internal val schema = schema.map { it.toRegexTokenSchema() }

    data class TokenSchema(val prefix: String, val definition: String) {
        internal fun toRegexTokenSchema() = RegexTokenSchema(prefix, Regex(this.toRegex()))
        private fun toRegex() = "^" + prefix + "(" + definition.replace(Regex("""(\d+)(!?)([nacxd])""")) {
            val exact = it.groups[2]?.value == "!"
            val length = it.groups[1]?.value
            val matchClass = when (it.groups[3]?.value) {
                "n" -> """[\d]"""
                "a" -> """[a-zA-Z]"""
                "c" -> """[\w ]"""
                "x" -> """."""
                "d" -> """[\d,]"""
                else -> throw IllegalStateException("CO JEST KURWA")
            }
            """(${matchClass}){${if (exact) "" else "0,"}${length}}"""
        } + ")" + """((?=\^)|[\r\n]{1,2}|$)"""
    }

    fun tokenize(content: String) =
        generateSequence(Pair(content, listOf<Token>())) { state ->
            when {
                state.first.isEmpty() -> null
                state.first.lineIs("-") -> Pair(state.first.stripLine(), state.second)
                state.first.lineIs("") -> Pair(state.first.stripLine(), state.second)
                else -> matchToken(state.first).let { match -> Pair(match.second, state.second + match.first) }
            }
        }.last().second


    internal fun matchToken(contents: String): Pair<Token, String> {
        val matchingSchema = schema.find { tokenSchema -> tokenSchema.regex.find(contents) != null }
        if (matchingSchema == null) throw IllegalStateException("Not Found Match for line: ${contents.lines().first()}")

        val matched = Token(matchingSchema.prefix, matchingSchema.regex.find(contents)!!.groups[1]!!.value)
        val rest = matchingSchema.regex.replace(contents, "")

        return Pair(matched, rest)
    }

    data class Token(val prefix: String, val content: String)
    internal data class RegexTokenSchema(val prefix: String, val regex: Regex)

    fun String.lineIs(equalTo: String) = this.lines()[0] == equalTo
    fun String.stripLine() = this.replace(this.lines()[0], "").replace(Regex("""^[\r\n]{1,2}"""),"")

}
