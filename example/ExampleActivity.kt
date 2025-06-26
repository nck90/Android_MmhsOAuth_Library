package com.example.mirimauth

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kr.mmhs.oauth.MirimOAuth
import kr.mmhs.oauth.MirimOAuthException
import kotlinx.coroutines.launch

/**
 * MirimOAuth 라이브러리 사용 예제
 */
class ExampleActivity : AppCompatActivity() {

    private lateinit var mirimOAuth: MirimOAuth
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var userInfoText: TextView
    private lateinit var apiTestButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        initViews()
        initMirimOAuth()
        setupClickListeners()
        checkLoginStatus()
    }

    private fun initViews() {
        loginButton = findViewById(R.id.btnLogin)
        logoutButton = findViewById(R.id.btnLogout)
        userInfoText = findViewById(R.id.tvUserInfo)
        apiTestButton = findViewById(R.id.btnApiTest)
    }

    private fun initMirimOAuth() {
        mirimOAuth = MirimOAuth(
            context = this,
            clientId = "your_client_id_here",
            clientSecret = "your_client_secret_here",
            redirectUri = "your-app-scheme://callback",
            scopes = listOf("profile", "email"),
            oauthServerUrl = "https://api-auth.mmhs.app"
        )
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            performLogin()
        }

        logoutButton.setOnClickListener {
            performLogout()
        }

        apiTestButton.setOnClickListener {
            testApiCall()
        }
    }

    private fun checkLoginStatus() {
        lifecycleScope.launch {
            try {
                if (mirimOAuth.checkIsLoggedIn()) {
                    updateUIForLoggedInUser()
                } else {
                    updateUIForLoggedOutUser()
                }
            } catch (e: Exception) {
                Log.e("OAuth", "로그인 상태 확인 실패", e)
                updateUIForLoggedOutUser()
            }
        }
    }

    private fun performLogin() {
        loginButton.isEnabled = false
        loginButton.text = "로그인 중..."

        lifecycleScope.launch {
            try {
                val user = mirimOAuth.logIn()
                Log.d("OAuth", "로그인 성공: ${user.email}")
                
                Toast.makeText(this@ExampleActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                updateUIForLoggedInUser()
                
            } catch (e: MirimOAuthException) {
                Log.e("OAuth", "OAuth 로그인 실패", e)
                Toast.makeText(this@ExampleActivity, "로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
                updateUIForLoggedOutUser()
                
            } catch (e: Exception) {
                Log.e("OAuth", "예상치 못한 로그인 오류", e)
                Toast.makeText(this@ExampleActivity, "로그인 오류가 발생했습니다", Toast.LENGTH_LONG).show()
                updateUIForLoggedOutUser()
            }
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                mirimOAuth.logOut()
                Log.d("OAuth", "로그아웃 성공")
                
                Toast.makeText(this@ExampleActivity, "로그아웃 완료", Toast.LENGTH_SHORT).show()
                updateUIForLoggedOutUser()
                
            } catch (e: Exception) {
                Log.e("OAuth", "로그아웃 실패", e)
                Toast.makeText(this@ExampleActivity, "로그아웃 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testApiCall() {
        apiTestButton.isEnabled = false
        apiTestButton.text = "API 호출 중..."

        lifecycleScope.launch {
            try {
                // 사용자 정보 새로고침
                val user = mirimOAuth.refreshUserInfo()
                Log.d("OAuth", "사용자 정보 새로고침 성공: ${user.email}")

                // 커스텀 API 호출 예제
                val response = mirimOAuth.makeAuthenticatedRequest(
                    endpoint = "/api/v1/user",
                    method = "GET"
                )
                Log.d("OAuth", "API 응답: $response")

                Toast.makeText(this@ExampleActivity, "API 호출 성공", Toast.LENGTH_SHORT).show()
                updateUserInfo(user)
                
            } catch (e: MirimOAuthException) {
                Log.e("OAuth", "API 호출 실패", e)
                Toast.makeText(this@ExampleActivity, "API 호출 실패: ${e.message}", Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                Log.e("OAuth", "예상치 못한 API 오류", e)
                Toast.makeText(this@ExampleActivity, "API 오류가 발생했습니다", Toast.LENGTH_LONG).show()
                
            } finally {
                apiTestButton.isEnabled = true
                apiTestButton.text = "API 테스트"
            }
        }
    }

    private fun updateUIForLoggedInUser() {
        loginButton.isEnabled = false
        loginButton.text = "로그인됨"
        
        logoutButton.isEnabled = true
        apiTestButton.isEnabled = true

        val user = mirimOAuth.getCurrentUser()
        updateUserInfo(user)
    }

    private fun updateUIForLoggedOutUser() {
        loginButton.isEnabled = true
        loginButton.text = "로그인"
        
        logoutButton.isEnabled = false
        apiTestButton.isEnabled = false

        userInfoText.text = "로그인이 필요합니다"
    }

    private fun updateUserInfo(user: kr.mmhs.oauth.MirimUser?) {
        if (user != null) {
            val userInfo = buildString {
                appendLine("👤 사용자 정보")
                appendLine("ID: ${user.id}")
                appendLine("이메일: ${user.email}")
                user.nickname?.let { appendLine("닉네임: $it") }
                user.major?.let { appendLine("전공: $it") }
                user.isGraduated?.let { appendLine("졸업 여부: ${if (it) "졸업" else "재학"}") }
                user.admission?.let { appendLine("입학년도: $it") }
                user.role?.let { appendLine("역할: $it") }
                user.generation?.let { appendLine("기수: ${it}기") }
            }
            userInfoText.text = userInfo
        } else {
            userInfoText.text = "사용자 정보를 불러올 수 없습니다"
        }
    }

    companion object {
        private const val TAG = "ExampleActivity"
    }
} 