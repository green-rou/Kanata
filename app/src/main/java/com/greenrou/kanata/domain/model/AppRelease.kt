package com.greenrou.kanata.domain.model

data class AppRelease(
    val version: String,
    val title: String,
    val body: String,
    val apkUrl: String,
    val pageUrl: String,
)
