package com.ksidelta.library.email

import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.util.Properties

class SendGridEmailService(
    val apiKey: String
) : EmailService {
    override fun sendHTML(from: String, to: String, subject: String, content: String) {
        val properties = Properties()
        properties.put("mail.host", "smtp.sendgrid.net")
        properties.put("mail.smtp.auth", "true")
        properties.put("mail.smtp.starttls.enable", "true");

        val session = Session.getInstance(properties)

        val message = MimeMessage(session).apply {
            setSubject(subject)
            setContent(
                MimeMultipart().apply {
                    addBodyPart(
                        MimeBodyPart().apply {
                            setContent(content, "text/html; charset=utf-8")
                        }
                    )
                }
            )
        }

        message.setFrom(from);

        Transport.send(
            message,
            arrayOf(InternetAddress(to)),
            "apikey", apiKey
        )
    }
}