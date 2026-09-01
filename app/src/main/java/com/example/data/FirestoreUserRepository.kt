package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(private val context: Context) {

    private val TAG = "FirestoreUserRepository"

    private val db: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
                null
            }
        }

    /**
     * Saves or updates a user profile document in Firestore at path `users/{uid}`.
     */
    suspend fun saveOrUpdateUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        role: String = "customer"
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                val docId = if (uid.isNotBlank()) uid else email.replace(".", "_")
                val docRef = firestore.collection("users").document(docId)

                val userMap = mutableMapOf<String, Any?>(
                    "uid" to docId,
                    "email" to email,
                    "displayName" to displayName,
                    "photoURL" to photoUrl,
                    "role" to role,
                    "lastLogin" to System.currentTimeMillis()
                )

                docRef.set(
                    mapOf("createdAt" to System.currentTimeMillis()) + userMap,
                    SetOptions.merge()
                ).await()
            }

            Log.d(TAG, "User document upserted successfully in Firestore at users/$uid")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error upserting user in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Atomically increments the app download/install counter in Firestore at `app_stats/downloads`.
     * Called once per unique device install (guarded by SharedPreferences in MainViewModel).
     */
    suspend fun incrementAppDownloadCount(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000L) {
                val docRef = firestore.collection("app_stats").document("downloads")
                docRef.set(
                    mapOf("count" to FieldValue.increment(1), "lastUpdated" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
            }
            Log.d(TAG, "App download counter incremented in Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error incrementing app download counter: ${e.message}")
            false
        }
    }

    /**
     * Reads the current app download/install count from Firestore at `app_stats/downloads`.
     * Returns 0 if the document doesn't exist yet.
     */
    suspend fun getAppDownloadCount(): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext 0L
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                firestore.collection("app_stats").document("downloads").get().await()
            }
            val count = snapshot?.getLong("count") ?: 0L
            Log.d(TAG, "App download count fetched from Firestore: $count")
            count
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching app download count: ${e.message}")
            0L
        }
    }

    /**
     * Saves the SMTP & Email Gateway settings to Firestore at `system_config/email_gateway`
     * so all devices (User, Rider, Admin) can seamlessly access active email dispatch configuration.
     */
    suspend fun saveEmailGatewayConfig(config: com.example.security.SmtpConfig): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                val docRef = firestore.collection("system_config").document("email_gateway")
                val data = mapOf(
                    "host" to config.host,
                    "port" to config.port,
                    "senderEmail" to config.senderEmail,
                    "appPassword" to config.appPassword,
                    "senderName" to config.senderName,
                    "webhookUrl" to config.webhookUrl,
                    "isEnabled" to config.isEnabled,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
            }
            Log.d(TAG, "Email gateway config synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error saving email gateway config to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches current Email Gateway settings from Firestore.
     */
    suspend fun getEmailGatewayConfig(): com.example.security.SmtpConfig? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext null
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                firestore.collection("system_config").document("email_gateway").get().await()
            }
            if (snapshot != null && snapshot.exists()) {
                val host = snapshot.getString("host") ?: "smtp.gmail.com"
                val port = snapshot.getLong("port")?.toInt() ?: 465
                val senderEmail = snapshot.getString("senderEmail") ?: "m.daniyalkhan490@gmail.com"
                val appPassword = snapshot.getString("appPassword")?.takeIf { it.isNotBlank() } ?: "pkymsolzualgbgzn"
                val senderName = snapshot.getString("senderName") ?: "Zyphuel Delivery Operations"
                val webhookUrl = snapshot.getString("webhookUrl") ?: ""
                val isEnabled = snapshot.getBoolean("isEnabled") ?: true
                com.example.security.SmtpConfig(
                    host = host,
                    port = port,
                    senderEmail = senderEmail,
                    appPassword = appPassword,
                    senderName = senderName,
                    webhookUrl = webhookUrl,
                    isEnabled = isEnabled
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching email gateway config from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Listens in real-time to Email Gateway configuration updates from Firestore.
     */
    fun observeEmailGatewayConfig(
        onConfigChanged: (com.example.security.SmtpConfig) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            firestore.collection("system_config").document("email_gateway")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for email gateway config: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val host = snapshot.getString("host") ?: "smtp.gmail.com"
                        val port = snapshot.getLong("port")?.toInt() ?: 465
                        val senderEmail = snapshot.getString("senderEmail") ?: "m.daniyalkhan490@gmail.com"
                        val appPassword = snapshot.getString("appPassword")?.takeIf { it.isNotBlank() } ?: "pkymsolzualgbgzn"
                        val senderName = snapshot.getString("senderName") ?: "Zyphuel Delivery Operations"
                        val webhookUrl = snapshot.getString("webhookUrl") ?: ""
                        val isEnabled = snapshot.getBoolean("isEnabled") ?: true
                        onConfigChanged(
                            com.example.security.SmtpConfig(
                                host = host,
                                port = port,
                                senderEmail = senderEmail,
                                appPassword = appPassword,
                                senderName = senderName,
                                webhookUrl = webhookUrl,
                                isEnabled = isEnabled
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up email gateway listener: ${e.message}")
            null
        }
    }
}
