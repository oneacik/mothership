package com.ksidelta.library.google

import com.ksidelta.library.http.HttpClient
import com.ksidelta.library.books.UrlBuilder

class ProfileClient(val httpClient: HttpClient) {
    fun me(token: String) =
        httpClient.get(
            UrlBuilder.queryUrl(
                "https://openidconnect.googleapis.com/v1/userinfo", mapOf(
                )
            ),
            UserInfo::class.java
        ) { it.bearer(token) }
            .data

    data class UserInfo(
        val sub: String,
        val name: String,
        val email: String
    )
}