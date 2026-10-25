package com.ttkk0000.meowcircle.client

import kotlin.test.*

/** 仅回归源码；本任务未获编译或测试执行授权。 */
class ClientReadStateTest {
    @Test fun connectionFailureKeepsRowsCursorAndOriginalReadTime() {
        val row = MobileRecord(1, 2, 3, "active", "猫咪日常")
        val loaded = PageState(listOf(row), next = 8, loaded = true, readAt = "2026-10-25T10:00:00Z")
        val failed = loaded.starting(true).failed("连接中断", failure = ClientReadFailure.Connection)
        assertEquals(loaded.items, failed.items)
        assertEquals(8L, failed.next)
        assertEquals(loaded.readAt, failed.readAt)
        assertFalse(failed.empty)
        assertTrue(failed.canLoadMore)
        val retry = failed.starting(true)
        assertNull(retry.failure)
        assertEquals(loaded.readAt, retry.readAt)
    }

    @Test fun revokedAccessDoesNotKeepPrivateRowsOrTheirTimestamp() {
        val loaded = PageState(listOf(MobileRecord(1, 2, 3, "active", "私密内容")), next = 8, loaded = true, readAt = "2026-10-25T10:00:00Z")
        val denied = loaded.failed("没有权限", clear = true, failure = ClientReadFailure.Forbidden)
        assertTrue(denied.items.isEmpty())
        assertNull(denied.readAt)
        assertEquals(ClientReadFailure.Forbidden, denied.failure)
        assertFalse(denied.empty)
        assertFalse(denied.canLoadMore)
    }

    @Test fun providerAndServerFailuresAreNotMisreportedAsOffline() {
        assertEquals(ClientReadFailure.Connection, MobileConnectionException(Exception()).readFailure())
        assertEquals(ClientReadFailure.SignIn, MobileApiException(401, "expired").readFailure())
        assertEquals(ClientReadFailure.Forbidden, MobileApiException(403, "denied").readFailure())
        assertEquals(ClientReadFailure.Unavailable, MobileApiException(404, "missing").readFailure())
        assertEquals(ClientReadFailure.Unavailable, MobileApiException(410, "gone").readFailure())
        assertEquals(ClientReadFailure.Busy, MobileApiException(429, "limited").readFailure())
        assertEquals(ClientReadFailure.Service, MobileApiException(503, "not configured").readFailure())
        assertEquals(ClientReadFailure.Service, MobileStorageException().readFailure())
    }

    @Test fun notificationReferencesKeep64BitPrecisionAndRejectAmbiguousValues() {
        assertEquals(9_007_199_254_740_993L, canonicalNotificationId("9007199254740993"))
        assertEquals(Long.MAX_VALUE, canonicalNotificationId(Long.MAX_VALUE.toString()))
        for (raw in listOf("0", "-1", "+1", "01", "1.0", "1e3", " 1", "1 ", "", "9223372036854775808", "https://other.example/1", "1?confirm=true")) {
            assertNull(canonicalNotificationId(raw), raw)
        }
    }

    @Test fun transmittedBytesDoNotMeanServerAcceptedThePhoto() {
        assertNull(uploadPercentage(100, null))
        assertNull(uploadPercentage(100, 0))
        assertEquals(0, uploadPercentage(-1, 100))
        assertEquals(60, uploadPercentage(60, 100))
        assertEquals(100, uploadPercentage(120, 100))
        val photo = PhotoAttachment("fixture-key", "猫咪照片", uploading = true).withProgress(100, 100)
        assertEquals(100, photo.uploadPercent)
        assertNull(photo.mediaId)
        assertTrue(photo.uploading)
    }
}
