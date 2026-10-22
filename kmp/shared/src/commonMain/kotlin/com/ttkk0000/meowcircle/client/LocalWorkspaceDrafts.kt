package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WorkspacePhoto(val key: String,val name: String,val mime: String,val mediaId: Long? = null,
    val private: Boolean = true,val safety: Boolean = false)
@Serializable
data class WorkspaceDraft(val key: String,val values: Map<String,String> = emptyMap(),val baseVersion: Long? = null,
    val photos: List<WorkspacePhoto> = emptyList(),val uncertain: Boolean = false,val requests: List<String> = emptyList(),
    val accessType: String? = null,val accessReference: String? = null)
@Serializable
data class WorkspaceSnapshot(val schema: Int = 1,val server: String,val owner: Long,val revision: Long = 0,val drafts: List<WorkspaceDraft> = emptyList())

/** 草稿只含编辑数据及受限凭证引用。令牌、密码、OTP 和 grant 仍留在各自安全通道。 */
class LocalWorkspaceDrafts(private val files: ComposeDraftFiles,private val guestFiles: ComposeDraftFiles,private val server: String) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun target(owner: Long): Pair<ComposeDraftFiles,Long> { require(owner >= 0); return if(owner == 0L) guestFiles to 1L else files to owner }
    private fun validate(snapshot: WorkspaceSnapshot,owner: Long) {
        require(snapshot.schema == 1 && snapshot.owner == owner && snapshot.server == server && snapshot.revision >= 0)
        require(snapshot.drafts.size <= 40 && snapshot.drafts.map { it.key }.distinct().size == snapshot.drafts.size)
        snapshot.drafts.forEach { draft ->
            require(draft.key.length in 1..200 && draft.key.all { it.isLetterOrDigit() || it in ":_-" })
            require(draft.values.size <= 300 && draft.values.all { (key,value) -> key.length in 1..100 && value.length <= 10000 })
            require(draft.values.keys.none { it.lowercase() in setOf("password","code","token","reauth_token","query_token","safety_token") })
            require(draft.baseVersion == null || draft.baseVersion >= 0)
            require(draft.photos.size <= 9 && draft.photos.map { it.key }.distinct().size == draft.photos.size)
            draft.photos.forEach { photo -> require(photo.key.matches(Regex("[a-f0-9]{32}")) && photo.name.length <= 200 && photo.mime in listOf("image/jpeg","image/png") && (photo.mediaId == null || photo.mediaId > 0)) }
            require(draft.requests.size <= 16 && draft.requests.all { it.length in 8..100 })
            require(draft.accessType == null || draft.accessType in listOf("safety_ticket","safety_session"))
            require(draft.accessReference == null || draft.accessReference.length in 8..100)
        }
    }
    private fun read(owner: Long): WorkspaceSnapshot? {
        val (directory,id) = target(owner)
        val data = directory.read(id,"manifest.json") ?: return null
        require(data.size <= 1024 * 1024)
        return json.decodeFromString<WorkspaceSnapshot>(data.decodeToString()).also { validate(it,owner) }
    }
    suspend fun load(owner: Long): WorkspaceSnapshot? = withContext(Dispatchers.Default) { mutex.withLock { read(owner) } }
    suspend fun photo(owner: Long,photo: WorkspacePhoto): SelectedPhoto = withContext(Dispatchers.Default) {
        mutex.withLock {
            require(photo.key.matches(Regex("[a-f0-9]{32}")))
            val (directory,id) = target(owner)
            val data = directory.read(id,"${photo.key}.photo") ?: throw LocalDraftException("原照片暂不可读，请重试，原草稿未覆盖")
            if(data.size !in 1..10 * 1024 * 1024) throw LocalDraftException("原照片大小不符合上传规则")
            SelectedPhoto(photo.name,photo.mime,data)
        }
    }
    suspend fun save(owner: Long,expectedRevision: Long,drafts: List<WorkspaceDraft>,bytes: Map<String,SelectedPhoto>): WorkspaceSnapshot = withContext(Dispatchers.Default) {
        mutex.withLock {
            val current = read(owner)
            if((current?.revision ?: 0) != expectedRevision) throw LocalDraftException("业务草稿已在另一窗口更新，当前输入保留，请先核对")
            val snapshot = WorkspaceSnapshot(server = server,owner = owner,revision = expectedRevision + 1,drafts = drafts)
            validate(snapshot,owner)
            val encoded = json.encodeToString(snapshot).encodeToByteArray()
            if(encoded.size > 1024 * 1024) throw LocalDraftException("本机业务草稿已达容量，请先处理或移除已有草稿")
            val (directory,id) = target(owner)
            for(photo in drafts.flatMap { it.photos }.filter { it.mediaId == null }) {
                val name = "${photo.key}.photo"
                if(!directory.exists(id,name)) {
                    val selected = bytes[photo.key] ?: throw LocalDraftException("原照片尚未恢复，原草稿不会被覆盖")
                    require(selected.mime == photo.mime && selected.bytes.size in 1..10 * 1024 * 1024)
                    directory.write(id,name,selected.bytes)
                }
            }
            directory.write(id,"manifest.json",encoded)
            // 清理失败不改变已经提交的 revision；下次保存继续回收未引用副本。
            try { directory.retainPhotos(id,drafts.flatMap { it.photos }.filter { it.mediaId == null }.map { "${it.key}.photo" }.toSet()) }
            catch (_: Exception) { }
            snapshot
        }
    }
    suspend fun clear(owner: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val current = read(owner)
            val (directory,id) = target(owner)
            directory.write(id,"manifest.json",json.encodeToString(WorkspaceSnapshot(server = server,owner = owner,revision = (current?.revision ?: 0) + 1)).encodeToByteArray())
            directory.retainPhotos(id,emptySet())
        }
    }
}
