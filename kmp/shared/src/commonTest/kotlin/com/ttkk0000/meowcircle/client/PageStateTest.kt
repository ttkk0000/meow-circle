package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** Regression source only: compilation and test execution have not been authorized. */
class PageStateTest {
    private fun row(id: Long) = MobileRecord(id, 1, 1, "active", "record-$id")
    private val loaded = PageState(listOf(row(1)), next = 10, loaded = true)

    @Test fun refreshRetainsRowsUntilReplacementArrives() {
        val pending = loaded.starting(false)
        assertEquals(loaded.items, pending.items)
        assertTrue(pending.refreshing)
        assertFalse(pending.canLoadMore)
        val result = pending.completed(MobilePage(listOf(row(2))), false)
        assertEquals(listOf(2L), result.items.map { it.id })
        assertTrue(result.exhausted)
    }

    @Test fun failedAppendRetainsTheOriginalCursorAndRows() {
        val failed = loaded.starting(true).failed("连接中断")
        assertEquals(10L, failed.next)
        assertEquals(loaded.items, failed.items)
        assertTrue(failed.appending)
        assertTrue(failed.canLoadMore)
        assertFalse(failed.empty)
        assertFalse(failed.exhausted)
        val retry = failed.starting(true)
        assertNull(retry.error)
        assertTrue(retry.loading)
        val result = retry.completed(MobilePage(listOf(row(1), row(2))), true)
        assertEquals(listOf(1L, 2L), result.items.map { it.id })
        assertTrue(result.exhausted)
        assertFalse(result.appending)
    }

    @Test fun refreshFailureRequiresRefreshBeforeAppending() {
        val failed = loaded.starting(false).failed("连接中断")
        assertEquals(loaded.items, failed.items)
        assertFalse(failed.appending)
        assertFalse(failed.canLoadMore)
        assertTrue(failed.starting(false).completed(MobilePage(listOf(row(3)), 20), false).canLoadMore)
    }

    @Test fun revokedAccessClearsRowsCursorAndAppendRetry() {
        val failed = loaded.starting(true).failed("没有访问权限", clear = true)
        assertTrue(failed.items.isEmpty())
        assertEquals(0L, failed.next)
        assertFalse(failed.loaded)
        assertFalse(failed.appending)
        assertFalse(failed.empty)
        assertFalse(failed.exhausted)
    }

    @Test fun filteredEmptyPageWithAnotherCursorIsNotAnEmptyResult() {
        val page = PageState<String>(loaded = true, next = 50)
        assertFalse(page.empty)
        assertFalse(page.exhausted)
        assertTrue(page.canLoadMore)
    }

    @Test fun onlySettledSuccessfulPagesDisplayEmptyOrNoMore() {
        assertFalse(PageState<String>().empty)
        assertTrue(PageState<String>(loaded = true).empty)
        assertFalse(loaded.starting(false).exhausted)
        assertFalse(PageState<String>(loaded = true, error = "失败").empty)
    }

    @Test fun nonAdvancingCursorCannotReplaceExistingContent() {
        assertFailsWith<IllegalStateException> { loaded.completed(MobilePage(listOf(row(2)), 10), true) }
        assertEquals(listOf(1L), loaded.items.map { it.id })
        assertEquals(10L, loaded.next)
    }
}
