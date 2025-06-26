package kr.mmhs.oauth

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * OAuth 인증 토큰을 담는 데이터 클래스
 */
data class AuthTokens(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("refresh_token")
    val refreshToken: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    @SerializedName("issued_at")
    val issuedAt: Date = Date()
) {
    /**
     * 토큰이 만료되었는지 확인
     */
    val isExpired: Boolean
        get() {
            val expirationDate = Date(issuedAt.time + (expiresIn * 1000L))
            return Date().after(expirationDate)
        }
} 