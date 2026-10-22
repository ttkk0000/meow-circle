package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** 回归用例源码；本任务不运行测试。 */
class LocalComposeDraftsTest {
    private class Files : ComposeDraftFiles {
        val data = mutableMapOf<Pair<Long, String>, ByteArray>()
        var failManifest = false
        override fun read(owner: Long, name: String) = data[owner to name]?.copyOf()
        override fun exists(owner: Long, name: String) = owner to name in data
        override fun write(owner: Long, name: String, bytes: ByteArray) {
            if (failManifest && name == "manifest.json") error("disk unavailable")
            data[owner to name] = bytes.copyOf()
        }
        override fun retainPhotos(owner: Long, names: Set<String>) {
            data.keys.removeAll { it.first == owner && it.second.endsWith(".photo") && it.second !in names }
        }
    }
    private val server = "https://mnd.example"
    private val key = "a".repeat(32)
    private val photo = SelectedPhoto("照片 1", "image/png", byteArrayOf(1, 2, 3))
    private fun draft() = LocalComposeDraft(server = server, owner = 7, title = "小满", text = "未完成草稿",
        edited = true, photos = listOf(LocalComposePhoto(key, photo.name, photo.mime)))

    @Test fun processRestartKeepsOriginalUploadKeyBytesAndOrder() = runTest {
        val files = Files()
        val before = LocalComposeDrafts(files, server)
        before.save(0, draft(), mapOf(key to photo))
        val after = LocalComposeDrafts(files, server)
        val restored = after.load(7)!!
        assertEquals(listOf(key), restored.photos.map { it.key })
        assertContentEquals(photo.bytes, after.photo(7, restored.photos.single()).bytes)
        assertEquals("未完成草稿", restored.text)
        assertNull(after.load(8))
    }

    @Test fun failedManifestWritePreservesPreviousSnapshotAndPhotos() = runTest {
        val files = Files()
        val drafts = LocalComposeDrafts(files, server)
        val saved = drafts.save(0, draft(), mapOf(key to photo))
        files.failManifest = true
        assertFailsWith<IllegalStateException> { drafts.save(saved.revision, saved.copy(photos = emptyList()), emptyMap()) }
        assertEquals(saved, drafts.load(7))
        assertContentEquals(photo.bytes, drafts.photo(7, saved.photos.single()).bytes)
    }

    @Test fun anotherWindowCannotOverwriteNewerOrClearedDraft() = runTest {
        val files = Files()
        val drafts = LocalComposeDrafts(files, server)
        val saved = drafts.save(0, draft(), mapOf(key to photo))
        assertFailsWith<LocalDraftException> { drafts.save(0, draft(), mapOf(key to photo)) }
        drafts.clear(7)
        assertFailsWith<LocalDraftException> { drafts.save(saved.revision, saved, mapOf(key to photo)) }
        assertEquals("", drafts.load(7)!!.text)
        assertFalse(files.exists(7, "$key.photo"))
    }

    @Test fun differentServerAndPathInjectionNeverReuseLocalPhoto() = runTest {
        val files = Files()
        val drafts = LocalComposeDrafts(files, server)
        drafts.save(0, draft(), mapOf(key to photo))
        assertFailsWith<LocalDraftException> { LocalComposeDrafts(files, "https://other.example").load(7) }
        assertFailsWith<LocalDraftException> {
            drafts.save(1, draft().copy(photos = listOf(LocalComposePhoto("../foreign", photo.name, photo.mime))), emptyMap())
        }
    }

    @Test fun uploadedIdIsCommittedBeforeOriginalBytesArePruned() = runTest {
        val files = Files()
        val drafts = LocalComposeDrafts(files, server)
        val saved = drafts.save(0, draft(), mapOf(key to photo))
        val uploaded = drafts.save(saved.revision, saved.copy(photos = saved.photos.map { it.copy(mediaId = 9007199254740993) },
            publicationKey = "original-publication"), emptyMap())
        assertFalse(files.exists(7, "$key.photo"))
        assertEquals(9007199254740993, drafts.load(7)!!.photos.single().mediaId)
        assertEquals(uploaded.publicationKey, drafts.load(7)!!.publicationKey)
    }
}
