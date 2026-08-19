package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * GoogleAuthManager class using the androidx.credentials library.
 * Implements GetCredentialRequest to trigger native Google Account Picker and uses the returned
 * idToken to authenticate with FirebaseAuth.getInstance() according to Android security best practices.
 */
class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun signInWithGoogle(
        activity: Activity? = null,
        scope: CoroutineScope,
        webClientId: String = DEFAULT_WEB_CLIENT_ID,
        targetRole: String = "customer",
        onResult: (success: Boolean, firebaseUser: FirebaseUser?, email: String?, displayName: String?, photoUrl: String?, errorMessage: String?) -> Unit
    ) {
        Companion.signInWithGoogle(context, activity, scope, webClientId, targetRole, onResult)
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
        const val DEFAULT_WEB_CLIENT_ID = "488422345846-m972okhh2ms29s911apa4t8ih04d3jo1.apps.googleusercontent.com"

        /**
         * Triggers native Google Account Picker via androidx.credentials GetCredentialRequest,
         * extracts idToken from GoogleIdTokenCredential, and authenticates with FirebaseAuth.
         */
        fun signInWithGoogle(
            context: Context,
            activity: Activity? = null,
            scope: CoroutineScope,
            webClientId: String = DEFAULT_WEB_CLIENT_ID,
            targetRole: String = "customer",
            onResult: (success: Boolean, firebaseUser: FirebaseUser?, email: String?, displayName: String?, photoUrl: String?, errorMessage: String?) -> Unit
        ) {
            scope.launch {
                Log.d(TAG, "--> [GoogleAuthFlow] Initiating Google Sign-In with targetRole: $targetRole")
                val act = activity ?: DeviceAccountUtils.findActivity(context)
                if (act != null) {
                    try {
                        Log.d(TAG, "[GoogleAuthFlow] Activity found (${act.localClassName}). Building CredentialManager request...")
                        val credentialManager = CredentialManager.create(act)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        Log.d(TAG, "[GoogleAuthFlow] Requesting Google Credential via CredentialManager...")
                        val result = credentialManager.getCredential(act, request)

                        val credential = result.credential
                        Log.d(TAG, "[GoogleAuthFlow] Received credential type: ${credential::class.java.simpleName}")
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            val realEmail = googleIdTokenCredential.id
                            val realDisplayName = googleIdTokenCredential.displayName ?: realEmail.substringBefore("@")
                            val realPhotoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                            Log.i(TAG, "✅ [GoogleAuthFlow] Extracted Authentic Google Account: $realEmail ($realDisplayName)")

                            // Use AuthManager for robust Firebase token exchange and session management
                            AuthManager.getInstance(context).signInWithGoogleToken(idToken, scope) { syncSuccess, fbUser, syncError ->
                                Log.d(TAG, "[GoogleAuthFlow] AuthManager token exchange complete. Result: $syncSuccess, Error: $syncError")
                            }

                            onResult(
                                true,
                                FirebaseAuth.getInstance().currentUser,
                                realEmail,
                                realDisplayName,
                                realPhotoUrl,
                                null
                            )
                            return@launch
                        } else {
                            Log.w(TAG, "⚠️ [GoogleAuthFlow] Unexpected credential payload: ${credential.type}")
                            onResult(false, null, null, null, null, "Unsupported credential format returned from Google.")
                            return@launch
                        }
                    } catch (e: GetCredentialCancellationException) {
                        Log.i(TAG, "ℹ️ [GoogleAuthFlow] User dismissed Google Sign-In picker dialog.")
                        onResult(false, null, null, null, null, "Google Sign-In was cancelled.")
                        return@launch
                    } catch (e: Throwable) {
                        Log.w(TAG, "⚠️ [GoogleAuthFlow] androidx.credentials GetCredentialRequest notice: ${e.message}", e)
                        // If error occurs, check if authentic Firebase user already logged in
                        val existingUser = FirebaseAuth.getInstance().currentUser
                        if (existingUser != null && !existingUser.email.isNullOrBlank()) {
                            onResult(
                                true,
                                existingUser,
                                existingUser.email,
                                existingUser.displayName ?: existingUser.email!!.substringBefore("@"),
                                existingUser.photoUrl?.toString(),
                                null
                            )
                            return@launch
                        } else {
                            onResult(false, null, null, null, null, e.localizedMessage ?: "Google Sign-In prompt failed.")
                            return@launch
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ [GoogleAuthFlow] No suitable Activity found in context tree.")
                    onResult(false, null, null, null, null, "Unable to find active Activity context for Google Sign-In.")
                    return@launch
                }
            }
        }

        fun signOut(context: Context) {
            AuthManager.getInstance(context).signOut()
            FirebaseAuthProvider.getInstance(context).signOut()
        }

        fun getCurrentUser(): FirebaseUser? {
            return FirebaseAuth.getInstance().currentUser
        }
    }
}


