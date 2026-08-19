package com.example.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Unified AuthViewModel that wraps AuthRepository and handles
 * Firebase authentication events via a single, clean StateFlow.
 * Ensures Google Sign-In status is correctly propagated to the UI.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "AuthViewModel"
    val authRepository = AuthRepository(application)

    val authState: StateFlow<AuthState> = authRepository.authState
    val firebaseUser: StateFlow<FirebaseUser?> = authRepository.firebaseUser
    val isLoading: StateFlow<Boolean> = authRepository.isLoading

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    init {
        Log.d(tag, "AuthViewModel initialized with centralized Firebase AuthStateListener")
    }

    /**
     * Authenticates with Firebase using a Google ID token and optional access token credential.
     * Passes credentials directly to FirebaseAuth instance for proper provider-based sign-in.
     */
    fun signInWithGoogleCredential(
        idToken: String,
        accessToken: String? = null,
        onResult: (success: Boolean, firebaseUser: FirebaseUser?, errorMessage: String?) -> Unit
    ) {
        startAuthentication()
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, accessToken)
            signInWithCredential(credential, onResult)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create GoogleAuthProvider credential", e)
            val errorMsg = e.localizedMessage ?: "Failed to process Google credential"
            onAuthError(errorMsg)
            onResult(false, null, errorMsg)
        }
    }

    /**
     * Authenticates with Firebase using a provided AuthCredential instance.
     */
    fun signInWithCredential(
        credential: AuthCredential,
        onResult: (success: Boolean, firebaseUser: FirebaseUser?, errorMessage: String?) -> Unit
    ) {
        startAuthentication()
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (task.isSuccessful) {
                        val user = task.result?.user ?: FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            onAuthSuccess(user)
                            _authErrorMessage.value = null
                            onResult(true, user, null)
                        } else {
                            val msg = "Authentication succeeded but no Firebase user found."
                            onAuthError(msg)
                            onResult(false, null, msg)
                        }
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Firebase credential authentication failed."
                        Log.w(tag, "signInWithCredential failed", task.exception)
                        onAuthError(errorMsg)
                        onResult(false, null, errorMsg)
                    }
                }
            }
    }

    /**
     * Triggers Google Sign-In via GoogleAuthManager and propagates results.
     */
    fun signInWithGoogle(
        activity: Activity,
        webClientId: String = GoogleAuthManager.DEFAULT_WEB_CLIENT_ID,
        onResult: (success: Boolean, firebaseUser: FirebaseUser?, email: String?, displayName: String?, photoUrl: String?, errorMessage: String?) -> Unit
    ) {
        startAuthentication()
        GoogleAuthManager.signInWithGoogle(
            context = getApplication(),
            activity = activity,
            scope = viewModelScope,
            webClientId = webClientId
        ) { success, user, email, displayName, photoUrl, error ->
            viewModelScope.launch {
                if (success) {
                    val finalUser = user ?: FirebaseAuth.getInstance().currentUser
                    if (finalUser != null) {
                        onAuthSuccess(finalUser)
                    } else {
                        authRepository.onAuthError("Google authentication completed without Firebase user instance.")
                    }
                    _authErrorMessage.value = null
                } else {
                    val msg = error ?: "Google Sign-In failed."
                    onAuthError(msg)
                    _authErrorMessage.value = msg
                }
                onResult(success, user, email, displayName, photoUrl, error)
            }
        }
    }

    fun startAuthentication() {
        _authErrorMessage.value = null
        authRepository.startAuthentication()
    }

    fun onAuthSuccess(user: FirebaseUser) {
        _authErrorMessage.value = null
        authRepository.onAuthSuccess(user)
    }

    fun onAuthError(message: String) {
        _authErrorMessage.value = message
        authRepository.onAuthError(message)
    }

    fun clearError() {
        _authErrorMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                GoogleAuthManager(getApplication()).signOut()
                _authErrorMessage.value = null
            } catch (e: Exception) {
                Log.e(tag, "Error during AuthViewModel signOut", e)
            }
        }
    }
}

typealias AuthenticationViewModel = AuthViewModel
