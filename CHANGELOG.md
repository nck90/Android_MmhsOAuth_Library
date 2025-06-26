# Changelog

모든 주목할 만한 변경사항은 이 파일에 문서화됩니다.

## [1.0.0] - 2024-12-20

### Added
- 🎉 안드로이드 네이티브 미림마이스터고 OAuth 라이브러리 첫 릴리스
- OAuth 2.0 인증 플로우 구현
- 보안 토큰 저장 (EncryptedSharedPreferences 사용)
- 자동 토큰 갱신 기능
- 사용자 정보 관리 기능
- 인증된 HTTP 요청 지원
- Custom Tabs를 통한 브라우저 기반 인증
- Kotlin Coroutines 지원
- ProGuard 규칙 포함
- 포괄적인 단위 테스트
- 상세한 문서화

### Features
- **MirimOAuth**: 메인 OAuth 클래스
- **MirimUser**: 사용자 정보 데이터 클래스
- **AuthTokens**: 토큰 관리 데이터 클래스
- **MirimOAuthException**: 커스텀 예외 처리
- **OAuthRedirectActivity**: OAuth 리다이렉트 처리

### Security
- AES256_GCM 암호화를 통한 보안 토큰 저장
- 자동 토큰 만료 처리
- 안전한 HTTP 통신 (HTTPS 강제)

### Documentation
- 상세한 README.md
- API 문서화
- 사용 예제 제공
- 문제 해결 가이드

### Technical Stack
- Kotlin
- Android API 21+
- OkHttp3 & Retrofit2
- Gson
- EncryptedSharedPreferences
- Custom Tabs
- Kotlin Coroutines

---

## 향후 계획

### [1.1.0] - 예정
- Biometric 인증 지원
- 오프라인 모드 개선
- 추가 에러 핸들링
- 성능 최적화

### [1.2.0] - 예정  
- 다중 계정 지원
- 커스텀 브라우저 지원
- 고급 보안 옵션
- WebView 대체 인증 플로우

---

**Format**: [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
**Types**: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security` 