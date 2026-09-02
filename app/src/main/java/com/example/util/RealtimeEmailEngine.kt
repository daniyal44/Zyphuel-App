package com.example.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.security.SecureStorageManager
import com.example.security.SmtpConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Result data class for email delivery attempts, providing transparent diagnostic feedback.
 */
data class EmailDeliveryResult(
    val isSuccess: Boolean,
    val channel: String,
    val message: String
)

/**
 * Production-grade Real-Time Email Delivery Engine for Zyphuel.
 *
 * Provides real-time email dispatch directly to user, rider, and admin Gmail inboxes using:
 * 1. Authenticated Direct TLS/SSL SMTP Protocol Client (RFC 5321 / RFC 5322) with AUTH LOGIN.
 * 2. High-speed Cloud HTTPS Webhook Relay (Google Apps Script / Cloud Functions) over Port 443.
 * 3. Cloud Firestore Trigger Email collection (`mail`) for server-side delivery.
 * 4. Structured fallback and live diagnostic reporting.
 */
object RealtimeEmailEngine {
    private const val TAG = "RealtimeEmailEngine"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Backward-compatible simple email dispatch method.
     */
    suspend fun sendRealtimeEmail(
        recipientEmail: String,
        subject: String,
        bodyText: String,
        htmlBody: String? = null,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val result = sendRealtimeEmailDetailed(recipientEmail, subject, bodyText, htmlBody, context)
        return@withContext result.isSuccess
    }

    /**
     * Dispatches an actual real-time email in background coroutine to the recipient's Gmail inbox
     * and returns detailed diagnostic status.
     */
    suspend fun sendRealtimeEmailDetailed(
        recipientEmail: String,
        subject: String,
        bodyText: String,
        htmlBody: String? = null,
        context: Context? = null,
        customConfig: SmtpConfig? = null
    ): EmailDeliveryResult = withContext(Dispatchers.IO) {
        val trimmedRecipient = recipientEmail.trim()
        if (trimmedRecipient.isBlank() || !trimmedRecipient.contains("@")) {
            Log.w(TAG, "Invalid recipient email address: $trimmedRecipient")
            return@withContext EmailDeliveryResult(
                isSuccess = false,
                channel = "Validation",
                message = "Invalid recipient email address: $trimmedRecipient"
            )
        }

        // Resolve active SMTP / Gateway Configuration with reliable defaults
        val rawConfig = customConfig ?: if (context != null) {
            SecureStorageManager.getSmtpConfig(context)
        } else {
            SmtpConfig()
        }
        val config = rawConfig.copy(
            host = rawConfig.host.ifBlank { "smtp.gmail.com" },
            port = if (rawConfig.port > 0) rawConfig.port else 465,
            senderEmail = rawConfig.senderEmail.ifBlank { "m.daniyalkhan490@gmail.com" },
            appPassword = rawConfig.appPassword.replace(" ", "").trim().ifBlank { "pkymsolzualgbgzn" },
            senderName = rawConfig.senderName.ifBlank { "Zyphuel Delivery Operations" }
        )

        val errors = mutableListOf<String>()

        // =========================================================================
        // CHANNEL 1: Authenticated Direct SMTP Client (RFC 5321 / RFC 5322)
        // =========================================================================
        if (config.isEnabled && config.appPassword.isNotBlank()) {
            try {
                val smtpResult = dispatchViaAuthenticatedSmtp(
                    recipientEmail = trimmedRecipient,
                    subject = subject,
                    bodyText = bodyText,
                    htmlBody = htmlBody,
                    config = config
                )
                if (smtpResult.isSuccess) {
                    Log.i(TAG, "✅ [Channel 1 - Authenticated SMTP] Email delivered to: $trimmedRecipient")
                    return@withContext smtpResult
                } else {
                    errors.add("Authenticated SMTP: ${smtpResult.message}")
                }
            } catch (e: Exception) {
                val err = "Authenticated SMTP Exception: ${e.localizedMessage ?: e.message}"
                Log.w(TAG, err)
                errors.add(err)
            }
        } else if (config.isEnabled && config.appPassword.isBlank()) {
            Log.d(TAG, "SMTP App Password is not yet configured in Admin settings.")
        }

        // =========================================================================
        // CHANNEL 2: Cloud HTTPS Webhook Relay (Google Apps Script / Webhook API)
        // =========================================================================
        if (config.webhookUrl.isNotBlank() && config.webhookUrl.startsWith("http")) {
            try {
                val webhookResult = dispatchViaWebhookRelay(
                    webhookUrl = config.webhookUrl,
                    recipientEmail = trimmedRecipient,
                    subject = subject,
                    bodyText = bodyText,
                    htmlBody = htmlBody,
                    senderName = config.senderName
                )
                if (webhookResult.isSuccess) {
                    Log.i(TAG, "✅ [Channel 2 - HTTPS Webhook Relay] Email delivered to: $trimmedRecipient")
                    return@withContext webhookResult
                } else {
                    errors.add("HTTPS Webhook: ${webhookResult.message}")
                }
            } catch (e: Exception) {
                val err = "Webhook Relay Exception: ${e.localizedMessage ?: e.message}"
                Log.w(TAG, err)
                errors.add(err)
            }
        }

        // =========================================================================
        // CHANNEL 3: Cloud Firestore `mail` Collection (Firebase Trigger Email)
        // =========================================================================
        try {
            val firestore = FirebaseFirestore.getInstance()
            val mailDoc = hashMapOf(
                "to" to listOf(trimmedRecipient),
                "message" to hashMapOf(
                    "subject" to subject,
                    "text" to bodyText,
                    "html" to (htmlBody ?: generateHtmlEmail(subject, bodyText))
                ),
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "deliveryStatus" to "PENDING",
                "recipient" to trimmedRecipient
            )
            firestore.collection("mail").add(mailDoc)
            Log.i(TAG, "ℹ️ [Channel 3 - Firestore Mail] Queued in Firestore 'mail' collection for: $trimmedRecipient")
        } catch (e: Exception) {
            Log.d(TAG, "Channel 3 (Firestore Mail) fallback: ${e.message}")
        }

        // =========================================================================
        // Fallback / Diagnostic State
        // =========================================================================
        if (config.appPassword.isBlank() && config.webhookUrl.isBlank()) {
            val diagnostic = "SMTP App Password or Webhook URL not configured. Please set Google App Password in Admin Mailbox settings."
            Log.w(TAG, diagnostic)
            return@withContext EmailDeliveryResult(
                isSuccess = false,
                channel = "Configuration Required",
                message = diagnostic
            )
        }

        val combinedErrors = if (errors.isEmpty()) "Delivery attempt failed on all channels" else errors.joinToString(" | ")
        return@withContext EmailDeliveryResult(
            isSuccess = false,
            channel = "All Channels",
            message = combinedErrors
        )
    }

    /**
     * Executes RFC 5321 / RFC 3207 compliant SMTP protocol session over SSL (port 465) or STARTTLS (port 587).
     */
    private fun dispatchViaAuthenticatedSmtp(
        recipientEmail: String,
        subject: String,
        bodyText: String,
        htmlBody: String?,
        config: SmtpConfig
    ): EmailDeliveryResult {
        val host = config.host.ifBlank { "smtp.gmail.com" }
        val port = if (config.port > 0) config.port else 465
        val senderEmail = config.senderEmail.ifBlank { "m.daniyalkhan490@gmail.com" }
        val senderName = config.senderName.ifBlank { "Zyphuel Delivery Operations" }
        val appPassword = config.appPassword.replace(" ", "").trim().ifBlank { "pkymsolzualgbgzn" }

        var socket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            fun readResponse(): String {
                val sb = StringBuilder()
                var line = reader?.readLine() ?: ""
                sb.append(line)
                Log.d(TAG, "SMTP Server <<< $line")
                while (line.length >= 4 && line[3] == '-') {
                    line = reader?.readLine() ?: ""
                    sb.append("\n").append(line)
                    Log.d(TAG, "SMTP Server <<< $line")
                }
                return sb.toString()
            }

            fun sendCommand(cmd: String, maskLog: Boolean = false) {
                if (maskLog) {
                    Log.d(TAG, "SMTP Client >>> [AUTHENTICATION CREDENTIAL]")
                } else {
                    Log.d(TAG, "SMTP Client >>> $cmd")
                }
                writer?.print("$cmd\r\n")
                writer?.flush()
            }

            if (port == 465) {
                // Direct SSL connection (Port 465) with explicit SNI (Server Name Indication)
                val plainSocket = Socket()
                plainSocket.connect(InetSocketAddress(host, port), 12000)
                plainSocket.soTimeout = 15000
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(plainSocket, host, port, true) as SSLSocket
                sslSocket.soTimeout = 15000
                sslSocket.startHandshake()
                socket = sslSocket
                reader = BufferedReader(InputStreamReader(sslSocket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream(), Charsets.UTF_8), true)

                // 1. Initial 220 greeting
                val greeting = readResponse()
                if (!greeting.startsWith("220")) {
                    return EmailDeliveryResult(false, "SMTP", "Server greeting failed: $greeting")
                }

                // 2. EHLO
                sendCommand("EHLO zyphuel.app")
                val ehloResp = readResponse()
                if (!ehloResp.contains("250")) {
                    return EmailDeliveryResult(false, "SMTP", "EHLO rejected: $ehloResp")
                }
            } else {
                // Plain socket with RFC 3207 STARTTLS upgrade (e.g. Port 587)
                val plainSocket = Socket()
                plainSocket.connect(InetSocketAddress(host, port), 12000)
                plainSocket.soTimeout = 15000
                socket = plainSocket
                reader = BufferedReader(InputStreamReader(plainSocket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(plainSocket.getOutputStream(), Charsets.UTF_8), true)

                // 1. Initial 220 greeting
                val greeting = readResponse()
                if (!greeting.startsWith("220")) {
                    return EmailDeliveryResult(false, "SMTP", "Server greeting failed: $greeting")
                }

                // 2. Initial EHLO
                sendCommand("EHLO zyphuel.app")
                val ehloResp = readResponse()
                if (!ehloResp.contains("250")) {
                    return EmailDeliveryResult(false, "SMTP", "Initial EHLO rejected: $ehloResp")
                }

                // 3. Issue STARTTLS
                sendCommand("STARTTLS")
                val tlsResp = readResponse()
                if (!tlsResp.startsWith("220")) {
                    return EmailDeliveryResult(false, "SMTP", "STARTTLS command rejected: $tlsResp")
                }

                // 4. Upgrade plain socket to SSLSocket
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(plainSocket, host, port, true) as SSLSocket
                sslSocket.soTimeout = 15000
                sslSocket.startHandshake()
                socket = sslSocket
                reader = BufferedReader(InputStreamReader(sslSocket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream(), Charsets.UTF_8), true)

                // 5. Re-issue EHLO over encrypted TLS session
                sendCommand("EHLO zyphuel.app")
                val ehloTlsResp = readResponse()
                if (!ehloTlsResp.contains("250")) {
                    return EmailDeliveryResult(false, "SMTP", "EHLO after TLS rejected: $ehloTlsResp")
                }
            }

            // 3. AUTH LOGIN
            sendCommand("AUTH LOGIN")
            val authResp = readResponse()
            if (!authResp.startsWith("334")) {
                return EmailDeliveryResult(false, "SMTP", "AUTH LOGIN unsupported or rejected: $authResp")
            }

            // Send base64 username
            val userBase64 = Base64.encodeToString(senderEmail.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(userBase64, maskLog = true)
            val userResp = readResponse()
            if (!userResp.startsWith("334")) {
                return EmailDeliveryResult(false, "SMTP", "Username rejected: $userResp")
            }

            // Send base64 app password
            val passBase64 = Base64.encodeToString(appPassword.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(passBase64, maskLog = true)
            val passResp = readResponse()
            if (!passResp.startsWith("235")) {
                val errorMsg = if (passResp.contains("535")) {
                    "Google SMTP 535: Invalid App Password. Please generate a 16-letter App Password in Google Account Security."
                } else {
                    "Authentication failed: $passResp"
                }
                return EmailDeliveryResult(false, "SMTP", errorMsg)
            }

            // 4. MAIL FROM
            sendCommand("MAIL FROM:<$senderEmail>")
            val mailFromResp = readResponse()
            if (!mailFromResp.startsWith("250")) {
                return EmailDeliveryResult(false, "SMTP", "MAIL FROM rejected: $mailFromResp")
            }

            // 5. RCPT TO
            sendCommand("RCPT TO:<$recipientEmail>")
            val rcptToResp = readResponse()
            if (!rcptToResp.startsWith("250")) {
                return EmailDeliveryResult(false, "SMTP", "RCPT TO rejected for $recipientEmail: $rcptToResp")
            }

            // 6. DATA
            sendCommand("DATA")
            val dataResp = readResponse()
            if (!dataResp.startsWith("354")) {
                return EmailDeliveryResult(false, "SMTP", "DATA command rejected: $dataResp")
            }

            // 7. MIME Headers & Content
            val dateHeader = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val messageId = "<${System.currentTimeMillis()}.${UUID.randomUUID()}@zyphuel.app>"
            val finalHtml = htmlBody ?: generateHtmlEmail(subject, bodyText)

            writer?.print("From: \"$senderName\" <$senderEmail>\r\n")
            writer?.print("To: <$recipientEmail>\r\n")
            writer?.print("Subject: $subject\r\n")
            writer?.print("Date: $dateHeader\r\n")
            writer?.print("Message-ID: $messageId\r\n")
            writer?.print("MIME-Version: 1.0\r\n")
            writer?.print("Content-Type: text/html; charset=UTF-8\r\n")
            writer?.print("Content-Transfer-Encoding: 8bit\r\n")
            writer?.print("\r\n")

            // RFC 5321 dot-stuffing and CRLF line formatting
            val normalizedHtml = finalHtml.replace("\r\n", "\n").replace("\r", "\n")
            val lines = normalizedHtml.split("\n")
            for (line in lines) {
                if (line.startsWith(".")) {
                    writer?.print(".$line\r\n")
                } else {
                    writer?.print("$line\r\n")
                }
            }
            writer?.print(".\r\n")
            writer?.flush()

            // 8. DATA acknowledgment
            val dataAck = readResponse()
            if (!dataAck.startsWith("250")) {
                return EmailDeliveryResult(false, "SMTP", "Message rejected after DATA: $dataAck")
            }

            // 9. QUIT
            try {
                sendCommand("QUIT")
                readResponse()
            } catch (_: Exception) {}

            return EmailDeliveryResult(
                isSuccess = true,
                channel = "Authenticated SMTP (${host}:$port)",
                message = "Delivered directly to $recipientEmail (Server Ack: ${dataAck.trim()})"
            )
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: e.message ?: "Socket connection error"
            return EmailDeliveryResult(false, "SMTP", "Connection error to $host:$port - $errorMsg")
        } finally {
            try { writer?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Dispatches transactional email via Google Apps Script Web App / HTTPS Webhook over Port 443.
     */
    private fun dispatchViaWebhookRelay(
        webhookUrl: String,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        htmlBody: String?,
        senderName: String
    ): EmailDeliveryResult {
        try {
            val payload = JSONObject().apply {
                put("to", recipientEmail)
                put("subject", subject)
                put("text", bodyText)
                put("html", htmlBody ?: generateHtmlEmail(subject, bodyText))
                put("senderName", senderName)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    return EmailDeliveryResult(
                        isSuccess = true,
                        channel = "Cloud HTTPS Webhook",
                        message = "Delivered to $recipientEmail (Webhook Status ${response.code})"
                    )
                } else {
                    return EmailDeliveryResult(
                        isSuccess = false,
                        channel = "Cloud HTTPS Webhook",
                        message = "Webhook returned HTTP ${response.code}: $respString"
                    )
                }
            }
        } catch (e: Exception) {
            return EmailDeliveryResult(
                isSuccess = false,
                channel = "Cloud HTTPS Webhook",
                message = e.localizedMessage ?: e.message ?: "Webhook network error"
            )
        }
    }

    /**
     * Generates a modern, responsive HTML email template for Zyphuel notifications.
     */
    fun generateHtmlEmail(subject: String, bodyText: String): String {
        val formattedBody = bodyText
            .replace("\n", "<br/>")
            .replace("━━━━━━━━━━━━━━━━━━━━━━━━━", "<hr style='border:none; border-top:1px dashed #CBD5E1; margin:14px 0;'/>")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>$subject</title>
                <style>
                    body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; margin: 0; padding: 20px; color: #1E293B; }
                    .email-container { max-width: 580px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06); border: 1px solid #E2E8F0; }
                    .email-header { background: linear-gradient(135deg, #0284C7 0%, #0369A1 100%); padding: 24px; text-align: center; color: #FFFFFF; }
                    .email-header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                    .email-body { padding: 24px; font-size: 15px; line-height: 1.6; color: #334155; }
                    .email-footer { background: #F1F5F9; padding: 16px; text-align: center; font-size: 12px; color: #64748B; border-top: 1px solid #E2E8F0; }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="email-header">
                        <h1>🚚 Zyphuel On-Demand Delivery</h1>
                        <p style="margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;">Official Transaction & Order Notification</p>
                    </div>
                    <div class="email-body">
                        $formattedBody
                    </div>
                    <div class="email-footer">
                        <p style="margin: 0;"><b>Zyphuel Lahore Central Headquarters</b> • Green Town, Lahore, Pakistan</p>
                        <p style="margin: 4px 0 0 0;">Customer Support Hotline: +92 323 0112464 • Email: m.daniyalkhan490@gmail.com</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Provides ready-to-use Google Apps Script code for zero-credential serverless Gmail relay.
     */
    fun getGoogleAppsScriptTemplate(): String {
        return """
            function doPost(e) {
              return handleEmailRequest(e);
            }

            function doGet(e) {
              return handleEmailRequest(e);
            }

            function handleEmailRequest(e) {
              try {
                var data = {};
                if (e && e.postData && e.postData.contents) {
                  try {
                    data = JSON.parse(e.postData.contents);
                  } catch (parseErr) {
                    data = e.parameter || {};
                  }
                } else if (e && e.parameter) {
                  data = e.parameter;
                }

                var recipient = data.to;
                var subject = data.subject || "Zyphuel Notification";
                var body = data.text || "";
                var html = data.html || body;
                var senderName = data.senderName || "Zyphuel Delivery Operations";

                if (!recipient) {
                  return ContentService.createTextOutput(JSON.stringify({
                    success: false,
                    error: "Recipient email parameter 'to' is missing"
                  })).setMimeType(ContentService.MimeType.JSON);
                }

                MailApp.sendEmail({
                  to: recipient,
                  subject: subject,
                  body: body,
                  htmlBody: html,
                  name: senderName
                });

                return ContentService.createTextOutput(JSON.stringify({
                  success: true,
                  message: "Delivered to " + recipient
                })).setMimeType(ContentService.MimeType.JSON);
              } catch (error) {
                return ContentService.createTextOutput(JSON.stringify({
                  success: false,
                  error: error.toString()
                })).setMimeType(ContentService.MimeType.JSON);
              }
            }
        """.trimIndent()
    }
}
