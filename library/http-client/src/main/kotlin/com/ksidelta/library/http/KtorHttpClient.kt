package com.ksidelta.library.http

import com.ksidelta.library.logger.Logger
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking

class KtorHttpClient : HttpClient {
    val logger = Logger(KtorHttpClient::class.java)

    val client = io.ktor.client.HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    override fun <T : Any> get(url: String, klass: Class<T>): T =
        runBlocking {
            client.get(url) {
                accept(ContentType.parse("application/json"))
                contentType(ContentType.parse("application/json"))
            }
                .apply { logger.log("Response Status: {}", status) }
                .run { body<T>(TypeInfo(klass.kotlin, null)) }
                .apply { logger.log(this.toString()) }
        }


    override fun <T : Any> post(url: String, klass: Class<T>): T =
        runBlocking {
            client.post(url) {
                accept(ContentType.parse("application/json"))
                contentType(ContentType.parse("application/json"))
            }
                .apply { logger.log("Response Status: {}", status) }
                .run { body<T>(TypeInfo(klass.kotlin, null)) }
                .apply { logger.log(this.toString()) }
        }
}

