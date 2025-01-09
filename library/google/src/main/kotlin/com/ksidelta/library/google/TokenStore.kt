package com.ksidelta.library.google

interface TokenStore {
    /**
     * @Contract This token will be up to date or missing
     */
    fun getToken(id: String): String?
    fun addToken(id: String, token: AddTokenDto)
    fun ids(): List<String>

    data class AddTokenDto(val token: String, val refreshToken: String?, val expirationSeconds: Int)
}