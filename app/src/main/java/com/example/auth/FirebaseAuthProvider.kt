package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FirebaseAuthProvider manages Firebase authentication user sessions and credentials.
 */
class FirebaseAuthProvider private constructor(private val context: Context) {

    private val tag = "FirebaseAuthProvider"
    private var firebaseAuth: FirebaseAuth? = null

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    init {
        initializeFirebaseAuth()
    }

    private fun initializeFirebaseAuth() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                firebaseAuth = auth
                _currentUserState.value = auth.currentUser

                auth.addAuthStateListener { updatedAuth ->
                    _currentUserState.value = updatedAuth.currentUser
                    Log.d(tag, "Firebase Auth state changed. User: ${updatedAuth.currentUser?.email}")
                }
            } else {
                Log.w(tag, "FirebaseApp is not initialized in this environment.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize FirebaseAuth", e)
        }
    }

    fun isFirebaseAvailable(): Boolean {
        return firebaseAuth != null && FirebaseApp.getApps(context).isNotEmpty()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth?.currentUser ?: _currentUserState.value
    }

    fun isUserSignedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Guarantees a cryptographically authenticated Firebase Auth session is active
     * before reading/writing to Cloud Firestore.
     * If user is not signed in with Google, acquires an anonymous auth session
     * so `request.auth != null` rules are satisfied across all Firestore endpoints.
     */
    fun ensureAuthSession(onComplete: (success: Boolean) -> Unit = {}) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(false)
            return
        }

        if (auth.currentUser != null) {
            _currentUserState.value = auth.currentUser
            onComplete(true)
            return
        }

        try {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        _currentUserState.value = user
                        Log.d(tag, "Acquired authenticated Firebase session for Firestore: UID=${user?.uid}")
                        onComplete(true)
                    } else {
                        Log.w(tag, "Firebase anonymous session fallback notice: ${task.exception?.message}")
                        onComplete(false)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Exception acquiring Firebase session", e)
            onComplete(false)
        }
    }

    /**
     * Authenticates with Firebase using a Google ID token retrieved from Google Identity / CredentialManager.
     */
    fun signInWithGoogleIdToken(
        idToken: String,
        onResult: (success: Boolean, user: FirebaseUser?, errorMessage: String?) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            Log.w(tag, "FirebaseAuth instance null. Operating in standalone mode.")
            onResult(false, null, "Firebase Auth service unavailable.")
            return
        }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        _currentUserState.value = firebaseUser
                        Log.d(tag, "Successfully authenticated Google user against Firebase: ${firebaseUser?.email}")
                        onResult(true, firebaseUser, null)
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Firebase Google sign-in failed."
                        Log.w(tag, "Firebase Google sign-in notice", task.exception)
                        onResult(false, null, errorMsg)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Exception during Google credential exchange in Firebase notice", e)
            onResult(false, null, e.localizedMessage ?: "An error occurred during Firebase credential exchange.")
        }
    }

    /**
     * Authenticates with Firebase using any generic AuthCredential (Facebook, GitHub, etc.).
     */
    fun signInWithCredential(
        credential: AuthCredential,
        onResult: (success: Boolean, user: FirebaseUser?, errorMessage: String?) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            onResult(false, null, "Firebase Auth service unavailable.")
            return
        }

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    _currentUserState.value = firebaseUser
                    onResult(true, firebaseUser, null)
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Firebase authentication failed."
                    onResult(false, null, errorMsg)
                }
            }
    }

    /**
     * Signs out the user from Firebase Auth session.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
            _currentUserState.value = null
            Log.d(tag, "User signed out from Firebase session.")
        } catch (e: Exception) {
            Log.e(tag, "Error signing out from Firebase", e)
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseAuthProvider? = null

        fun getInstance(context: Context): FirebaseAuthProvider {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthProvider(context.applicationContext).also { instance = it }
            }
        }
    }
}
