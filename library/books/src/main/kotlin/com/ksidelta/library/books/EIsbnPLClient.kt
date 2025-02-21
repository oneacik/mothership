package com.ksidelta.library.books

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.ksidelta.library.http.HttpClient

class EIsbnPLClient(val httpClient: HttpClient) : BookClient {

    override fun fetchByIsbn(isbn: String): BookClient.Book? {
        val response = httpClient.get("https://e-isbn.pl/IsbnWeb/api.xml?isbn=${isbn}", ONIXMessage::class.java).data

        return null;
    }

    data class ONIXMessage(
        @JacksonXmlProperty(localName = "Product") val product: Product?,

        ) {
        data class Product(
            @JacksonXmlProperty(localName = "DescriptiveDetail") val descriptiveDetail: DescriptiveDetail,
            @JacksonXmlProperty(localName = "PublishingDetail") val publishingDetail: PublishingDetail,
        ) {
            data class DescriptiveDetail(
                @JacksonXmlProperty(localName = "TitleDetail") val titleDetail: TitleDetail,
                @JacksonXmlProperty(localName = "Contributor") val contributor: List<Contributor>,
            ) {
                data class TitleDetail(
                    @JacksonXmlProperty(localName = "TitleType") val titleType: Int,
                    @JacksonXmlProperty(localName = "TitleElement") val titleElement: TitleElement,

                    ) {
                    data class TitleElement(
                        @JacksonXmlProperty(localName = "TitleElementLevel") val titleElementLevel: Int,
                        @JacksonXmlProperty(localName = "TitleText") val titleText: String
                    )
                }

                data class Contributor(
                    @JacksonXmlProperty(localName = "PersonNameInverted") val personNameInverted: String
                ) {}
            }

            data class PublishingDetail(@JacksonXmlProperty(localName = "PublishingDate") val publishingDate: PublishingDate) {
                data class PublishingDate(
                    @JacksonXmlProperty(localName = "PublishingDateRole") val publishingDateRole: String,
                    @JacksonXmlProperty(localName = "Date") val date: String
                ) {}
            }

        }
    }
}