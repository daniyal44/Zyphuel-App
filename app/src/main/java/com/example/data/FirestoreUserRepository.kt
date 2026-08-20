package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
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
}

