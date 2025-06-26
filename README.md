<div align="center">
  <h1>🔐 미림마이스터고 OAuth 안드로이드 라이브러리</h1>
  <p>미림마이스터고 학생들을 위한 OAuth 인증 서비스의 안드로이드 네이티브 라이브러리입니다</p>
  <p>
    <a href="https://api-auth.mmhs.kr/">
      <img src="https://img.shields.io/badge/API-oauth.mmhs.kr%2Fapi-5E81F4?style=flat-square" alt="API" />
    </a>  
    <a href="https://github.com/nck90/React.js_MmhsOAuth_Client">
      <img src="https://img.shields.io/badge/GitHub-Frontend-FF6B6B?style=flat-square&logo=github" alt="GitHub Frontend" />
    </a>
    <a href="https://github.com/nck90/Nest.js_MmhsOAuth_Server">
      <img src="https://img.shields.io/badge/GitHub-Backend-6BCB77?style=flat-square&logo=github" alt="GitHub Backend" />
    </a>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  </p>
</div>

## 📝 프로젝트 소개

미림마이스터고 OAuth 안드로이드는 교내 앱 개발자들이 간편하게 미림마이스터고 인증 시스템을 연동할 수 있도록 제공되는 안드로이드 네이티브 라이브러리입니다. OAuth 2.0 프로토콜을 기반으로 안전하고 편리한 인증 기능을 구현할 수 있습니다.

## 🛠️ 기술 스택

- **언어**: Kotlin
- **플랫폼**: Android (API 21+)
- **HTTP 클라이언트**: OkHttp3, Retrofit2
- **보안**: EncryptedSharedPreferences
- **비동기 처리**: Kotlin Coroutines

## 📦 설치 방법

### 1. JitPack을 통한 설치 (추천)

프로젝트 루트의 `build.gradle` 파일에 JitPack 리포지토리를 추가하세요:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

앱 모듈의 `build.gradle` 파일에 의존성을 추가하세요:

```gradle
dependencies {
    implementation 'com.github.nck90:Android_MmhsOAuth_Library:1.0.0'
}
```

### 2. 로컬 라이브러리로 설치

1. 이 레포지토리를 클론하세요
2. 프로젝트를 안드로이드 스튜디오에서 열고 빌드하세요
3. 생성된 AAR 파일을 앱 프로젝트에 포함시키세요

## 🚀 사용 방법

### 1. 기본 설정

```kotlin
import kr.mmhs.oauth.MirimOAuth

class MainActivity : AppCompatActivity() {
    private lateinit var mirimOAuth: MirimOAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // MirimOAuth 인스턴스 생성
        mirimOAuth = MirimOAuth(
            context = this,
            clientId = "YOUR_CLIENT_ID",
            clientSecret = "YOUR_CLIENT_SECRET",
            redirectUri = "mirim-oauth://callback",
            scopes = listOf("profile", "email")
        )
    }
}
```

### 2. 로그인

```kotlin
// 코루틴을 사용한 로그인
lifecycleScope.launch {
    try {
        val user = mirimOAuth.logIn()
        // 로그인 성공
        Log.d("OAuth", "로그인 성공: ${user.email}")
    } catch (e: Exception) {
        // 로그인 실패
        Log.e("OAuth", "로그인 실패", e)
    }
}
```

### 3. 로그인 상태 확인

```kotlin
lifecycleScope.launch {
    if (mirimOAuth.checkIsLoggedIn()) {
        val user = mirimOAuth.getCurrentUser()
        Log.d("OAuth", "이미 로그인됨: ${user?.email}")
    } else {
        Log.d("OAuth", "로그인 필요")
    }
}
```

### 4. 사용자 정보 조회

```kotlin
val currentUser = mirimOAuth.getCurrentUser()
currentUser?.let {
    Log.d("OAuth", "사용자 ID: ${it.id}")
    Log.d("OAuth", "이메일: ${it.email}")
    Log.d("OAuth", "닉네임: ${it.nickname}")
    Log.d("OAuth", "전공: ${it.major}")
    Log.d("OAuth", "졸업 여부: ${it.isGraduated}")
}
```

### 5. 인증된 API 호출

```kotlin
lifecycleScope.launch {
    try {
        val response = mirimOAuth.makeAuthenticatedRequest(
            endpoint = "/api/v1/profile",
            method = "GET"
        )
        Log.d("OAuth", "API 응답: $response")
    } catch (e: Exception) {
        Log.e("OAuth", "API 호출 실패", e)
    }
}
```

### 6. 로그아웃

```kotlin
lifecycleScope.launch {
    mirimOAuth.logOut()
    Log.d("OAuth", "로그아웃 완료")
}
```

## 📱 AndroidManifest.xml 설정

앱의 `AndroidManifest.xml`에 다음 설정을 추가하세요:

```xml
<application>
    <!-- OAuth 리다이렉트 처리를 위한 액티비티 -->
    <activity
        android:name="kr.mmhs.oauth.OAuthRedirectActivity"
        android:exported="true"
        android:launchMode="singleTop"
        android:theme="@android:style/Theme.Translucent.NoTitleBar">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <!-- 여기서 scheme를 앱에 맞게 변경하세요 -->
            <data android:scheme="your-app-scheme" />
        </intent-filter>
    </activity>
</application>
```

## 🔧 고급 사용법

### 커스텀 OAuth 서버 URL

```kotlin
val mirimOAuth = MirimOAuth(
    context = this,
    clientId = "YOUR_CLIENT_ID",
    clientSecret = "YOUR_CLIENT_SECRET",
    redirectUri = "your-scheme://callback",
    scopes = listOf("profile", "email"),
    oauthServerUrl = "https://custom-oauth.example.com"
)
```

### 토큰 새로고침

```kotlin
lifecycleScope.launch {
    try {
        val accessToken = mirimOAuth.getAccessToken()
        Log.d("OAuth", "액세스 토큰: $accessToken")
    } catch (e: Exception) {
        Log.e("OAuth", "토큰 가져오기 실패", e)
    }
}
```

## 📋 API 문서

### MirimOAuth 클래스

| 메서드 | 설명 | 반환 타입 |
|--------|------|-----------|
| `logIn()` | OAuth 로그인 수행 | `suspend fun(): MirimUser` |
| `logOut()` | 로그아웃 | `suspend fun(): Unit` |
| `getCurrentUser()` | 현재 사용자 정보 조회 | `fun(): MirimUser?` |
| `isLoggedIn()` | 로그인 상태 확인 | `fun(): Boolean` |
| `checkIsLoggedIn()` | 로그인 상태 확인 및 토큰 갱신 | `suspend fun(): Boolean` |
| `getAccessToken()` | 액세스 토큰 조회 | `suspend fun(): String` |
| `refreshUserInfo()` | 사용자 정보 새로고침 | `suspend fun(): MirimUser` |
| `makeAuthenticatedRequest()` | 인증된 HTTP 요청 | `suspend fun(): Map<String, Any>` |

### MirimUser 데이터 클래스

```kotlin
data class MirimUser(
    val id: String,
    val email: String,
    val nickname: String? = null,
    val major: String? = null,
    val isGraduated: Boolean? = null,
    val admission: String? = null,
    val role: String? = null,
    val generation: Int? = null
)
```

## ⚠️ 주의사항

1. **보안**: `clientSecret`은 안전하게 관리하세요. 가능하면 서버 사이드에서 처리하는 것을 권장합니다.
2. **권한**: 인터넷 권한이 필요합니다.
3. **리다이렉트 URI**: OAuth 서버에 등록된 리다이렉트 URI와 정확히 일치해야 합니다.
4. **스킴**: 커스텀 URL 스킴은 다른 앱과 충돌하지 않도록 고유해야 합니다.

## 🐛 문제 해결

### 일반적인 문제들

1. **OAuth 콜백이 작동하지 않는 경우**
   - AndroidManifest.xml의 intent-filter 설정을 확인하세요
   - 리다이렉트 URI가 OAuth 서버에 등록되어 있는지 확인하세요

2. **네트워크 보안 정책 오류**
   - `network_security_config.xml`을 설정하거나 HTTPS를 사용하세요

3. **프로가드 관련 문제**
   - 제공된 proguard 규칙을 적용하세요

## 🔗 링크 & 소셜

- [프로젝트 웹사이트](https://auth.mmhs.app)
- [Flutter 버전](https://github.com/nck90/Flutter_MmhsOAuth_Library)
- [개발 블로그](https://velog.io/@kaje033/Project-Mirim-OAuth%EA%B0%80-%EB%AD%94%EB%8D%B0-0-%EA%B0%9C%EC%9A%94)
- [Hyphen team](https://github.com/HyphenDev)
- [Instagram](https://www.instagram.com/hyphen_team)

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 🤝 기여하기

버그 리포트, 기능 요청, 풀 리퀘스트는 언제나 환영합니다!

---

<div align="center">
  <p>Made with ❤️ by <a href="https://github.com/HyphenDev">Hyphen Team</a></p>
</div> 