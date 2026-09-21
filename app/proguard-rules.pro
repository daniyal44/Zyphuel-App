# Zyphuel Production ProGuard & R8 Optimization Rules (v2.6.4 Build 41)
# Comprehensive Reverse-Engineering Protection & Obfuscation Configuration

# -----------------------------------------------------------------------------
# 1. Code Obfuscation & Renaming Settings
# -----------------------------------------------------------------------------
-repackageclasses 'com.example.obf'
-allowaccessmodification
-keeppackagenames com.example.**

# Keep source file and line numbers for sanitized stack traces in crash logs
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# -----------------------------------------------------------------------------
# 2. Android Architecture Components & Room Database
# -----------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep all Room Entities, DAOs, and Database models
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * extends androidx.room.TypeConverter { *; }

# Keep data models used in local persistence and Firestore
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# -----------------------------------------------------------------------------
# 3. Security, Hardware Keystore & Biometrics
# -----------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class com.example.security.** { *; }
-keepclassmembers class com.example.security.** { *; }

# -----------------------------------------------------------------------------
# 4. Jetpack Compose & Material 3
# -----------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-keep class * extends androidx.compose.runtime.State
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# -----------------------------------------------------------------------------
# 5. Firebase, Google Play Services & Maps
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# -----------------------------------------------------------------------------
# 6. Kotlin Coroutines & Reflection
# -----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# -----------------------------------------------------------------------------
# 7. Network, JSON & Email Gateways
# -----------------------------------------------------------------------------
-keepattributes EnclosingMethod
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-dontwarn java.awt.**
-dontwarn javax.activation.**
