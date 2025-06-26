package kr.mmhs.oauth

class MirimOAuthException(
    message: String,
    val errorCode: Int? = null,
    val data: Any? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    override fun toString(): String {
        return "MirimOAuthException: $message ${errorCode?.let { "(Code: $it)" } ?: ""}"
    }
} 