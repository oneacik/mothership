package com.ksidelta.library.google

class ProfileService(val profileClient: ProfileClient) {
    fun me(token: String) =
        profileClient.me(token)
            .let {
                it.run {
                    UserInfo(id = sub, name = name, email = email)
                }
            }

    data class UserInfo(
        val id: String,
        val name: String,
        val email: String,
    )
}