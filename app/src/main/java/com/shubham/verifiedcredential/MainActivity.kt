package com.shubham.verifiedcredential

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.shubham.verifiedcredential.backend.BackendClient
import com.shubham.verifiedcredential.backend.FirebaseSessionRequest
import com.shubham.verifiedcredential.backend.VerifiedEmailCredentialRequest
import com.shubham.verifiedcredential.ui.theme.VerifiedCredentialTheme
import java.security.SecureRandom
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerifiedCredentialTheme {
                VerifiedCredentialApp()
            }
        }
    }
}

enum class AuthMode {
    SignIn,
    SignUp
}

private const val RELYING_PARTY_ID = "movieapp-cdb85.web.app"
private const val RELYING_PARTY_NAME = "Verified Credential"
private const val BIOMETRIC_KEY_ALIAS = "verified_credential_biometric_gate"
private const val BIOMETRIC_SESSION_WINDOW_MS = 2 * 60 * 1000L

data class StoredProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val emailVerified: Boolean = false,
    val anonymous: Boolean = false,
    val authProvider: String = "",
    val verificationSource: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class AuthUiState(
    val mode: AuthMode = AuthMode.SignUp,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val smsCode: String = "",
    val phoneVerificationId: String? = null,
    val isPhoneCodeSent: Boolean = false,
    val uid: String? = null,
    val signedInEmail: String? = null,
    val signedInPhoneNumber: String? = null,
    val isEmailVerified: Boolean = false,
    val profile: StoredProfile? = null,
    val credentialNonce: String? = null,
    val credentialPreview: String? = null,
    val passkeyChallenge: String? = null,
    val passkeyRegistrationPreview: String? = null,
    val passkeyAuthenticationPreview: String? = null,
    val isBiometricUnlocked: Boolean = false,
    val biometricStatus: String = "Not checked yet",
    val biometricExpiresAtMillis: Long? = null,
    val backendJwt: String? = null,
    val backendStatus: String = "Backend not synced yet",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val secureRandom = SecureRandom()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        auth.currentUser?.let { user ->
            _state.update {
                it.copy(
                    uid = user.uid,
                    signedInEmail = user.email,
                    signedInPhoneNumber = user.phoneNumber,
                    isEmailVerified = user.isEmailVerified,
                    name = user.displayName.orEmpty(),
                    email = user.email.orEmpty(),
                    phoneNumber = user.phoneNumber.orEmpty()
                )
            }

            viewModelScope.launch {
                refreshVerification(showSuccessWhenVerified = false)
            }
        }
    }

    fun setMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, message = null, error = null) }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value.trim()) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun onPhoneNumberChange(value: String) {
        _state.update { it.copy(phoneNumber = value.trim()) }
    }

    fun onSmsCodeChange(value: String) {
        _state.update { it.copy(smsCode = value.trim()) }
    }

    fun signUp() {
        val current = _state.value
        val validationError = validateCredentials(current, requireName = true)
        if (validationError != null) {
            _state.update { it.copy(error = validationError, message = null) }
            return
        }

        viewModelScope.launch {
            runAuthAction {
                val result = auth
                    .createUserWithEmailAndPassword(current.email, current.password)
                    .await()
                val user = result.user ?: error("Firebase did not return a user.")
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(current.name.trim())
                        .build()
                ).await()
                user.sendEmailVerification().await()

                _state.update {
                    it.copy(
                        uid = user.uid,
                        signedInEmail = user.email,
                        isEmailVerified = false,
                        profile = null,
                        message = "Verification email sent. Open it, then come back and tap refresh.",
                        error = null
                    )
                }
            }
        }
    }

    fun signIn() {
        val current = _state.value
        val validationError = validateCredentials(current, requireName = false)
        if (validationError != null) {
            _state.update { it.copy(error = validationError, message = null) }
            return
        }

        viewModelScope.launch {
            runAuthAction {
                val result = auth
                    .signInWithEmailAndPassword(current.email, current.password)
                    .await()
                val user = result.user ?: error("Firebase did not return a user.")
                user.reload().await()
                applyAuthenticatedUser(
                    user = user,
                    showSuccess = true,
                    verificationSource = "firebase_email_verification",
                    requireVerifiedEmail = true
                )
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            runAuthAction {
                val googleCredential = try {
                    getGoogleIdCredential(
                        activity = activity,
                        filterByAuthorizedAccounts = true
                    )
                } catch (_: NoCredentialException) {
                    getGoogleIdCredential(
                        activity = activity,
                        filterByAuthorizedAccounts = false
                    )
                }
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )
                val result = auth.signInWithCredential(firebaseCredential).await()
                val user = result.user ?: error("Firebase did not return a Google user.")
                user.reload().await()
                applyAuthenticatedUser(
                    user = auth.currentUser ?: user,
                    showSuccess = true,
                    verificationSource = "google_sign_in",
                    requireVerifiedEmail = false
                )
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            runAuthAction {
                val result = auth.signInAnonymously().await()
                val user = result.user ?: error("Firebase did not return an anonymous user.")
                applyAuthenticatedUser(
                    user = user,
                    showSuccess = true,
                    verificationSource = "anonymous",
                    requireVerifiedEmail = false
                )
            }
        }
    }

    fun sendPhoneOtp(activity: Activity) {
        val phone = _state.value.phoneNumber
        if (!phone.startsWith("+") || phone.length < 8) {
            _state.update {
                it.copy(error = "Enter a phone number in E.164 format, for example +16505551234.")
            }
            return
        }

        _state.update { it.copy(isLoading = true, error = null, message = null) }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(exception: com.google.firebase.FirebaseException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = exception.localizedMessage ?: "Phone verification failed."
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPhoneCodeSent = true,
                        phoneVerificationId = verificationId,
                        message = "OTP sent to $phone.",
                        error = null
                    )
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneOtp() {
        val verificationId = _state.value.phoneVerificationId
        val code = _state.value.smsCode
        if (verificationId.isNullOrBlank()) {
            _state.update { it.copy(error = "Send the OTP first.") }
            return
        }
        if (code.length < 6) {
            _state.update { it.copy(error = "Enter the 6 digit OTP.") }
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            runAuthAction {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: error("Firebase did not return a phone user.")
                applyAuthenticatedUser(
                    user = user,
                    showSuccess = true,
                    verificationSource = "phone_otp",
                    requireVerifiedEmail = false
                )
            }
        }
    }

    fun resendVerificationEmail() {
        val user = auth.currentUser
        if (user == null) {
            _state.update { it.copy(error = "Sign in first so Firebase knows which email to verify.") }
            return
        }

        viewModelScope.launch {
            runAuthAction {
                user.sendEmailVerification().await()
                _state.update {
                    it.copy(
                        message = "Verification email sent again.",
                        error = null
                    )
                }
            }
        }
    }

    fun refreshVerification(showSuccessWhenVerified: Boolean = true) {
        val user = auth.currentUser
        if (user == null) {
            _state.update { it.copy(error = "No signed-in Firebase user found.") }
            return
        }

        viewModelScope.launch {
            runAuthAction {
                user.reload().await()
                applyAuthenticatedUser(
                    user = auth.currentUser ?: user,
                    showSuccess = showSuccessWhenVerified,
                    verificationSource = providerSource(auth.currentUser ?: user),
                    requireVerifiedEmail = requiresEmailVerification(auth.currentUser ?: user)
                )
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _state.update {
            AuthUiState(
                mode = AuthMode.SignIn,
                message = "Signed out."
            )
        }
    }

    fun setBiometricResult(
        isUnlocked: Boolean,
        status: String
    ) {
        val expiresAt = if (isUnlocked) {
            System.currentTimeMillis() + BIOMETRIC_SESSION_WINDOW_MS
        } else {
            null
        }
        _state.update {
            it.copy(
                isBiometricUnlocked = isUnlocked,
                biometricStatus = status,
                biometricExpiresAtMillis = expiresAt,
                message = if (isUnlocked) "Biometric authentication succeeded on this device." else it.message,
                error = if (isUnlocked) null else status
            )
        }
    }

    fun expireBiometricIfNeeded() {
        val state = _state.value
        val expiresAt = state.biometricExpiresAtMillis ?: return
        if (state.isBiometricUnlocked && System.currentTimeMillis() >= expiresAt) {
            _state.update {
                it.copy(
                    isBiometricUnlocked = false,
                    biometricExpiresAtMillis = null,
                    biometricStatus = "Biometric session expired. Authenticate again."
                )
            }
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun requestVerifiedEmailCredential(activity: Activity) {
        viewModelScope.launch {
            runAuthAction {
                val nonce = createNonce()
                val request = GetCredentialRequest(
                    listOf(
                        GetDigitalCredentialOption(
                            requestJson = verifiedEmailRequestJson(nonce)
                        )
                    )
                )

                val response = CredentialManager
                    .create(activity)
                    .getCredential(activity, request)

                val credential = response.credential
                if (credential is DigitalCredential) {
                    val backendResponse = BackendClient.api.verifyEmailCredential(
                        VerifiedEmailCredentialRequest(
                            credentialJson = credential.credentialJson,
                            nonce = nonce,
                            fcmToken = currentFcmToken()
                        )
                    )
                    backendResponse.firebaseCustomToken?.let { customToken ->
                        auth.signInWithCustomToken(customToken).await()
                    }
                    auth.currentUser?.reload()?.await()
                    auth.currentUser?.let { user ->
                        applyAuthenticatedUser(
                            user = user,
                            showSuccess = false,
                            verificationSource = "verified_email_credential",
                            requireVerifiedEmail = false
                        )
                    }
                    _state.update {
                        it.copy(
                            credentialNonce = nonce,
                            credentialPreview = credential.credentialJson.take(900),
                            backendJwt = backendResponse.appJwt,
                            backendStatus = "Verified email backend accepted ${backendResponse.profile.email.orEmpty()}.",
                            message = "Verified email credential was verified by backend.",
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(error = "Credential Manager returned ${credential::class.simpleName}, not a digital credential.")
                    }
                }
            }
        }
    }

    fun createPasskey(activity: Activity) {
        val user = auth.currentUser
        if (user == null) {
            _state.update { it.copy(error = "Sign in and verify the email before creating a passkey.") }
            return
        }
        if (!user.isEmailVerified) {
            _state.update { it.copy(error = "Verify the Firebase email first, then create the passkey.") }
            return
        }

        viewModelScope.launch {
            runAuthAction {
                val challenge = createNonce()
                val request = CreatePublicKeyCredentialRequest(
                    requestJson = passkeyRegistrationOptionsJson(user, challenge)
                )
                val response = CredentialManager
                    .create(activity)
                    .createCredential(activity, request)

                if (response is CreatePublicKeyCredentialResponse) {
                    _state.update {
                        it.copy(
                            passkeyChallenge = challenge,
                            passkeyRegistrationPreview = response.registrationResponseJson.take(900),
                            message = "Passkey registration response received. Send it to your backend to verify and store the public key.",
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(error = "Credential Manager returned ${response::class.simpleName}, not a passkey registration response.")
                    }
                }
            }
        }
    }

    fun signInWithPasskey(activity: Activity) {
        viewModelScope.launch {
            runAuthAction {
                val challenge = createNonce()
                val request = GetCredentialRequest(
                    listOf(
                        GetPublicKeyCredentialOption(
                            requestJson = passkeyAuthenticationOptionsJson(challenge)
                        )
                    )
                )
                val response = CredentialManager
                    .create(activity)
                    .getCredential(activity, request)

                val credential = response.credential
                if (credential is PublicKeyCredential) {
                    _state.update {
                        it.copy(
                            passkeyChallenge = challenge,
                            passkeyAuthenticationPreview = credential.authenticationResponseJson.take(900),
                            message = "Passkey assertion received. Backend verification should mint/sign in with a Firebase custom token.",
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(error = "Credential Manager returned ${credential::class.simpleName}, not a passkey assertion.")
                    }
                }
            }
        }
    }

    private suspend fun runAuthAction(block: suspend () -> Unit) {
        _state.update { it.copy(isLoading = true, error = null, message = null) }
        try {
            block()
        } catch (exception: CreateCredentialException) {
            _state.update {
                it.copy(
                    error = exception.message ?: "Passkey creation was not completed.",
                    isLoading = false
                )
            }
            return
        } catch (exception: GetCredentialException) {
            _state.update {
                it.copy(
                    error = exception.message ?: "No verified email credential was returned.",
                    isLoading = false
                )
            }
            return
        } catch (exception: Exception) {
            _state.update {
                it.copy(
                    error = exception.localizedMessage ?: "Something went wrong.",
                    isLoading = false
                )
            }
            return
        }
        _state.update { it.copy(isLoading = false) }
    }

    private suspend fun getGoogleIdCredential(
        activity: Activity,
        filterByAuthorizedAccounts: Boolean
    ): GoogleIdTokenCredential {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .setNonce(createNonce())
            .build()

        val response = CredentialManager
            .create(activity)
            .getCredential(
                context = activity,
                request = GetCredentialRequest(listOf(googleIdOption))
            )
        val credential = response.credential

        return when {
            credential is GoogleIdTokenCredential -> credential
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                GoogleIdTokenCredential.createFrom(credential.data)
            }
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL -> {
                GoogleIdTokenCredential.createFrom(credential.data)
            }
            else -> error("Credential Manager returned ${credential::class.simpleName}, not a Google ID token.")
        }
    }

    private suspend fun applyAuthenticatedUser(
        user: FirebaseUser,
        showSuccess: Boolean,
        verificationSource: String,
        requireVerifiedEmail: Boolean
    ) {
        if (requireVerifiedEmail && !user.isEmailVerified) {
            _state.update {
                it.copy(
                    uid = user.uid,
                    signedInEmail = user.email,
                    signedInPhoneNumber = user.phoneNumber,
                    isEmailVerified = false,
                    profile = null,
                    message = "Email is not verified yet.",
                    error = null
                )
            }
            return
        }

        val localProfile = localProfile(user, verificationSource)
        _state.update {
            it.copy(
                uid = user.uid,
                signedInEmail = user.email,
                signedInPhoneNumber = user.phoneNumber,
                isEmailVerified = true,
                profile = localProfile,
                backendStatus = "Signed in locally. Syncing Firestore/backend...",
                message = if (showSuccess) "Signed in. Syncing profile..." else null,
                error = null
            )
        }
        syncFirestoreAndBackend(user, verificationSource)
    }

    private suspend fun syncFirestoreAndBackend(
        user: FirebaseUser,
        verificationSource: String
    ) {
        try {
            val savedProfile = saveVerifiedProfile(user, verificationSource = verificationSource)
            _state.update {
                it.copy(
                    profile = savedProfile,
                    backendStatus = "Firestore synced. Syncing backend..."
                )
            }
        } catch (exception: Exception) {
            _state.update {
                it.copy(
                    backendStatus = "Firestore sync failed: ${exception.localizedMessage ?: "Unknown error"}"
                )
            }
        }

        syncBackendSession(user)
    }

    private fun localProfile(
        user: FirebaseUser,
        verificationSource: String
    ): StoredProfile {
        return StoredProfile(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            phoneNumber = user.phoneNumber.orEmpty(),
            emailVerified = user.isEmailVerified,
            anonymous = user.isAnonymous,
            authProvider = providerSource(user),
            verificationSource = verificationSource,
            createdAt = "Local pending",
            updatedAt = "Local pending"
        )
    }

    private suspend fun saveVerifiedProfile(
        user: FirebaseUser,
        verificationSource: String
    ): StoredProfile {
        val document = firestore.collection("users").document(user.uid)
        val snapshot = document.get().await()
        val data = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "displayName" to user.displayName.orEmpty(),
            "email" to user.email.orEmpty(),
            "phoneNumber" to user.phoneNumber.orEmpty(),
            "emailVerified" to user.isEmailVerified,
            "anonymous" to user.isAnonymous,
            "authProvider" to providerSource(user),
            "verificationSource" to verificationSource,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snapshot.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        document.set(data, SetOptions.merge()).await()
        val saved = document.get().await()
        return StoredProfile(
            uid = saved.getString("uid").orEmpty(),
            displayName = saved.getString("displayName").orEmpty(),
            email = saved.getString("email").orEmpty(),
            phoneNumber = saved.getString("phoneNumber").orEmpty(),
            emailVerified = saved.getBoolean("emailVerified") == true,
            anonymous = saved.getBoolean("anonymous") == true,
            authProvider = saved.getString("authProvider").orEmpty(),
            verificationSource = saved.getString("verificationSource").orEmpty(),
            createdAt = saved.getTimestamp("createdAt")?.toDate()?.toString().orEmpty(),
            updatedAt = saved.getTimestamp("updatedAt")?.toDate()?.toString().orEmpty()
        )
    }

    private suspend fun syncBackendSession(user: FirebaseUser) {
        try {
            val firebaseIdToken = user.getIdToken(false).await().token
                ?: error("Firebase did not return an ID token.")
            val response = BackendClient.api.createSession(
                FirebaseSessionRequest(
                    firebaseIdToken = firebaseIdToken,
                    fcmToken = currentFcmToken()
                )
            )
            _state.update {
                it.copy(
                    backendJwt = response.appJwt,
                    backendStatus = "Backend synced as ${response.profile.authProvider}."
                )
            }
        } catch (exception: Exception) {
            _state.update {
                it.copy(
                    backendStatus = backendErrorMessage(exception)
                )
            }
        }
    }

    private fun backendErrorMessage(exception: Exception): String {
        if (exception is HttpException) {
            val body = exception.response()?.errorBody()?.string()
            return buildString {
                append("Backend sync failed: HTTP ")
                append(exception.code())
                if (!body.isNullOrBlank()) {
                    append(" ")
                    append(body.take(240))
                }
            }
        }
        return exception.localizedMessage ?: "Backend sync failed. Is the API running?"
    }

    private suspend fun currentFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            null
        }
    }

    private fun validateCredentials(
        state: AuthUiState,
        requireName: Boolean
    ): String? {
        return when {
            requireName && state.name.trim().length < 2 -> "Enter your name."
            state.email.isBlank() || !state.email.contains("@") -> "Enter a valid email."
            state.password.length < 6 -> "Password should be at least 6 characters."
            else -> null
        }
    }

    private fun requiresEmailVerification(user: FirebaseUser): Boolean {
        return providerSource(user) == "password"
    }

    private fun providerSource(user: FirebaseUser): String {
        if (user.isAnonymous) return "anonymous"
        return user.providerData
            .map { it.providerId }
            .firstOrNull { it != "firebase" }
            ?: "firebase"
    }

    private fun createNonce(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.base64Url()
    }

    private fun ByteArray.base64Url(): String {
        return Base64.encodeToString(
            this,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    private fun passkeyRegistrationOptionsJson(
        user: FirebaseUser,
        challenge: String
    ): String {
        val userId = user.uid.toByteArray(Charsets.UTF_8).base64Url()
        val email = jsonEscape(user.email.orEmpty())
        val displayName = jsonEscape(user.displayName?.ifBlank { user.email }.orEmpty())

        return """
            {
              "challenge": "$challenge",
              "rp": {
                "name": "$RELYING_PARTY_NAME",
                "id": "$RELYING_PARTY_ID"
              },
              "user": {
                "id": "$userId",
                "name": "$email",
                "displayName": "$displayName"
              },
              "pubKeyCredParams": [
                {
                  "type": "public-key",
                  "alg": -7
                },
                {
                  "type": "public-key",
                  "alg": -257
                }
              ],
              "timeout": 60000,
              "attestation": "none",
              "authenticatorSelection": {
                "residentKey": "required",
                "requireResidentKey": true,
                "userVerification": "required"
              }
            }
        """.trimIndent()
    }

    private fun passkeyAuthenticationOptionsJson(challenge: String): String {
        return """
            {
              "challenge": "$challenge",
              "rpId": "$RELYING_PARTY_ID",
              "timeout": 60000,
              "userVerification": "required"
            }
        """.trimIndent()
    }

    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun verifiedEmailRequestJson(nonce: String): String {
        return """
            {
              "requests": [
                {
                  "protocol": "openid4vp-v1-unsigned",
                  "data": {
                    "response_type": "vp_token",
                    "nonce": "$nonce",
                    "dcql_query": {
                      "credentials": [
                        {
                          "id": "email",
                          "format": "dc+sd-jwt",
                          "meta": {
                            "vct_values": [
                              "https://credentials.google.com/identity/email_address/v1"
                            ]
                          },
                          "claims": [
                            {
                              "path": ["email"]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifiedCredentialApp(viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Verified Credential",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeaderSection()

                    if (state.profile != null && state.isEmailVerified) {
                        HomeScreen(
                            state = state,
                            onRefresh = { viewModel.refreshVerification() },
                            onSignOut = viewModel::signOut
                        )
                    } else {
                        AuthScreen(
                            state = state,
                            onModeChange = viewModel::setMode,
                            onNameChange = viewModel::onNameChange,
                            onEmailChange = viewModel::onEmailChange,
                            onPasswordChange = viewModel::onPasswordChange,
                            onPhoneNumberChange = viewModel::onPhoneNumberChange,
                            onSmsCodeChange = viewModel::onSmsCodeChange,
                            onSubmit = {
                                if (state.mode == AuthMode.SignUp) {
                                    viewModel.signUp()
                                } else {
                                    viewModel.signIn()
                                }
                            },
                            onGoogleSignIn = {
                                val activity = it as? Activity
                                if (activity != null) {
                                    viewModel.signInWithGoogle(activity)
                                }
                            },
                            onAnonymousSignIn = viewModel::signInAnonymously,
                            onSendPhoneOtp = {
                                val activity = it as? Activity
                                if (activity != null) {
                                    viewModel.sendPhoneOtp(activity)
                                }
                            },
                            onVerifyPhoneOtp = viewModel::verifyPhoneOtp,
                            onResend = viewModel::resendVerificationEmail,
                            onRefreshVerification = { viewModel.refreshVerification() },
                            onSignOut = viewModel::signOut
                        )
                    }

                    CredentialManagerSection(
                        state = state,
                        onRequestCredential = {
                            val activity = it as? Activity
                            if (activity != null) {
                                viewModel.requestVerifiedEmailCredential(activity)
                            }
                        }
                    )

                    PasskeySection(
                        state = state,
                        onCreatePasskey = {
                            val activity = it as? Activity
                            if (activity != null) {
                                viewModel.createPasskey(activity)
                            }
                        },
                        onSignInWithPasskey = {
                            val activity = it as? Activity
                            if (activity != null) {
                                viewModel.signInWithPasskey(activity)
                            }
                        }
                    )

                    BiometricSection(
                        state = state,
                        onExpireCheck = viewModel::expireBiometricIfNeeded,
                        onAuthenticate = {
                            val activity = it as? FragmentActivity
                            if (activity != null) {
                                launchBiometricAuthentication(
                                    activity = activity,
                                    onResult = viewModel::setBiometricResult
                                )
                            }
                        }
                    )

                    StatusMessages(state = state)
                }

                if (state.isLoading) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Email verification without losing the user",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "Firebase handles the current auth path. Credential Manager shows the new one-tap verified email handoff that should be validated on a backend.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSmsCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: (Any) -> Unit,
    onAnonymousSignIn: () -> Unit,
    onSendPhoneOtp: (Any) -> Unit,
    onVerifyPhoneOtp: () -> Unit,
    onResend: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == AuthMode.SignUp,
                    onClick = { onModeChange(AuthMode.SignUp) },
                    label = { Text("Sign up") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                FilterChip(
                    selected = state.mode == AuthMode.SignIn,
                    onClick = { onModeChange(AuthMode.SignIn) },
                    label = { Text("Sign in") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            if (state.mode == AuthMode.SignUp) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (state.mode == AuthMode.SignUp) {
                        Icons.Outlined.PersonAdd
                    } else {
                        Icons.AutoMirrored.Outlined.Login
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.mode == AuthMode.SignUp) "Create account" else "Sign in")
            }

            OutlinedButton(
                onClick = { onGoogleSignIn(context) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google")
            }

            OutlinedButton(
                onClick = onAnonymousSignIn,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue anonymously")
            }

            PhoneAuthFields(
                state = state,
                onPhoneNumberChange = onPhoneNumberChange,
                onSmsCodeChange = onSmsCodeChange,
                onSendPhoneOtp = { onSendPhoneOtp(context) },
                onVerifyPhoneOtp = onVerifyPhoneOtp
            )

            if (state.uid != null && !state.isEmailVerified) {
                VerificationActions(
                    email = state.signedInEmail.orEmpty(),
                    onResend = onResend,
                    onRefreshVerification = onRefreshVerification,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun PhoneAuthFields(
    state: AuthUiState,
    onPhoneNumberChange: (String) -> Unit,
    onSmsCodeChange: (String) -> Unit,
    onSendPhoneOtp: () -> Unit,
    onVerifyPhoneOtp: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Phone OTP",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number") },
            placeholder = { Text("+16505551234") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

        if (state.isPhoneCodeSent) {
            OutlinedTextField(
                value = state.smsCode,
                onValueChange = onSmsCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onSendPhoneOtp,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isPhoneCodeSent) "Resend OTP" else "Send OTP")
            }
            Button(
                onClick = onVerifyPhoneOtp,
                enabled = !state.isLoading && state.isPhoneCodeSent,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify")
            }
        }
    }
}

@Composable
private fun VerificationActions(
    email: String,
    onResend: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text("Waiting for Firebase email verification: $email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onResend,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resend")
            }
            Button(
                onClick = onRefreshVerification,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh")
            }
        }

        TextButton(onClick = onSignOut) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use another account")
        }
    }
}

@Composable
private fun HomeScreen(
    state: AuthUiState,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    val profile = state.profile ?: return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Firestore profile created after verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            ProfileRow(label = "Name", value = profile.displayName.ifBlank { "Not set" })
            ProfileRow(label = "Email", value = profile.email.ifBlank { "Not set" })
            ProfileRow(label = "Phone", value = profile.phoneNumber.ifBlank { "Not set" })
            ProfileRow(label = "UID", value = profile.uid)
            ProfileRow(label = "Email verified", value = profile.emailVerified.toString())
            ProfileRow(label = "Anonymous", value = profile.anonymous.toString())
            ProfileRow(label = "Provider", value = profile.authProvider.ifBlank { "Not set" })
            ProfileRow(label = "Source", value = profile.verificationSource)
            ProfileRow(label = "Backend", value = state.backendStatus)
            ProfileRow(label = "Created", value = profile.createdAt.ifBlank { "Pending server timestamp" })
            ProfileRow(label = "Updated", value = profile.updatedAt.ifBlank { "Pending server timestamp" })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign out")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CredentialManagerSection(
    state: AuthUiState,
    onRequestCredential: (Any) -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Credential Manager verified email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This requests a cryptographic email credential from Android. The app only previews the response; a backend must validate the SD-JWT before Firestore is updated.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            OutlinedButton(
                onClick = { onRequestCredential(context) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request verified email")
            }

            if (state.credentialPreview != null) {
                ProfileRow(label = "Nonce", value = state.credentialNonce.orEmpty())
                ProfileRow(label = "Credential preview", value = state.credentialPreview)
            }
        }
    }
}

@Composable
private fun PasskeySection(
    state: AuthUiState,
    onCreatePasskey: (Any) -> Unit,
    onSignInWithPasskey: (Any) -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Passkeys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Android creates and retrieves passkeys through Credential Manager. Your backend must issue the challenge, verify the response, store the public key, and then mint the Firebase session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            ProfileRow(label = "Relying party", value = RELYING_PARTY_ID)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onCreatePasskey(context) },
                    enabled = !state.isLoading && state.isEmailVerified,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create")
                }
                Button(
                    onClick = { onSignInWithPasskey(context) },
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in")
                }
            }

            if (!state.isEmailVerified) {
                Text(
                    text = "Create is enabled after the Firebase email verification home screen appears.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (state.passkeyRegistrationPreview != null || state.passkeyAuthenticationPreview != null) {
                ProfileRow(label = "Challenge", value = state.passkeyChallenge.orEmpty())
            }
            state.passkeyRegistrationPreview?.let {
                ProfileRow(label = "Registration response preview", value = it)
            }
            state.passkeyAuthenticationPreview?.let {
                ProfileRow(label = "Authentication response preview", value = it)
            }
        }
    }
}

@Composable
private fun BiometricSection(
    state: AuthUiState,
    onExpireCheck: () -> Unit,
    onAuthenticate: (Any) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(state.biometricExpiresAtMillis) {
        while (state.biometricExpiresAtMillis != null) {
            delay(1_000)
            onExpireCheck()
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Biometric device check",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "This uses a Keystore-backed strong-biometric key. New or removed biometrics invalidate the key, and a successful unlock expires after two minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProfileRow(
                label = "Status",
                value = if (state.isBiometricUnlocked) {
                    "Unlocked"
                } else {
                    state.biometricStatus
                }
            )
            ProfileRow(
                label = "Expires",
                value = state.biometricExpiresAtMillis?.let { expiresAt ->
                    val remainingSeconds = ((expiresAt - System.currentTimeMillis()) / 1000)
                        .coerceAtLeast(0)
                    "${remainingSeconds}s remaining"
                } ?: "Locked"
            )
            Button(
                onClick = { onAuthenticate(context) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Authenticate")
            }
        }
    }
}

private fun launchBiometricAuthentication(
    activity: FragmentActivity,
    onResult: (Boolean, String) -> Unit
) {
    val authenticators = BIOMETRIC_STRONG
    val biometricManager = BiometricManager.from(activity)

    when (val availability = biometricManager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> Unit
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            onResult(false, "This device does not have biometric hardware.")
            return
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            onResult(false, "Biometric hardware is currently unavailable.")
            return
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            onResult(false, "No strong biometric is enrolled.")
            return
        }
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
            onResult(false, "A security update is required before biometric auth can run.")
            return
        }
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
            onResult(false, "This biometric configuration is unsupported on this device.")
            return
        }
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
            onResult(false, "Biometric availability is unknown.")
            return
        }
        else -> {
            onResult(false, "Biometric auth is unavailable. Code: $availability")
            return
        }
    }

    val cipher = try {
        createBiometricCipher()
    } catch (exception: KeyPermanentlyInvalidatedException) {
        resetBiometricKey()
        onResult(
            false,
            "Biometric enrollment changed. The old key was invalidated; authenticate again to trust the new biometric set."
        )
        return
    } catch (exception: Exception) {
        onResult(
            false,
            exception.localizedMessage ?: "Could not prepare biometric key."
        )
        return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val cryptoObject = result.cryptoObject
                if (cryptoObject?.cipher == null) {
                    onResult(false, "Biometric prompt returned without a valid crypto object.")
                    return
                }
                onResult(true, "Strong biometric accepted. Session expires in 120s.")
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(false, "Biometric was not recognized. Try again.")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Verified Credential")
        .setSubtitle("Strong biometric required")
        .setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(authenticators)
        .build()

    prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
}

private fun createBiometricCipher(): Cipher {
    val secretKey = getOrCreateBiometricKey()
    return Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
        .apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
}

private fun getOrCreateBiometricKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    )
    val builder = KeyGenParameterSpec.Builder(
        BIOMETRIC_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .setUserAuthenticationRequired(true)
        .setInvalidatedByBiometricEnrollment(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setUserAuthenticationParameters(
            0,
            KeyProperties.AUTH_BIOMETRIC_STRONG
        )
    } else {
        @Suppress("DEPRECATION")
        builder.setUserAuthenticationValidityDurationSeconds(-1)
    }

    keyGenerator.init(builder.build())
    return keyGenerator.generateKey()
}

private fun resetBiometricKey() {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
    getOrCreateBiometricKey()
}

@Composable
private fun StatusMessages(state: AuthUiState) {
    state.message?.let {
        StatusCard(
            text = it,
            isError = false
        )
    }
    state.error?.let {
        StatusCard(
            text = it,
            isError = true
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun StatusCard(text: String, isError: Boolean) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifiedCredentialPreview() {
    VerifiedCredentialTheme {
        HeaderSection()
    }
}
