import io.ktor.utils.io.charsets.Charset

object HtmlFiles {

    fun createIndex(emails: List<String>) =
        HtmlFiles::class.java.getResourceAsStream("/index.html")!!
            .readAllBytes().toString(Charset.defaultCharset())
            .replace(
                "\$user",
                emails.map {
                    """
                <option value="$it">$it</option>
                """
                }.joinToString("\n")
            )
}