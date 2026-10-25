package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.client.PageState

@Composable fun LoadingStatus(label: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = MobileUi.contentGap).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun <T> PageRefresh(page: PageState<T>) {
    if (page.refreshing) LoadingStatus("正在刷新，保留当前内容…")
}

/** 同一个列表只显示当前请求对应的动作；翻页失败沿原游标重试。 */
@Composable fun <T> PageFooter(page: PageState<T>, retry: () -> Unit, more: () -> Unit,
    emptyText: String = "暂时没有可显示的内容", refreshVisible: Boolean = false, enabled: Boolean = true) {
    if (refreshVisible && page.refreshing) return
    val error = page.error
    Column(Modifier.fillMaxWidth().padding(vertical = MobileUi.contentGap), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
        when {
            page.loading -> LoadingStatus(when {
                page.appending -> "正在加载下一页…"
                page.refreshing -> "正在刷新，保留当前内容…"
                else -> "正在加载…"
            })
            error != null -> {
                val title = when {
                    page.appending -> "下一页加载失败"
                    page.loaded -> "刷新失败，当前内容已保留"
                    else -> "暂时无法加载"
                }
                Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(MobileUi.compactGap)) {
                    Text(title, color = MaterialTheme.colorScheme.error)
                    ReadFailureContent(page.failure, error, if (page.appending) more else retry, page.readAt, enabled)
                }
            }
            page.empty -> EmptyResults(title = emptyText, detail = "当前条件下没有可显示的记录。可以调整筛选或稍后重新读取；不会混入其它范围的内容。")
            page.exhausted -> Text("没有更多了", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            page.canLoadMore -> MndSecondaryButton("加载下一页", enabled, modifier = Modifier, onClick = more)
            !page.loaded -> MndSecondaryButton("加载内容", enabled, modifier = Modifier, onClick = retry)
        }
    }
}
