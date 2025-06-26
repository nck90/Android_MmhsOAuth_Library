package kr.mmhs.oauth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * OAuth 리다이렉트를 처리하는 투명한 액티비티
 */
class OAuthRedirectActivity : Activity() {
    
    companion object {
        const val OAUTH_CALLBACK_ACTION = "kr.mmhs.oauth.OAUTH_CALLBACK"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val data: Uri? = intent?.data
        if (data != null) {
            // OAuth 콜백 데이터를 브로드캐스트로 전송
            val callbackIntent = Intent(OAUTH_CALLBACK_ACTION).apply {
                putExtra("uri", data.toString())
            }
            sendBroadcast(callbackIntent)
        }
        
        // 액티비티 종료
        finish()
    }
} 