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
}
