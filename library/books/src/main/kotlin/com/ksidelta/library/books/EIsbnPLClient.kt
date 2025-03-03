package com.ksidelta.library.books

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.ksidelta.library.http.HttpClient
import java.util.HashMap

class EIsbnPLClient(val httpClient: HttpClient) : BookClient {

    override fun fetchByIsbn(isbn: String): BookClient.Book? {
        val response = httpClient.get("https://e-isbn.pl/IsbnWeb/api.xml?isbn=${isbn}", ONIXMessage::class.java).data

        if (response.product == null)
            return null

        val title = response.product.descriptiveDetail["TitleDetail"]?.listifyToObj()
            ?.filter { it["TitleType"]!! == "01" }
            ?.map { it["TitleElement"] as Map<String, String> }
            ?.sortedBy { it["TitleElementLevel"] }
            ?.reversed()
            ?.firstOrNull()
            ?.let { it["TitleText"] } ?: "????"

        val author = response.product.descriptiveDetail["Contributor"]?.listify()
            ?.filter { it["ContributorRole"]!!.startsWith("A") }
            ?.map { it["PersonNameInverted"]!! }
            ?.joinToString(", ")
            ?: "???"
        val year = response.product.publishingDetail?.publishingDate?.date?.substring(0, 4) ?: "???"

        return BookClient.Book(isbn, title, author, year)
    }

    fun Any.listify(): List<HashMap<String, String>> =
        when (this) {
            is HashMap<*, *> -> listOf(this as HashMap<String, String>)
            is List<*> -> this as List<HashMap<String, String>>
            else -> emptyList()
        }


    fun Any.listifyToObj(): List<HashMap<String, Object>> =
        when (this) {
            is HashMap<*, *> -> listOf(this as HashMap<String, Object>)
            is List<*> -> this as List<HashMap<String, Object>>
            else -> emptyList()
        }

    data class ONIXMessage(
        @JacksonXmlProperty(localName = "Product") val product: Product?,
    ) {

        data class Product(
            @JacksonXmlProperty(localName = "DescriptiveDetail") val descriptiveDetail: Map<String, Object>,
            @JacksonXmlProperty(localName = "PublishingDetail") val publishingDetail: PublishingDetail,
        )

        data class PublishingDetail(@JacksonXmlProperty(localName = "PublishingDate") val publishingDate: PublishingDate) {
            data class PublishingDate(
                @JacksonXmlProperty(localName = "PublishingDateRole") val publishingDateRole: String,
                @JacksonXmlProperty(localName = "Date") val date: String
            )
        }

    }
}