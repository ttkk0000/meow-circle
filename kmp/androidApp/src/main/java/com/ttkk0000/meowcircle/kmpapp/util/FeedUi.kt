package com.ttkk0000.meowcircle.kmpapp.util

import kotlin.math.floor

fun categoryLabel(c: String): String =
    when (c) {
        "daily_share" -> "日常"
        "help" -> "求助"
        "activity" -> "活动"
        "trade" -> "交易"
        else -> c
    }

fun formatCompactCount(n: Long): String {
    val x = floor(n.toDouble()).toInt().coerceAtLeast(0)
    if (x >= 10_000) {
        val v = x / 10_000.0
        val s = String.format("%.1f", v).replace(Regex("\\.0$"), "")
        return "${s}w"
    }
    if (x >= 1_000) {
        val v = x / 1_000.0
        val s = String.format("%.1f", v).replace(Regex("\\.0$"), "")
        return "${s}k"
    }
    return x.toString()
}
