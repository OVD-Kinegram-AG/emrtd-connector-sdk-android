# These are compile-only annotation classes pulled in by OpenTelemetry.
# They’re not used at runtime, so we let R8 ignore any missing references.
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**

# Keep classes that BouncyCastle accesses using reflection
# Fixes
# > java.security.NoSuchAlgorithmException: no such algorithm: ISO9797Alg3Mac for provider BC
# and similar errors
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-dontwarn javax.naming.**

# We have to access some fields in the jMRTD library using reflection because
# they are private. These should not be renamed.
-keep class org.jmrtd.DefaultFileSystem { *; }
-keepclassmembers class org.jmrtd.PassportService {
    private org.jmrtd.DefaultFileSystem *;
}
