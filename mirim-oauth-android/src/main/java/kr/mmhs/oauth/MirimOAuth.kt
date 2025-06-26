package kr.mmhs.oauth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 미림마이스터고 OAuth 인증을 처리하는 메인 클래스
 */
class MirimOAuth(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
    private val scopes: List<String>,
    private val oauthServerUrl: String = "https://api-auth.mmhs.app"
) {
    companion object {
        private const val TOKEN_KEY = "mirim_oauth_tokens"
        private const val USER_KEY = "mirim_oauth_user"
        private const val PREFS_NAME = "mirim_oauth_prefs"
    }

    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 보안 저장소
    private val encryptedPrefs by lazy {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private var currentUser: MirimUser? = null
    private var tokens: AuthTokens? = null
    private var authCallback: ((Result<MirimUser>) -> Unit)? = null

    // OAuth 콜백 수신기
    private val oauthReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val uriString = intent?.getStringExtra("uri")
            if (uriString != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val uri = Uri.parse(uriString)
                        val code = uri.getQueryParameter("code")
                        val state = uri.getQueryParameter("state")
                        
                        if (code != null) {
                            val tokens = exchangeCodeForTokens(code, state)
                            val user = fetchUserInfo(tokens.accessToken)
                            
                            this@MirimOAuth.tokens = tokens
                            this@MirimOAuth.currentUser = user
                            
                            authCallback?.invoke(Result.success(user))
                        } else {
                            authCallback?.invoke(Result.failure(MirimOAuthException("Authorization code not received")))
                        }
                    } catch (e: Exception) {
                        authCallback?.invoke(Result.failure(e))
                    } finally {
                        authCallback = null
                        unregisterReceiver()
                    }
                }
            }
        }
    }

    /**
     * 현재 로그인된 사용자 정보
     */
    fun getCurrentUser(): MirimUser? = currentUser

    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean = currentUser != null && tokens != null && !tokens!!.isExpired

    /**
     * 현재 액세스 토큰 반환
     */
    suspend fun getAccessToken(): String {
        val validTokens = getValidTokens()
        return validTokens.accessToken
    }

    /**
     * OAuth 로그인 수행
     */
    suspend fun logIn(): MirimUser = suspendCoroutine { continuation ->
        try {
            authCallback = { result ->
                result.fold(
                    onSuccess = { user -> continuation.resume(user) },
                    onFailure = { error -> continuation.resumeWithException(error) }
                )
            }

            registerReceiver()
            startOAuthFlow()
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * 로그아웃
     */
    suspend fun logOut() {
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit()
                .remove(TOKEN_KEY)
                .remove(USER_KEY)
                .apply()
        }
        currentUser = null
        tokens = null
    }

    /**
     * 로그인 상태 확인 및 토큰 갱신
     */
    suspend fun checkIsLoggedIn(): Boolean {
        if (isLoggedIn()) return true

        return withContext(Dispatchers.IO) {
            val tokenJson = encryptedPrefs.getString(TOKEN_KEY, null)
            if (tokenJson == null) {
                return@withContext false
            }

            try {
                val storedTokens = gson.fromJson(tokenJson, AuthTokens::class.java)
                if (storedTokens.isExpired) {
                    try {
                        tokens = refreshTokens(storedTokens.refreshToken)
                        currentUser = getStoredUser()
                        return@withContext true
                    } catch (e: Exception) {
                        logOut()
                        return@withContext false
                    }
                }
                tokens = storedTokens
                currentUser = getStoredUser()
                return@withContext true
            } catch (e: Exception) {
                logOut()
                return@withContext false
            }
        }
    }

    /**
     * 사용자 정보 새로고침
     */
    suspend fun refreshUserInfo(): MirimUser {
        val validTokens = getValidTokens()
        val user = fetchUserInfo(validTokens.accessToken)
        currentUser = user
        return user
    }

    /**
     * 인증된 HTTP 요청 수행
     */
    suspend fun makeAuthenticatedRequest(
        endpoint: String,
        method: String = "GET",
        body: Map<String, Any>? = null,
        additionalHeaders: Map<String, String>? = null
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        val validTokens = getValidTokens()
        
        val requestBuilder = Request.Builder()
            .url("$oauthServerUrl$endpoint")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${validTokens.accessToken}")

        additionalHeaders?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val requestBody = if (body != null) {
                    gson.toJson(body).toRequestBody("application/json".toMediaType())
                } else {
                    "".toRequestBody("application/json".toMediaType())
                }
                requestBuilder.post(requestBody)
            }
            "PUT" -> {
                val requestBody = if (body != null) {
                    gson.toJson(body).toRequestBody("application/json".toMediaType())
                } else {
                    "".toRequestBody("application/json".toMediaType())
                }
                requestBuilder.put(requestBody)
            }
            "DELETE" -> {
                val requestBody = if (body != null) {
                    gson.toJson(body).toRequestBody("application/json".toMediaType())
                } else {
                    null
                }
                if (requestBody != null) {
                    requestBuilder.delete(requestBody)
                } else {
                    requestBuilder.delete()
                }
            }
            else -> throw MirimOAuthException("Unsupported HTTP method: $method")
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw MirimOAuthException(
                "Request failed",
                errorCode = response.code,
                data = response.body?.string()
            )
        }

        val responseBody = response.body?.string() ?: "{}"
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return@withContext gson.fromJson<Map<String, Any>>(responseBody, type)
    }

    private suspend fun getValidTokens(): AuthTokens {
        if (tokens == null || tokens!!.isExpired) {
            val storedTokenJson = withContext(Dispatchers.IO) {
                encryptedPrefs.getString(TOKEN_KEY, null)
            }
            if (storedTokenJson == null) {
                throw MirimOAuthException("Not logged in")
            }

            try {
                val storedTokens = gson.fromJson(storedTokenJson, AuthTokens::class.java)
                if (storedTokens.isExpired) {
                    return refreshTokens(storedTokens.refreshToken)
                }
                tokens = storedTokens
                return storedTokens
            } catch (e: Exception) {
                throw MirimOAuthException("Failed to get valid tokens: ${e.message}", cause = e)
            }
        }

        return tokens!!
    }

    private suspend fun getStoredUser(): MirimUser? = withContext(Dispatchers.IO) {
        val userJson = encryptedPrefs.getString(USER_KEY, null)
        if (userJson == null) {
            return@withContext null
        }

        try {
            return@withContext gson.fromJson(userJson, MirimUser::class.java)
        } catch (e: Exception) {
            throw MirimOAuthException("Failed to get user data: ${e.message}", cause = e)
        }
    }

    private fun startOAuthFlow() {
        val authUrl = buildAuthUrl()
        
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }

    private fun buildAuthUrl(): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to scopes.joinToString(","),
            "state" to "code"
        )

        val query = params.map { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }.joinToString("&")

        return "$oauthServerUrl/api/v1/oauth/authorize?$query"
    }

    private suspend fun exchangeCodeForTokens(code: String, state: String?): AuthTokens = withContext(Dispatchers.IO) {
        val requestBody = mapOf(
            "code" to code,
            "state" to state,
            "clientId" to clientId,
            "clientSecret" to clientSecret,
            "redirectUri" to redirectUri,
            "scopes" to scopes.joinToString(",")
        )

        val request = Request.Builder()
            .url("$oauthServerUrl/api/v1/oauth/token")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw MirimOAuthException(
                "Failed to exchange code for tokens",
                errorCode = response.code,
                data = response.body?.string()
            )
        }

        val responseBody = response.body?.string() ?: ""
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = gson.fromJson(responseBody, type)

        val status = (data["status"] as? Double)?.toInt() ?: 0
        if (status != 200) {
            throw MirimOAuthException(
                data["message"] as? String ?: "Token exchange failed",
                errorCode = status,
                data = data
            )
        }

        val tokenData = data["data"] as? Map<String, Any> ?: throw MirimOAuthException("Invalid token response")
        val tokens = AuthTokens(
            accessToken = tokenData["access_token"] as String,
            refreshToken = tokenData["refresh_token"] as String,
            expiresIn = (tokenData["expires_in"] as? Double)?.toInt() ?: 3600
        )

        // 토큰을 보안 저장소에 저장
        encryptedPrefs.edit()
            .putString(TOKEN_KEY, gson.toJson(tokens))
            .apply()

        return@withContext tokens
    }

    private suspend fun refreshTokens(refreshToken: String): AuthTokens = withContext(Dispatchers.IO) {
        val requestBody = mapOf("refreshToken" to refreshToken)

        val request = Request.Builder()
            .url("$oauthServerUrl/api/v1/auth/refresh")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw MirimOAuthException(
                "Token refresh failed",
                errorCode = response.code,
                data = response.body?.string()
            )
        }

        val responseBody = response.body?.string() ?: ""
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = gson.fromJson(responseBody, type)

        val status = (data["status"] as? Double)?.toInt() ?: 0
        if (status != 200) {
            throw MirimOAuthException(
                data["message"] as? String ?: "Token refresh failed",
                errorCode = status,
                data = data
            )
        }

        val tokenData = data["data"] as? Map<String, Any> ?: throw MirimOAuthException("Invalid refresh response")
        val tokens = AuthTokens(
            accessToken = tokenData["accessToken"] as String,
            refreshToken = refreshToken,
            expiresIn = (tokenData["expiresIn"] as? Double)?.toInt() ?: 3600
        )

        // 새로운 토큰을 보안 저장소에 저장
        encryptedPrefs.edit()
            .putString(TOKEN_KEY, gson.toJson(tokens))
            .apply()

        return@withContext tokens
    }

    private suspend fun fetchUserInfo(accessToken: String): MirimUser = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$oauthServerUrl/api/v1/user")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw MirimOAuthException(
                "Failed to fetch user info",
                errorCode = response.code,
                data = response.body?.string()
            )
        }

        val responseBody = response.body?.string() ?: ""
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = gson.fromJson(responseBody, type)

        val status = (data["status"] as? Double)?.toInt() ?: 0
        if (status != 200) {
            throw MirimOAuthException(
                data["message"] as? String ?: "Failed to fetch user info",
                errorCode = status,
                data = data
            )
        }

        val userData = data["data"] as? Map<String, Any> ?: throw MirimOAuthException("Invalid user response")
        val user = gson.fromJson(gson.toJson(userData), MirimUser::class.java)

        // 사용자 정보를 보안 저장소에 저장
        encryptedPrefs.edit()
            .putString(USER_KEY, gson.toJson(user))
            .apply()

        return@withContext user
    }

    private fun registerReceiver() {
        val filter = IntentFilter(OAuthRedirectActivity.OAUTH_CALLBACK_ACTION)
        context.registerReceiver(oauthReceiver, filter)
    }

    private fun unregisterReceiver() {
        try {
            context.unregisterReceiver(oauthReceiver)
        } catch (e: IllegalArgumentException) {
            // 이미 해제된 경우 무시
        }
    }
} 