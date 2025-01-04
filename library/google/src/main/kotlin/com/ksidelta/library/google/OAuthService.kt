package com.ksidelta.library.google

import com.ksidelta.library.logger.Logger
import com.ksidelta.library.store.Store
import java.time.Instant
import java.time.temporal.ChronoUnit

open class OAuthService(val configuration: Configuration, val oAuthClient: OAuthClient) {
    val logger = Logger(OAuthService::class.java)
    fun initiate(redirect: (String) -> Unit, originalUrl: String, offline: Boolean = false) {
        redirect(
            OAuthUrls.authorize(
                configuration.clientId,
                configuration.redirectUrl,
                originalUrl
            )
        )
    }

    fun handleReturn(parameters: Map<String, List<String>>, redirect: (String) -> Unit): StoredToken {
        val code = parameters["code"]?.joinToString() ?: throw IllegalStateException("No Code in return")
        val state = parameters["state"]?.joinToString() ?: throw IllegalStateException("No State in return")
        val returnTo = state.replace("redirectTo:", "")

        return oAuthClient.requestToken(configuration, code)
            .run {
                StoredToken(
                    accessToken = access_token,
                    refreshToken = refresh_token,
                    expiration = Instant.now().plus(expires_in.toLong(), ChronoUnit.SECONDS)
                )
            }
            .apply { logger.log(accessToken) }
            .also { redirect(returnTo) }
    }

    fun ensureFresh(
        storedToken: StoredToken?,
        redirect: (String) -> Unit,
        originalUrl: String,
        offline: Boolean = false
    ): StoredToken? =
        storedToken
            ?.let { if (storedToken.refreshToken == null && offline) null else it }
            ?.also { token ->
                if (token.expiration.fresh())
                    return token
            }
            ?.let { if (storedToken.refreshToken == null) null else it }
            ?.let { token ->
                oAuthClient.refreshToken(configuration, token.refreshToken!!)
                    .run {
                        storedToken.copy(
                            accessToken = access_token,
                            expiration = Instant.now().plus(expires_in.toLong(), ChronoUnit.SECONDS)
                        )
                    }
            } ?: initiate(redirect, originalUrl, offline).let { null }

    data class StoredToken(
        val accessToken: String,
        val refreshToken: String?,
        val expiration: Instant
    ) {}
}

fun Instant.fresh() = this > Instant.now()
