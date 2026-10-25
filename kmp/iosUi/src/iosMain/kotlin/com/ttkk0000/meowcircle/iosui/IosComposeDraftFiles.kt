@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ttkk0000.meowcircle.iosui

import com.ttkk0000.meowcircle.client.ComposeDraftFiles
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

class IosComposeDraftFiles(private val namespace: String = "mnd-compose", private val manifestLimit: ULong = 128uL * 1024uL) : ComposeDraftFiles {
    init { require(namespace.matches(Regex("mnd-[a-z-]+"))) }
    private val manager = NSFileManager.defaultManager

    private fun directory(owner: Long): String {
        require(owner > 0)
        val support = (manager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).first() as NSURL).path
            ?: error("Private draft directory unavailable")
        val path = "$support/$namespace/$owner"
        check(manager.createDirectoryAtPath(path, withIntermediateDirectories = true,
            attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUntilFirstUserAuthentication), error = null))
        check(NSURL.fileURLWithPath(path).setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null))
        return path
    }

    private fun file(owner: Long, name: String): String {
        require(name == "manifest.json" || name.matches(Regex("[a-f0-9]{32}\\.photo")))
        return "${directory(owner)}/$name"
    }

    override fun read(owner: Long, name: String): ByteArray? {
        val path = file(owner, name)
        if (!manager.fileExistsAtPath(path)) return null
        val data = NSData.dataWithContentsOfFile(path) ?: error("Private draft file unavailable")
        check(data.length <= if (name == "manifest.json") manifestLimit else 10uL * 1024uL * 1024uL)
        return ByteArray(data.length.toInt()).also { bytes ->
            if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override fun exists(owner: Long, name: String) = manager.fileExistsAtPath(file(owner, name))

    override fun write(owner: Long, name: String, bytes: ByteArray) {
        require(bytes.isNotEmpty())
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(file(owner, name), options = NSDataWritingAtomic or NSDataWritingFileProtectionCompleteUntilFirstUserAuthentication, error = null))
    }

    override fun retainPhotos(owner: Long, names: Set<String>) {
        val path = directory(owner)
        val children = manager.contentsOfDirectoryAtPath(path, error = null) ?: error("Private draft directory unavailable")
        children.filterIsInstance<String>().filter { it.matches(Regex("[a-f0-9]{32}\\.photo")) && it !in names }
            .forEach { check(manager.removeItemAtPath("$path/$it", error = null)) }
    }

}
