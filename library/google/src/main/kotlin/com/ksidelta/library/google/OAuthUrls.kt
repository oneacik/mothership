package com.ksidelta.library.google

import com.ksidelta.library.utils.UrlBuilder

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

    fun authorize(
        clientId: String,
        redirectUrl: String,
        originalUrl: String
    ): String =
        UrlBuilder.queryUrl(
            "https://accounts.google.com/o/oauth2/v2/auth",
            mapOf(
                "response_type" to "code",

                "client_id" to clientId,

                "scope" to "openid email profile https://www.googleapis.com/auth/drive",
                "state" to "redirectTo:${originalUrl}",
                "redirect_uri" to redirectUrl,
            )
        )
}