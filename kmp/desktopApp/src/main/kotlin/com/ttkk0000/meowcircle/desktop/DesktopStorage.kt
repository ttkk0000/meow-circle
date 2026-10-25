package com.ttkk0000.meowcircle.desktop

import com.github.javakeyring.Keyring
import com.ttkk0000.meowcircle.client.ComposeDraftFiles
import com.ttkk0000.meowcircle.client.MobileSecretStorage
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal data class DesktopEndpoint(val api: String, val web: String, val terms: String, val privacy: String) {
    companion object {
        fun fromEnvironment(): DesktopEndpoint {
            fun origin(name: String, fallback: String): String {
                val value = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() } ?: fallback
                val uri = URI(value)
                require(uri.scheme == "https" || uri.scheme == "http" && uri.host in listOf("localhost", "127.0.0.1", "::1"))
                require(uri.host != null && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path in listOf("", "/"))
                return value.trimEnd('/')
            }
            val api = origin("MND_API_BASE_URL", "http://127.0.0.1:8080")
            return DesktopEndpoint(api, origin("MND_WEB_BASE_URL", api), System.getenv("MND_TERMS_URL").orEmpty(), System.getenv("MND_PRIVACY_URL").orEmpty())
        }
    }
}

/** 文件按服务／账号隔离，原子替换，拒绝符号链接；不写入会话明文。 */
internal class DesktopPrivateFiles(private val base: Path, private val manifestLimit: Int) : ComposeDraftFiles {
    companion object {
        fun root(endpoint: String): Path {
            val os = System.getProperty("os.name").lowercase()
            val home = Path.of(System.getProperty("user.home"))
            val support = when {
                os.contains("mac") -> home.resolve("Library/Application Support")
                os.contains("win") -> System.getenv("LOCALAPPDATA")?.let(Path::of) ?: home.resolve("AppData/Local")
                else -> System.getenv("XDG_DATA_HOME")?.takeIf { Path.of(it).isAbsolute }?.let(Path::of) ?: home.resolve(".local/share")
            }
            val id = MessageDigest.getInstance("SHA-256").digest(endpoint.toByteArray()).joinToString("") { "%02x".format(it) }
            return support.resolve("meow-doggie/v2/$id").also(::privateDirectory)
        }
        fun privateDirectory(path: Path) {
            Files.createDirectories(path)
            check(!Files.isSymbolicLink(path))
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"))
        }
        fun atomic(path: Path, bytes: ByteArray) {
            privateDirectory(path.parent)
            check(!Files.isSymbolicLink(path))
            val temporary = Files.createTempFile(path.parent, ".pending-", ".tmp")
            try {
                if (Files.getFileStore(temporary).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"))
                Files.write(temporary, bytes)
                Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
            } finally { Files.deleteIfExists(temporary) }
        }
    }
    private fun directory(owner: Long): Path {
        require(owner > 0)
        return base.resolve(owner.toString()).also(::privateDirectory)
    }
    private fun file(owner: Long, name: String): Path {
        require(name == "manifest.json" || name.matches(Regex("[a-f0-9]{32}\\.photo")))
        return directory(owner).resolve(name).also { check(!Files.isSymbolicLink(it)) }
    }
    override fun read(owner: Long, name: String): ByteArray? {
        val path = file(owner, name)
        if (!Files.exists(path, NOFOLLOW_LINKS)) return null
        check(Files.size(path) <= if (name == "manifest.json") manifestLimit.toLong() else 10L * 1024 * 1024)
        return Files.readAllBytes(path)
    }
    override fun write(owner: Long, name: String, bytes: ByteArray) {
        require(bytes.size in 1..if (name == "manifest.json") manifestLimit else 10 * 1024 * 1024)
        atomic(file(owner, name), bytes)
    }
    override fun exists(owner: Long, name: String) = Files.exists(file(owner, name), NOFOLLOW_LINKS)
    override fun retainPhotos(owner: Long, names: Set<String>) {
        Files.list(directory(owner)).use { files -> files.filter { it.fileName.toString().matches(Regex("[a-f0-9]{32}\\.photo")) && it.fileName.toString() !in names }
            .forEach { check(!Files.isSymbolicLink(it)); Files.delete(it) } }
    }
}

/** 系统钥匙串只保管随机AES密钥，完整回执保险库经GCM加密后原子落盘。 */
internal class DesktopSecretStorage(private val base: Path) : MobileSecretStorage {
    private val file = base.resolve("session.gcm")
    private val service = "com.ttkk0000.meowcircle.desktop.v2"
    private val account = base.fileName.toString()
    private val aad = "$service:$account".toByteArray()
    @Synchronized override fun read(): String? {
        check(!Files.isSymbolicLink(file))
        if (!Files.exists(file, NOFOLLOW_LINKS)) return null
        check(Files.size(file) in 29..2L * 1024 * 1024)
        val bytes = Files.readAllBytes(file)
        check(bytes[0] == 1.toByte())
        val key = Keyring.create().use { Base64.getDecoder().decode(it.getPassword(service, account)) }
        try {
            check(key.size == 32)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(aad)
            return cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
        } finally { key.fill(0) }
    }
    @Synchronized override fun write(value: String?) {
        check(!Files.isSymbolicLink(file))
        if (value == null) { Files.deleteIfExists(file); return }
        val key = Keyring.create().use { ring ->
            if (Files.exists(file, NOFOLLOW_LINKS)) Base64.getDecoder().decode(ring.getPassword(service, account))
            else ByteArray(32).also { SecureRandom().nextBytes(it); ring.setPassword(service, account, Base64.getEncoder().encodeToString(it)) }
        }
        try {
            check(key.size == 32)
            val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce)); cipher.updateAAD(aad)
            val encrypted = byteArrayOf(1) + nonce + cipher.doFinal(value.toByteArray())
            check(encrypted.size <= 2 * 1024 * 1024)
            DesktopPrivateFiles.atomic(file, encrypted)
        } finally { key.fill(0) }
    }
}
