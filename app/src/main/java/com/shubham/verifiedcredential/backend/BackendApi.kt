package com.shubham.verifiedcredential.backend

import com.shubham.verifiedcredential.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class VerifiedEmailCredentialRequest(
    val credentialJson: String,
    val nonce: String,
    val fcmToken: String? = null
)

data class FirebaseSessionRequest(
    val firebaseIdToken: String,
    val fcmToken: String? = null
)

data class NotificationTokenRequest(
    val firebaseIdToken: String,
    val fcmToken: String,
    val platform: String = "android"
)

data class BackendProfile(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val emailVerified: Boolean,
    val authProvider: String,
    val verificationSource: String
)

data class BackendAuthResponse(
    val firebaseCustomToken: String?,
    val appJwt: String,
    val profile: BackendProfile
)

data class BackendSessionResponse(
    val appJwt: String,
    val profile: BackendProfile
)

data class BackendMessageResponse(
    val ok: Boolean,
    val message: String
)

interface BackendApi {
    @POST("api/auth/verified-email")
    suspend fun verifyEmailCredential(
        @Body request: VerifiedEmailCredentialRequest
    ): BackendAuthResponse

    @POST("api/auth/session")
    suspend fun createSession(
        @Body request: FirebaseSessionRequest
    ): BackendSessionResponse

    @POST("api/notifications/register")
    suspend fun registerNotificationToken(
        @Body request: NotificationTokenRequest
    ): BackendMessageResponse
}

object BackendClient {
    val api: BackendApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
