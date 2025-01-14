package com.ksidelta.library.google

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.utils.UrlBuilder

class OAuthClient(val client: HttpClient) {
    fun requestToken(configuration: Configuration, code: String) =
        code
            .let { code ->
                configuration.run {
                    UrlBuilder.queryUrl(
                        "https://oauth2.googleapis.com/token", mapOf(
                            "code" to code,
                            "client_id" to clientId,
                            "client_secret" to clientSecret,
                            "redirect_uri" to redirectUrl,
                            "grant_type" to "authorization_code"
                        )
                    )
                }
            }
            .let { client.post(it, TokenResponse::class.java) }

    fun refreshToken(configuration: Configuration, refreshToken: String) =
        client.post(
            UrlBuilder.queryUrl(
                "https://oauth2.googleapis.com/token", mapOf(
                    "client_id" to configuration.clientId,
                    "client_secret" to configuration.clientSecret,
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken
                )
            ),
            TokenResponse::class.java
        )


    data class TokenResponse(
        val access_token: String,
        val expires_in: Int,
        val id_token: String,
        val scope: String,
        val token_type: String,
        val refresh_token: String?
    )
}