package com.ksidelta.library.google

import com.ksidelta.library.http.UrlBuilder

object OAuthUrls {

    fun requestRefreshToken() {}
    fun requestToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUrl: String
    ) = UrlBuilder.queryUrl(
            "https://oauth2.googleapis.com/token", mapOf(
                "code" to code,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "redirect_uri" to redirectUrl,
                "grant_type" to "authorization_code"
            )
        )

    fun token(
        clientId: String,
        redirectUrl: String
    ): String =
        UrlBuilder.queryUrl(
            "https://accounts.google.com/o/oauth2/v2/auth",
            mapOf(
                "response_type" to "code",

                "client_id" to clientId,

                "scope" to "openid email profile",
                "redirect_uri" to redirectUrl,
            )
        )
}