# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Ktor & OkHttp
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**

# MapLibre
-dontwarn org.maplibre.**
-keep class org.maplibre.** { *; }

# SQLDelight
-dontwarn app.cash.sqldelight.**
-keep class com.dabber.traveldabble.db.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Multiplatform Settings
-dontwarn com.russhwolf.settings.**
-keep class com.russhwolf.settings.** { *; }
