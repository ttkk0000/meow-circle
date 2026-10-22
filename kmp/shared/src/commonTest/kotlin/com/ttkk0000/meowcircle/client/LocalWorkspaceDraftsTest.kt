package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** 账号隔离、原上传身份与原子提交回归源码；本任务不执行测试。 */
class LocalWorkspaceDraftsTest {
    private class Files : ComposeDraftFiles {
        val values = mutableMapOf<Pair<Long,String>,ByteArray>()
        var failManifest = false
        override fun read(owner: Long,name: String) = values[owner to name]?.copyOf()
        override fun exists(owner: Long,name: String) = values.containsKey(owner to name)
        override fun write(owner: Long,name: String,bytes: ByteArray) {
            if(failManifest && name == "manifest.json") error("disk unavailable")
            values[owner to name] = bytes.copyOf()
        }
        override fun retainPhotos(owner: Long,names: Set<String>) {
            values.keys.removeAll { it.first == owner && it.second.endsWith(".photo") && it.second !in names }
        }
    }
    private val key = "a".repeat(32)
    private val photo = SelectedPhoto("原照片","image/png",byteArrayOf(1,2,3))
    private val draft = WorkspaceDraft("CaseEdit:0::0",mapOf("title" to "原求助"),17,
        listOf(WorkspacePhoto(key,photo.name,photo.mime,safety = true)),true,listOf("original-request"),"safety_session","original-reference")

    @Test fun guestAndRealAccountOneNeverShareDirectory() = runTest {
        val accounts = Files(); val guests = Files(); val storage = LocalWorkspaceDrafts(accounts,guests,"server")
        storage.save(0,0,listOf(draft),mapOf(key to photo))
        assertNull(storage.load(1))
        storage.save(1,0,listOf(draft.copy(values = mapOf("title" to "账号一"))),mapOf(key to photo))
        assertEquals("原求助",storage.load(0)!!.drafts.single().values["title"])
        assertEquals("账号一",storage.load(1)!!.drafts.single().values["title"])
        assertNull(storage.load(2))
    }

    @Test fun restartKeepsOriginalKeyVersionPurposeAndUnknownRequest() = runTest {
        val accounts = Files(); val guests = Files()
        LocalWorkspaceDrafts(accounts,guests,"server").save(7,0,listOf(draft),mapOf(key to photo))
        val after = LocalWorkspaceDrafts(accounts,guests,"server")
        val restored = after.load(7)!!.drafts.single()
        assertEquals(draft,restored)
        assertContentEquals(photo.bytes,after.photo(7,restored.photos.single()).bytes)
    }

    @Test fun failedManifestDoesNotRemoveOriginalPhotoOrAdvanceRevision() = runTest {
        val accounts = Files(); val storage = LocalWorkspaceDrafts(accounts,Files(),"server")
        val saved = storage.save(7,0,listOf(draft),mapOf(key to photo))
        accounts.failManifest = true
        assertFailsWith<IllegalStateException> { storage.save(7,saved.revision,emptyList(),emptyMap()) }
        assertEquals(saved,storage.load(7))
        assertTrue(accounts.exists(7,"$key.photo"))
    }

    @Test fun logoutTombstonePreventsOldWindowFromResurrectingMaterials() = runTest {
        val accounts = Files(); val storage = LocalWorkspaceDrafts(accounts,Files(),"server")
        val saved = storage.save(7,0,listOf(draft),mapOf(key to photo))
        storage.clear(7)
        assertFailsWith<LocalDraftException> { storage.save(7,saved.revision,listOf(draft),mapOf(key to photo)) }
        assertTrue(storage.load(7)!!.drafts.isEmpty())
        assertFalse(accounts.exists(7,"$key.photo"))
    }

    @Test fun credentialsForeignServerAndPathInjectionAreRejected() = runTest {
        val accounts = Files(); val guests = Files(); val storage = LocalWorkspaceDrafts(accounts,guests,"server")
        assertFailsWith<IllegalArgumentException> { storage.save(7,0,listOf(draft.copy(values = mapOf("reauth_token" to "must-not-persist"))),mapOf(key to photo)) }
        assertFailsWith<IllegalArgumentException> { storage.save(7,0,listOf(draft.copy(photos = listOf(WorkspacePhoto("../foreign",photo.name,photo.mime)))),emptyMap()) }
        storage.save(7,0,listOf(draft),mapOf(key to photo))
        assertFailsWith<IllegalArgumentException> { LocalWorkspaceDrafts(accounts,guests,"other-server").load(7) }
    }
}
