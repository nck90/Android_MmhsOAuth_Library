package kr.mmhs.oauth

import org.junit.Test
import org.junit.Assert.*

/**
 * MirimOAuth 라이브러리 단위 테스트
 */
class MirimOAuthTest {

    @Test
    fun testMirimUserDataClass() {
        val user = MirimUser(
            id = "test_id",
            email = "test@example.com",
            nickname = "테스트",
            major = "소프트웨어개발과",
            isGraduated = false,
            admission = "2024",
            role = "student",
            generation = 1
        )

        assertEquals("test_id", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("테스트", user.nickname)
        assertEquals("소프트웨어개발과", user.major)
        assertEquals(false, user.isGraduated)
        assertEquals("2024", user.admission)
        assertEquals("student", user.role)
        assertEquals(1, user.generation)
    }

    @Test
    fun testAuthTokensExpiration() {
        val tokens = AuthTokens(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresIn = -1 // 이미 만료된 토큰
        )

        assertTrue(tokens.isExpired)
    }

    @Test
    fun testAuthTokensNotExpired() {
        val tokens = AuthTokens(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresIn = 3600 // 1시간 후 만료
        )

        assertFalse(tokens.isExpired)
    }

    @Test
    fun testMirimOAuthException() {
        val exception = MirimOAuthException(
            message = "Test error",
            errorCode = 400,
            data = mapOf("error" to "test_error")
        )

        assertEquals("Test error", exception.message)
        assertEquals(400, exception.errorCode)
        assertTrue(exception.toString().contains("MirimOAuthException"))
        assertTrue(exception.toString().contains("Test error"))
        assertTrue(exception.toString().contains("400"))
    }
} 