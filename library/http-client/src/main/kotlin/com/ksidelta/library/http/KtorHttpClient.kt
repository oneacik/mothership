package com.ksidelta.library.http

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadCapability
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ksidelta.library.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.serialization.kotlinx.serialization
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.runBlocking

class KtorHttpClient : HttpClient {
    val logger = Logger(KtorHttpClient::class.java)

    val client = io.ktor.client.HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson() {
                jsonFactory.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            }

            val xmlMapper = XmlMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            register(ContentType.Text.Xml, JacksonConverter(xmlMapper))
            register(ContentType.Application.Xml, JacksonConverter(xmlMapper))
        }
    }

    override fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO): HttpClient.Response<T> =
        runBlocking {
            client.get(url) {
                configure(DTO()).headers.entries.forEach { header ->
                    this.headers.append(header.key, header.value)
                }
            }.handleResponse(klass)
        }


    override fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO): HttpClient.Response<T> =
        runBlocking {
            client.post(url) {
                configure(DTO()).headers.entries.forEach { header ->
                    this.headers.append(header.key, header.value)
                }
                configure(DTO()).body?.let { setBody(it) }
            }.handleResponse(klass)
        }

    private suspend fun <T : Any> HttpResponse.handleResponse(klass: Class<T>): HttpClient.Response<T> =
        this
            .apply { logger.log("Response Status: {}", status) }
            .let { response ->
                runCatching {
                    if (response.status.value >= 400) {
                        logger.log(response.body<String>())
                    }

                    HttpClient.Response(
                        response.body<T>(TypeInfo(klass.kotlin, null)),
                        response.status.value
                    )
                }.onFailure {
                    logger.log(response.body<String>())
                }.getOrThrow()
            }


    class XmlContentConverter : ContentConverter {
        override suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any?): OutgoingContent? {
            TODO("Not yet implemented")
        }

        override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
            TODO("Not yet implemented")
        }
    }
}

