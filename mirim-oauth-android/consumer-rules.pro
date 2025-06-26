# Consumer ProGuard rules for MirimOAuth library

# Keep public API classes and methods
-keep public class kr.mmhs.oauth.MirimOAuth {
    public *;
}

-keep public class kr.mmhs.oauth.MirimUser {
    public *;
}

-keep public class kr.mmhs.oauth.AuthTokens {
    public *;
}

-keep public class kr.mmhs.oauth.MirimOAuthException {
    public *;
}

# Keep data class properties for JSON serialization
-keepclassmembers class kr.mmhs.oauth.MirimUser {
    <fields>;
}

-keepclassmembers class kr.mmhs.oauth.AuthTokens {
    <fields>;
}

# Gson related
-keepattributes Signature
-keepattributes *Annotation* 