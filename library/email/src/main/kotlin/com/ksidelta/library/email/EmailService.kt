package com.ksidelta.library.email

interface EmailService {
    fun sendHTML(
        from: String,
        to: String,
        title: String,
        content: String,
        userFriendlyFrom: String?
    )
}