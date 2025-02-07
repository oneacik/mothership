package com.ksidelta.library.google

import com.ksidelta.library.books.UrlBuilder

object OAuthUrls {
    fun authorize(
        clientId: String,
        redirectUrl: String,
        originalUrl: String,
        offline: Boolean = true
    ): String =
        UrlBuilder.queryUrl(
            "https://accounts.google.com/o/oauth2/v2/auth",
            mapOf(
                "response_type" to "code",

                "client_id" to clientId,

                "scope" to "openid email profile https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/spreadsheets" ,
                "state" to "redirectTo:${originalUrl}",
                "redirect_uri" to redirectUrl,
                "access_type" to if (offline) "offline" else "online",
            ) + if (offline) mapOf("prompt" to "consent") else emptyMap()
        )
}