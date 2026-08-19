package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * AuthRepository manages Firebase Authentication state listening and session state synchronization.
 * Listens to FirebaseAuth.getInstance().addAuthStateListener to ensure UI state (e.g., loading spinners)
 * stays in sync with actual Firebase session state.
 */
class AuthRepository(private val context: Context) {

    private val tag = "AuthRepository"

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(null)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        initAuthStateListener()
    }

    private fun initAuthStateListener() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val firebaseAuth = FirebaseAuth.getInstance()
                
                val currentUser = firebaseAuth.currentUser
                _firebaseUser.value = currentUser
                if (currentUser != null) {
                    _authState.value = AuthState.Authenticated(currentUser)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }

                authListener = FirebaseAuth.AuthStateListener { auth ->
                    val user = auth.currentUser
                    _firebaseUser.value = user
                    _isLoading.value = false
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user)
                        Log.d(tag, "Firebase AuthStateListener: User authenticated (${user.email})")
                    } else {
                        _authState.value = AuthState.Unauthenticated
                        Log.d(tag, "Firebase AuthStateListener: User unauthenticated")
                    }
                }

                authListener?.let { listener ->
                    firebaseAuth.addAuthStateListener(listener)
                }
            } else {
                Log.w(tag, "FirebaseApp is not initialized in context.")
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AuthStateListener", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Auth state error")
            _isLoading.value = false
        }
    }

    fun startAuthentication() {
        _isLoading.value = true
        _authState.value = AuthState.Loading
    }

    fun onAuthSuccess(user: FirebaseUser) {
        _firebaseUser.value = user
        _isLoading.value = false
        _authState.value = AuthState.Authenticated(user)
    }

    fun onAuthError(message: String) {
        _isLoading.value = false
        _authState.value = AuthState.Error(message)
    }

    fun signOut() {
        try {
            _isLoading.value = true
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
            _firebaseUser.value = null
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            Log.e(tag, "Error during signOut", e)
        } finally {
            _isLoading.value = false
        }
    }
}
