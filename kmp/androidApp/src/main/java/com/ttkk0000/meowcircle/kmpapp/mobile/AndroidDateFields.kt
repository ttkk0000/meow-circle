package com.ttkk0000.meowcircle.kmpapp.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** 仅在日期与时间都确认后编辑草稿；取消任一步不写入半份日期，也不会提交。 */
@Composable internal fun DateTimeField(
    f: MobileFeatureStore, s: FeatureState, key: String, label: String, fallback: String = "",
    dateOnly: Boolean = false, optional: Boolean = false,
) {
    val context = LocalContext.current
    val value = f.field(key, fallback)
    val initial = runCatching {
        if (dateOnly) LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault())
        else OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
    }.getOrElse { ZonedDateTime.now() }
    var dateOpen by rememberSessionUiState("$key.dateOpen") { false }
    var pendingDay by rememberSessionUiState<LocalDate?>("$key.pendingDay") { null }
    var calendarDay by rememberSessionUiState("$key.calendarDay", generation = value) { initial.toLocalDate() }
    var pendingHour by rememberSessionUiState("$key.pendingHour", generation = value) { initial.hour }
    var pendingMinute by rememberSessionUiState("$key.pendingMinute", generation = value) { initial.minute }
    val enabled = !s.busy && !s.loading && s.confirmation == null && !s.unconfirmedResult
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { dateOpen = true }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Outlined.DateRange, null)
            Spacer(Modifier.width(12.dp))
            Text(if (value.isBlank()) "选择${if (dateOnly) "日期" else "日期与时间"}" else if (dateOnly) value else displayTime(value),
                Modifier.weight(1f))
        }
        if (!dateOnly) Text("按本机时区 ${ZoneId.systemDefault().id} 选择，确认页会显示实际时间。", style = MaterialTheme.typography.bodySmall)
        if (optional && value.isNotBlank()) TextButton({ f.edit(key, "") }, enabled = enabled,
            modifier = Modifier.align(Alignment.End)) { Text("清除日期") }
    }
    if (dateOpen) DisposableEffect(s.route, key) {
        val dialog = DatePickerDialog(context, { _, year, month, day ->
            val selected = LocalDate.of(year, month + 1, day)
            dateOpen = false
            if (dateOnly) f.edit(key, selected.toString()) else pendingDay = selected
        }, calendarDay.year, calendarDay.monthValue - 1, calendarDay.dayOfMonth)
        dialog.datePicker.init(calendarDay.year, calendarDay.monthValue - 1, calendarDay.dayOfMonth) { _, year, month, day ->
            calendarDay = LocalDate.of(year, month + 1, day)
        }
        if (dateOnly) dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.setOnDismissListener { dateOpen = false }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    }
    pendingDay?.let { day -> DisposableEffect(s.route, key, day) {
        val dialog = object : TimePickerDialog(context, { _, hour, minute ->
            f.edit(key, day.atTime(hour, minute).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
            pendingDay = null
        }, pendingHour, pendingMinute, DateFormat.is24HourFormat(context)) {
            override fun onTimeChanged(view: android.widget.TimePicker, hourOfDay: Int, minute: Int) {
                super.onTimeChanged(view, hourOfDay, minute)
                // Constructor callbacks must not touch captured state before attachment.
                if (view.isAttachedToWindow) { pendingHour = hourOfDay; pendingMinute = minute }
            }
        }
        dialog.setOnDismissListener { pendingDay = null }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    } }
}

/** 宽表单按可读宽度排两列，窄窗／大字体保持一列；不改变字段的状态 owner。 */
@Composable internal fun FormPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val currentFirst by rememberUpdatedState(first)
    val currentSecond by rememberUpdatedState(second)
    val firstContent = remember { movableContentOf { currentFirst() } }
    val secondContent = remember { movableContentOf { currentSecond() } }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / scale >= MobileUi.formPairWidth) Row(horizontalArrangement = Arrangement.spacedBy(MobileUi.sectionGap)) {
            Column(Modifier.weight(1f)) { firstContent() }; Column(Modifier.weight(1f)) { secondContent() }
        } else Column(verticalArrangement = Arrangement.spacedBy(MobileUi.contentGap)) { firstContent(); secondContent() }
    }
}
