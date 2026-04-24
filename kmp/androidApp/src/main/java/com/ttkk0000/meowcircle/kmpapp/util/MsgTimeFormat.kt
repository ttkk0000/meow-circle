package com.ttkk0000.meowcircle.kmpapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** MOBILE「消息」列表右侧时间：今天显示时刻，昨天 / 本周 / 否则短日期。 */
fun formatConversationListTime(iso: String): String =
    runCatching {
        val zone = ZoneId.systemDefault()
        val zdt = Instant.parse(iso).atZone(zone)
        val day = zdt.toLocalDate()
        val now = LocalDate.now(zone)
        val yesterday = now.minusDays(1)
        val weekdayFmt = DateTimeFormatter.ofPattern("EEE").withLocale(Locale.SIMPLIFIED_CHINESE)
        when {
            day == now ->
                DateTimeFormatter.ofPattern("HH:mm").format(zdt)
            day == yesterday -> "昨天"
            ChronoUnit.DAYS.between(day, now) < 7 ->
                weekdayFmt.format(zdt)
            else -> DateTimeFormatter.ofPattern("MM-dd").format(zdt)
        }
    }.getOrElse { iso.take(10) }
