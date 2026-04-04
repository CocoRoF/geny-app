# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.geny.app.data.dto.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
