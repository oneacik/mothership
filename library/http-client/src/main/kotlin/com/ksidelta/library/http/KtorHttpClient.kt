package com.ksidelta.library.http

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.ksidelta.library.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.jackson.jackson
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking

class KtorHttpClient : HttpClient {
    val logger = Logger(KtorHttpClient::class.java)

    val client = io.ktor.client.HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson() {
                jsonFactory.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            }
        }
    }

    override fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO): T =
        runBlocking {
            client.get(url) {
                configure(DTO()).headers.entries.forEach { header ->
                    this.headers.append(header.key, header.value)
                }
            }.handleResponse(klass)
        }


    override fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO): T =
        runBlocking {
            client.post(url) {
               configure(DTO()).headers.entries.forEach { header ->
                    this.headers.append(header.key, header.value)
                }
            }.handleResponse(klass)
        }

    private suspend fun <T : Any> HttpResponse.handleResponse(klass: Class<T>): T =
        this
            .apply { logger.log("Response Status: {}", status) }
            .let { response ->
                runCatching {
                    response.body<T>(TypeInfo(klass.kotlin, null))
                }.onFailure {
                    logger.log(response.body<String>())
                }.getOrThrow()
            }

}

