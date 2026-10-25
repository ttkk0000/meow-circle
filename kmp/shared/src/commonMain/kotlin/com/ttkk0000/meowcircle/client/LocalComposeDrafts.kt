package com.ttkk0000.meowcircle.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 平台实现只访问应用私有、排除备份的目录；write 必须原子替换单个文件。 */
interface ComposeDraftFiles {
    fun read(owner: Long, name: String): ByteArray?
    fun write(owner: Long, name: String, bytes: ByteArray)
    fun exists(owner: Long, name: String): Boolean
    fun retainPhotos(owner: Long, names: Set<String>)
}

@Serializable
data class LocalComposePhoto(val key: String, val name: String, val mime: String, val mediaId: Long? = null)

@Serializable
data class LocalComposeDraft(
    val schema: Int = 1, val server: String, val owner: Long, val revision: Long = 0,
    val cloudVersion: Long = 0, val cloudLoaded: Boolean = false,
    val title: String = "", val text: String = "", val edited: Boolean = false,
    val photos: List<LocalComposePhoto> = emptyList(), val publicationKey: String? = null,
    val entry: EntryBody? = null,
    val sourceRequestId: String? = null, val sourceRecord: MobileRecord<SourceDeclarationBody>? = null,
)

class LocalDraftException(val reason: String) : Exception(reason)

/** 由 Application / IosClient 共用，避免不同窗口悄悄覆盖同一份本地草稿。 */
class LocalComposeDrafts(private val files: ComposeDraftFiles, val server: String) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun validate(draft: LocalComposeDraft, owner: Long) {
        if (owner <= 0 || draft.owner != owner || draft.server != server || draft.schema != 1 ||
            draft.revision < 0 || draft.cloudVersion < 0 || draft.title.length > 160 || draft.text.length > 10000 ||
            draft.photos.size > 9 || draft.photos.map { it.key }.toSet().size != draft.photos.size ||
            draft.photos.any { !it.key.matches(Regex("[a-f0-9]{32}")) || it.name.length > 200 ||
                it.mime !in listOf("image/jpeg", "image/png") || it.mediaId?.let { id -> id <= 0 } == true } ||
            draft.publicationKey?.let { it.length !in 8..100 } == true ||
            draft.sourceRequestId?.let { !it.matches(Regex("[A-Za-z0-9_-]{8,100}")) } == true ||
            draft.sourceRecord?.let { it.id <= 0 || it.ownerId != owner } == true) {
            throw LocalDraftException("本地草稿与当前账号或服务不一致，请保留文件并核对")
        }
    }

    private fun read(owner: Long): LocalComposeDraft? {
        val bytes = files.read(owner, "manifest.json") ?: return null
        if (bytes.size > 128 * 1024) throw LocalDraftException("本地草稿无法读取，请保留文件并核对")
        return json.decodeFromString<LocalComposeDraft>(bytes.decodeToString()).also { validate(it, owner) }
    }

    suspend fun load(owner: Long): LocalComposeDraft? = withContext(Dispatchers.Default) {
        mutex.withLock { read(owner) }
    }

    suspend fun photo(owner: Long, photo: LocalComposePhoto): SelectedPhoto = withContext(Dispatchers.Default) {
        mutex.withLock {
            require(owner > 0 && photo.key.matches(Regex("[a-f0-9]{32}")))
            val bytes = files.read(owner, "${photo.key}.photo")
            if (bytes == null || bytes.size !in 1..10 * 1024 * 1024) {
                throw LocalDraftException("原照片暂不可读，请重试或移除后重新选择")
            }
            SelectedPhoto(photo.name, photo.mime, bytes)
        }
    }

    suspend fun save(expectedRevision: Long, draft: LocalComposeDraft, photos: Map<String, SelectedPhoto>): LocalComposeDraft =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                validate(draft, draft.owner)
                val current = read(draft.owner)
                if ((current?.revision ?: 0L) != expectedRevision) {
                    throw LocalDraftException("本地草稿已在另一窗口更新，请先核对，不会覆盖原文件")
                }
                val pending = draft.photos.filter { it.mediaId == null }
                for (item in pending) {
                    val name = "${item.key}.photo"
                    if (!files.exists(draft.owner, name)) {
                        val photo = photos[item.key] ?: throw LocalDraftException("原照片暂不可读，尚未覆盖本地草稿")
                        require(photo.mime == item.mime && photo.bytes.size in 1..10 * 1024 * 1024)
                        files.write(draft.owner, name, photo.bytes)
                    }
                }
                val next = draft.copy(revision = expectedRevision + 1)
                // 先保证原始重试字节落盘，再提交清单；失败时旧清单仍可恢复。
                files.write(draft.owner, "manifest.json", json.encodeToString(next).encodeToByteArray())
                // 清理失败不回滚已提交的清单；下次保存时继续回收无引用照片。
                try { files.retainPhotos(draft.owner, pending.map { "${it.key}.photo" }.toSet()) }
                catch (_: Exception) { /* 保留多余私有文件比丢失当前草稿安全。 */ }
                next
            }
        }

    suspend fun clear(owner: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val previous = read(owner)
            // 空清单保留递增版本，阻止退出前排队的旧窗口写回已清理的数据。
            val empty = LocalComposeDraft(server = server, owner = owner, revision = (previous?.revision ?: 0L) + 1)
            files.write(owner, "manifest.json", json.encodeToString(empty).encodeToByteArray())
            files.retainPhotos(owner, emptySet())
        }
    }
}
