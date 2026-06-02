# ── General ───────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.greenrou.kanata.**$$serializer { *; }
-keepclassmembers class com.greenrou.kanata.** {
    *** Companion;
}
-keepclasseswithmembers class com.greenrou.kanata.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ── OkHttp + Okio ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * {
    @retrofit2.http.GET *;
    @retrofit2.http.POST *;
    @retrofit2.http.PUT *;
    @retrofit2.http.DELETE *;
    @retrofit2.http.HEAD *;
    @retrofit2.http.OPTIONS *;
    @retrofit2.http.HTTP *;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# ── Apollo GraphQL ────────────────────────────────────────────────────────────
-keep class com.greenrou.kanata.data.remote.anilist.** { *; }
-dontwarn com.apollographql.**
-keep class com.apollographql.** { *; }
-keep interface com.apollographql.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Jsoup ─────────────────────────────────────────────────────────────────────
-keep class org.jsoup.** { *; }
-dontwarn com.google.re2j.**

# ── Media3 / ExoPlayer ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* { <fields>; }
-dontwarn com.google.protobuf.**

# ── Koin ──────────────────────────────────────────────────────────────────────
-dontwarn org.koin.**

# ── WebView JS interface ──────────────────────────────────────────────────────
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class * extends android.webkit.WebViewClient { *; }
-keep class * extends android.webkit.WebChromeClient { *; }

# ── NewPipe Extractor ─────────────────────────────────────────────────────────
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**
-dontwarn javax.script.**

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.ListenableWorker { *; }
-dontwarn androidx.work.**

# ── Firebase / Crashlytics ────────────────────────────────────────────────────
# Crashlytics needs line numbers and source files to show readable stack traces
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# ── Mod API (must keep original names — extensions reference by name at runtime) ─
-keep interface com.greenrou.kanata.modapi.** { *; }
-keep class com.greenrou.kanata.modapi.** { *; }

# ── Kotlin types used in mod API suspend function signatures ──────────────────
-keep class kotlin.coroutines.** { *; }
-keepnames class kotlin.coroutines.**
-keep class kotlin.Result { *; }
-keep class kotlin.Result$Companion { *; }
-keep class kotlin.Result$Failure { *; }

# ── App domain / data models ──────────────────────────────────────────────────
-keep class com.greenrou.kanata.domain.model.** { *; }
-keep class com.greenrou.kanata.domain.parser.** { *; }
-keep class com.greenrou.kanata.data.local.** { *; }
-keep class com.greenrou.kanata.data.remote.dto.** { *; }
