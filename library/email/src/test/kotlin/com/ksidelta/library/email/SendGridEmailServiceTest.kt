package com.ksidelta.library.email

import kotlin.test.Test

class SendGridEmailServiceTest {

    @Test
    fun sendMail() {
        val service = SendGridEmailService(
            System.getenv("MOTHERSHIP_SENDGRID_APIKEY")
                ?: throw IllegalStateException("GDZIE JEST SENDGRID")
        )
        service.sendHTML("no-reply@libruch.hsp.sh", "piotr.suwala@respublica.org.pl", "EEE", "TEST")
    }
}