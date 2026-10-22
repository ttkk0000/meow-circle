package com.ttkk0000.meowcircle.kmpapp.mobile

import android.content.Context
import android.util.AtomicFile
import com.ttkk0000.meowcircle.client.ComposeDraftFiles
import java.io.File

/** 相册临时授权不作为恢复依据；只保存规范化后、属于当前账号的应用私有副本。 */
class AndroidComposeDraftFiles(context: Context, directoryName: String = "mnd-compose", private val manifestLimit: Long = 128L * 1024) : ComposeDraftFiles {
    init { require(directoryName in setOf("mnd-compose","mnd-workflows","mnd-workflows-guest","mnd-replies","mnd-replies-guest")) }
    private val root = File(context.noBackupFilesDir, directoryName)

    private fun directory(owner: Long): File {
        require(owner > 0)
        return File(root, owner.toString()).also { check(it.isDirectory || it.mkdirs()) }
    }

    private fun file(owner: Long, name: String): File {
        require(name == "manifest.json" || name.matches(Regex("[a-f0-9]{32}\\.photo")))
        return File(directory(owner), name)
    }

    override fun read(owner: Long, name: String): ByteArray? {
        val target = file(owner, name)
        if (!target.exists() && !File(target.path + ".bak").exists()) return null
        return AtomicFile(target).openRead().use {
            check(it.channel.size() <= if (name == "manifest.json") manifestLimit else 10L * 1024 * 1024)
            it.readBytes()
        }
    }

    override fun exists(owner: Long, name: String): Boolean {
        val target = file(owner, name)
        return target.isFile || File(target.path + ".bak").isFile
    }

    override fun write(owner: Long, name: String, bytes: ByteArray) {
        val atomic = AtomicFile(file(owner, name))
        val stream = atomic.startWrite()
        try { stream.write(bytes); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }

    override fun retainPhotos(owner: Long, names: Set<String>) {
        val children = directory(owner).listFiles() ?: error("Private draft directory unavailable")
        children.filter { it.name.matches(Regex("[a-f0-9]{32}\\.photo(?:\\.bak|\\.new)?")) }
            .filter { it.name.removeSuffix(".bak").removeSuffix(".new") !in names }
            .forEach { check(it.delete() || !it.exists()) }
    }

}
