package com.greenrou.kanata.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

fun Throwable.reportToCrashlytics(context: String) {
    FirebaseCrashlytics.getInstance().apply {
        setCustomKey("error_context", context)
        recordException(this@reportToCrashlytics)
    }
}
