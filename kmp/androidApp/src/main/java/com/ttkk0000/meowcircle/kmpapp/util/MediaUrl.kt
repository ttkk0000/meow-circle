package com.ttkk0000.meowcircle.kmpapp.util

fun resolveMediaUrl(
    apiBase: String,
    url: String?,
): String? {
    val u = url?.trim().orEmpty()
    if (u.isEmpty()) return null
    if (u.startsWith("http://") || u.startsWith("https://")) return u
    val root = apiBase.trimEnd('/')
    return if (u.startsWith("/")) "$root$u" else "$root/$u"
}
