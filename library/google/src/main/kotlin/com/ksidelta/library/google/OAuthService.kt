package com.ksidelta.library.google

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.logger.Logger

class OAuthService(val configuration: Configuration, val client: HttpClient) {
    val logger = Logger(OAuthService::class.java)
    fun initiate(redirect: (String) -> Unit) {
        redirect(
            OAuthUrls.token(
                configuration.clientId,
                configuration.redirectUrl
            )
        )
    }

    fun handleReturn(code: String) =
        configuration
            .run { OAuthUrls.requestToken(code, clientId, clientSecret, redirectUrl) }
            .let { client.post(it, TokenResponse::class.java) }
            .apply { logger.log(access_token) }


    data class TokenResponse(
        val access_token: String,
        val expires_in: Int,
        val id_token: String,
        val scope: String,
        val token_type: String,
        val refresh_token: String?
    )
}
