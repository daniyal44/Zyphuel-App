package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthManager
import com.example.auth.GoogleAuthManager
import com.example.ui.AuthScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * LoginActivity implementing the Android Credential Manager API with Google Auth providers.
 * Correctly retrieves and authenticates real device accounts via GoogleIdTokenCredential
 * and authenticates with Firebase Auth without fallback to mock or placeholder data.
 */
class LoginActivity : FragmentActivity() {

    private lateinit var credentialManager: CredentialManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        credentialManager = CredentialManager.create(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val targetRole = intent.getStringExtra(EXTRA_TARGET_ROLE) ?: "customer"
        val isRegister = intent.getBooleanExtra(EXTRA_IS_REGISTER, false)
        val autoTriggerGoogle = intent.getBooleanExtra(EXTRA_AUTO_TRIGGER_GOOGLE, false)

        if (autoTriggerGoogle) {
            launchCredentialManagerGoogleSignIn(targetRole = targetRole)
        }

        setContent {
            MyApplicationTheme {
                AuthScreen(
                    viewModel = viewModel,
                    isRegister = isRegister,
                    isRider = (targetRole.lowercase() == "rider")
                )
            }
        }
    }

    /**
     * Executes the Credential Manager API request with GetGoogleIdOption to authenticate
     * real device accounts using the configured Google Web Client ID.
     */
    fun launchCredentialManagerGoogleSignIn(
        targetRole: String = "customer",
        onCompleted: ((success: Boolean, email: String?, displayName: String?, errorMessage: String?) -> Unit)? = null
    ) {
        lifecycleScope.launch {
            Log.d(TAG, "Initiating Credential Manager Google Sign-In for role: $targetRole")

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GoogleAuthManager.DEFAULT_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d(TAG, "Requesting credentials from CredentialManager...")
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val realEmail = googleIdTokenCredential.id
                    val realDisplayName = googleIdTokenCredential.displayName ?: realEmail.substringBefore("@")
                    val realPhotoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                    Log.i(TAG, "Successfully retrieved real Google account: $realEmail")

                    // Authenticate with Firebase using real Google ID token
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                val fbUser = authTask.result?.user
                                val uid = fbUser?.uid ?: ""
                                Log.i(TAG, "Firebase Authentication successful for user: $realEmail (UID: $uid)")

                                viewModel.loginWithSocialAccount(
                                    provider = "Google",
                                    socialEmail = realEmail,
                                    socialName = realDisplayName,
                                    profilePicUrl = realPhotoUrl,
                                    targetRole = targetRole,
                                    uid = uid,
                                    onFailure = { err ->
                                        Log.e(TAG, "Social login failure: $err")
                                        Toast.makeText(this@LoginActivity, err, Toast.LENGTH_LONG).show()
                                        onCompleted?.invoke(false, null, null, err)
                                    }
                                ) { user ->
                                    viewModel.completeLogin(user)
                                    setResult(Activity.RESULT_OK, Intent().apply {
                                        putExtra(EXTRA_USER_EMAIL, realEmail)
                                        putExtra(EXTRA_USER_NAME, realDisplayName)
                                        putExtra(EXTRA_USER_ROLE, user.role)
                                        putExtra(EXTRA_USER_UID, uid)
                                    })
                                    Toast.makeText(this@LoginActivity, "Welcome back, ${user.name}! 🚀", Toast.LENGTH_SHORT).show()
                                    onCompleted?.invoke(true, realEmail, realDisplayName, null)
                                    finish()
                                }
                            } else {
                                val err = authTask.exception?.localizedMessage ?: "Firebase token validation failed."
                                Log.e(TAG, "Firebase sign-in error: $err")
                                Toast.makeText(this@LoginActivity, err, Toast.LENGTH_LONG).show()
                                onCompleted?.invoke(false, null, null, err)
                            }
                        }
                } else {
                    val msg = "Unsupported credential format received from Credential Manager."
                    Log.w(TAG, msg)
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    onCompleted?.invoke(false, null, null, msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, "User dismissed the Google Sign-In prompt.")
                Toast.makeText(this@LoginActivity, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show()
                onCompleted?.invoke(false, null, null, "Cancelled by user")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager Exception: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Google Account error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onCompleted?.invoke(false, null, null, e.localizedMessage)
            } catch (e: Exception) {
                Log.e(TAG, "General Exception in Credential Manager: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Sign-In error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onCompleted?.invoke(false, null, null, e.localizedMessage)
            }
        }
    }

    companion object {
        private const val TAG = "LoginActivity"

        const val EXTRA_TARGET_ROLE = "extra_target_role"
        const val EXTRA_IS_REGISTER = "extra_is_register"
        const val EXTRA_AUTO_TRIGGER_GOOGLE = "extra_auto_trigger_google"

        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_ROLE = "extra_user_role"
        const val EXTRA_USER_UID = "extra_user_uid"

        fun createIntent(
            context: Context,
            targetRole: String = "customer",
            isRegister: Boolean = false,
            autoTriggerGoogle: Boolean = false
        ): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_TARGET_ROLE, targetRole)
                putExtra(EXTRA_IS_REGISTER, isRegister)
                putExtra(EXTRA_AUTO_TRIGGER_GOOGLE, autoTriggerGoogle)
            }
        }
    }
}
