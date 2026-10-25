package com.ttkk0000.meowcircle.clientui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual fun dateTime(value: String): String? = runCatching { OffsetDateTime.parse(value.trim()).toString() }.getOrNull()
actual fun displayTime(value: String): String = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx")) }.getOrDefault(value)
actual fun previousGrapheme(text: String, offset: Int): Int {
    val boundary = android.icu.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT)
    boundary.setText(text)
    return boundary.preceding(offset.coerceIn(0,text.length)).takeIf { it != android.icu.text.BreakIterator.DONE } ?: 0
}

actual fun futureTime(value: String): Boolean = runCatching { OffsetDateTime.parse(value).toInstant().isAfter(java.time.Instant.now()) }.getOrDefault(false)
actual fun validDay(value: String): Boolean = runCatching { java.time.LocalDate.parse(value) }.isSuccess
