package com.greenrou.kanata.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

fun Throwable.reportToCrashlytics(context: String) {
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("error_context", context)
        setCustomKey("error_type", this@reportToCrashlytics::class.simpleName ?: "Unknown")
        setCustomKey("error_message", (this@reportToCrashlytics.message ?: "").take(200))
        setCustomKey("error_location", this@reportToCrashlytics.findAppFrame())
        recordException(this@reportToCrashlytics)
    }
}

private fun Throwable.findAppFrame(): String =
    stackTrace
        .firstOrNull { it.className.startsWith("com.greenrou.kanata") }
        ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
        ?: stackTrace.firstOrNull()
            ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" }
        ?: "unknown"
