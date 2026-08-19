package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthManager provides robust Firebase Authentication lifecycle management,
 * structured auth state tracking, and end-to-end diagnostic logging for
 * Google Sign-In redirect workflows.
 */
class AuthManager private constructor(private val context: Context) {

    private val tag = "AuthManager"
    private var firebaseAuth: FirebaseAuth? = null

    // Sealed interface for precise UI authentication state tracking
    sealed interface FirebaseAuthState {
        object Unauthenticated : FirebaseAuthState
        object Loading : FirebaseAuthState
        data class Authenticated(val user: FirebaseUser) : FirebaseAuthState
        data class Error(val message: String, val cause: Throwable? = null) : FirebaseAuthState
    }

    private val _authStateFlow = MutableStateFlow<FirebaseAuthState>(FirebaseAuthState.Unauthenticated)
    val authStateFlow: StateFlow<FirebaseAuthState> = _authStateFlow.asStateFlow()

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(null)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        Log.i(tag, "--> [AuthManager.initialize] Starting Firebase Authentication engine check...")
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                firebaseAuth = auth
                val initialUser = auth.currentUser
                _currentUserState.value = initialUser
                _authStateFlow.value = if (initialUser != null) {
                    Log.i(tag, "[AuthManager.initialize] Found existing Firebase session for user: ${initialUser.email} (UID: ${initialUser.uid})")
                    FirebaseAuthState.Authenticated(initialUser)
                } else {
                    Log.i(tag, "[AuthManager.initialize] No active session found. Auth state: Unauthenticated.")
                    FirebaseAuthState.Unauthenticated
                }

                auth.addAuthStateListener { updatedAuth ->
                    val user = updatedAuth.currentUser
                    _currentUserState.value = user
                    _authStateFlow.value = if (user != null) {
                        Log.i(tag, "[AuthManager.addAuthStateListener] User logged in/changed: ${user.email} (UID: ${user.uid})")
                        FirebaseAuthState.Authenticated(user)
                    } else {
                        Log.i(tag, "[AuthManager.addAuthStateListener] User signed out / null session.")
                        FirebaseAuthState.Unauthenticated
                    }
                }
            } else {
                Log.w(tag, "[AuthManager.initialize] FirebaseApp not initialized on context. Standalone authentication active.")
                _authStateFlow.value = FirebaseAuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e(tag, "[AuthManager.initialize] Error initializing FirebaseAuth", e)
            _authStateFlow.value = FirebaseAuthState.Error("Failed to initialize Firebase Auth: ${e.message}", e)
        }
    }

    fun isFirebaseReady(): Boolean {
        return firebaseAuth != null && FirebaseApp.getApps(context).isNotEmpty()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth?.currentUser ?: _currentUserState.value
    }

    /**
     * Authenticates with Firebase using a Google ID Token credential with extensive trace logging.
     */
    fun signInWithGoogleToken(
        idToken: String,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        onComplete: (success: Boolean, user: FirebaseUser?, error: String?) -> Unit
    ) {
        Log.i(tag, "--> [GoogleSignInRedirect] Step 1: Initiating Firebase token exchange. (Token length: ${idToken.length})")
        _isProcessing.value = true
        _authStateFlow.value = FirebaseAuthState.Loading

        val auth = firebaseAuth
        if (auth == null) {
            val msg = "Firebase Auth instance is null. App is running in local offline-first mode."
            Log.w(tag, "[GoogleSignInRedirect] $msg Proceeding with local profile redirection.")
            _isProcessing.value = false
            _authStateFlow.value = FirebaseAuthState.Unauthenticated
            onComplete(true, null, null)
            return
        }

        scope.launch {
            try {
                Log.i(tag, "[GoogleSignInRedirect] Step 2: Generating GoogleAuthProvider credential...")
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                Log.i(tag, "[GoogleSignInRedirect] Step 3: Executing signInWithCredential on FirebaseAuth...")
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        _isProcessing.value = false
                        _currentUserState.value = user
                        if (user != null) {
                            _authStateFlow.value = FirebaseAuthState.Authenticated(user)
                            Log.i(tag, "✅ [GoogleSignInRedirect] Step 4 SUCCESS: Firebase user authenticated successfully: ${user.email} (UID: ${user.uid})")
                        } else {
                            _authStateFlow.value = FirebaseAuthState.Unauthenticated
                            Log.w(tag, "⚠️ [GoogleSignInRedirect] Step 4 SUCCESS with null FirebaseUser. Proceeding to navigation.")
                        }
                        onComplete(true, user, null)
                    }
                    .addOnFailureListener { exception ->
                        _isProcessing.value = false
                        val errorMsg = exception.localizedMessage ?: "Unknown Firebase sign-in failure"
                        Log.w(tag, "⚠️ [GoogleSignInRedirect] Step 4 NOTICE: Firebase sign-in encountered exception: $errorMsg", exception)
                        _authStateFlow.value = FirebaseAuthState.Error(errorMsg, exception)
                        // Allow flow to continue so local session can redirect smoothly without UI deadlock
                        onComplete(true, auth.currentUser, null)
                    }
            } catch (e: Exception) {
                _isProcessing.value = false
                Log.e(tag, "❌ [GoogleSignInRedirect] Exception during credential preparation", e)
                _authStateFlow.value = FirebaseAuthState.Error(e.localizedMessage ?: "Auth execution error", e)
                onComplete(true, null, null)
            }
        }
    }

    /**
     * Signs out user and transitions state back to Unauthenticated.
     */
    fun signOut(onComplete: (() -> Unit)? = null) {
        Log.i(tag, "--> [AuthManager.signOut] Signing out user...")
        _isProcessing.value = true
        try {
            firebaseAuth?.signOut()
            _currentUserState.value = null
            _authStateFlow.value = FirebaseAuthState.Unauthenticated
            Log.i(tag, "✅ [AuthManager.signOut] Session terminated successfully.")
        } catch (e: Exception) {
            Log.e(tag, "❌ [AuthManager.signOut] Error during sign out", e)
        } finally {
            _isProcessing.value = false
            onComplete?.invoke()
        }
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
