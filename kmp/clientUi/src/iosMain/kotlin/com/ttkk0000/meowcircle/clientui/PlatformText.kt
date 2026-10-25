@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ttkk0000.meowcircle.clientui

import kotlinx.cinterop.useContents
import platform.Foundation.*

private fun instant(value: String): NSDate? {
    val formatter = NSISO8601DateFormatter()
    formatter.formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
    return formatter.dateFromString(value) ?: NSISO8601DateFormatter().dateFromString(value)
}
actual fun dateTime(value: String): String? = value.trim().takeIf { instant(it) != null }
actual fun displayTime(value: String): String = instant(value)?.let { date -> NSDateFormatter().apply {
    locale = NSLocale(localeIdentifier = "en_US_POSIX"); dateFormat = "yyyy-MM-dd HH:mm XXX"
}.stringFromDate(date) } ?: value
actual fun previousGrapheme(text: String, offset: Int): Int {
    val end = offset.coerceIn(0,text.length)
    if(end == 0) return 0
    return NSString.create(string = text).rangeOfComposedCharacterSequenceAtIndex((end - 1).toULong()).useContents { location.toInt() }
}

actual fun futureTime(value: String): Boolean = instant(value)?.let { it.timeIntervalSince1970 > NSDate().timeIntervalSince1970 } ?: false
actual fun validDay(value: String): Boolean {
    val formatter = NSDateFormatter().apply { locale = NSLocale(localeIdentifier = "en_US_POSIX"); dateFormat = "yyyy-MM-dd"; lenient = false }
    val date = formatter.dateFromString(value) ?: return false
    return formatter.stringFromDate(date) == value
}
