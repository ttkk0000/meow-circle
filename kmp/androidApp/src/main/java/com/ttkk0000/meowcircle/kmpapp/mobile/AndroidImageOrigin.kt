package com.ttkk0000.meowcircle.kmpapp.mobile

import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream

/** 转图只保留服务端合同允许的来源字段；相机、位置及其他元数据不复制。 */
internal object AndroidImageOrigin {
    private val allowed = setOf("Label","ContentProducer","ProduceID","ReservedCode1","ContentPropagator","PropagateID","ReservedCode2")
    fun read(bytes: ByteArray,mime: String?): String? {
        if(mime !in listOf("image/jpeg","image/png")) {
            require(!bytes.toString(Charsets.ISO_8859_1).contains("AIGC",ignoreCase = true)) { "此格式包含来源标识，请改用保留标识的 JPEG 或 PNG" }
            return null
        }
        val parts = if(mime == "image/png") pngText(bytes) else jpegText(bytes)
        var selected: String? = null
        parts.forEach { part ->
            if(part.contains("AIGC",ignoreCase = true)) {
                val decoded = part.replace("&quot;","\"").replace("&apos;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&")
                val start = Regex("\\{\\s*\"AIGC\"\\s*:").find(decoded)?.range?.first ?: error("来源标识无法读取，请保留原文件")
                val root = JSONTokener(decoded.substring(start).take(16384)).nextValue() as? JSONObject ?: error("来源标识无效")
                require(root.keys().asSequence().toSet() == setOf("AIGC"))
                val original = root.getJSONObject("AIGC")
                require(original.keys().asSequence().toSet().all { it in allowed })
                require(original.getString("Label") in listOf("1","2","3"))
                val clean = JSONObject()
                allowed.sorted().forEach { name ->
                    val value = if(original.has(name)) original.get(name) else ""
                    require(value is String && value.length <= 1024 && value.all { it.code in 0x21..0x7e && it != '\\' })
                    clean.put(name,value)
                }
                val label = JSONObject().put("AIGC",clean).toString()
                require(selected == null || JSONObject(selected!!).getJSONObject("AIGC").toString() == clean.toString()) { "来源标识冲突，请保留原文件" }
                selected = label
            }
        }
        return selected
    }
    fun write(bytes: ByteArray,png: Boolean,label: String?): ByteArray {
        if(label == null) return bytes
        val payload = "AIGC\u0000$label".toByteArray(Charsets.UTF_8)
        return if(png) {
            require(bytes.size >= 33)
            val type = "tEXt".toByteArray(); val checksum = CRC32().apply { update(type); update(payload) }.value
            ByteArrayOutputStream().apply {
                write(bytes,0,33); write(ByteBuffer.allocate(4).putInt(payload.size).array()); write(type); write(payload)
                write(ByteBuffer.allocate(4).putInt(checksum.toInt()).array()); write(bytes,33,bytes.size - 33)
            }.toByteArray()
        } else {
            require(bytes.size >= 2 && payload.size < 65533)
            ByteArrayOutputStream().apply { write(bytes,0,2); write(byteArrayOf(0xff.toByte(),0xfe.toByte())); write(ByteBuffer.allocate(2).putShort((payload.size + 2).toShort()).array()); write(payload); write(bytes,2,bytes.size - 2) }.toByteArray()
        }
    }
    private fun jpegText(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>(); var pos = 2
        while(pos + 4 <= bytes.size) {
            if((bytes[pos].toInt() and 255) != 255) break
            val marker = bytes[pos + 1].toInt() and 255
            if(marker == 0xda || marker == 0xd9) break
            if(marker == 0xff) { pos++; continue }
            if(marker == 1 || marker in 0xd0..0xd7) { pos += 2; continue }
            val length = ((bytes[pos + 2].toInt() and 255) shl 8) or (bytes[pos + 3].toInt() and 255)
            require(length >= 2 && length <= bytes.size - pos - 2)
            if(marker in 0xe0..0xef || marker == 0xfe) result.add(bytes.copyOfRange(pos + 4,pos + length + 2).toString(Charsets.UTF_8))
            pos += length + 2
        }
        return result
    }
    private fun pngText(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>(); var pos = 8
        while(pos + 12 <= bytes.size) {
            val length = ByteBuffer.wrap(bytes,pos,4).int
            require(length >= 0 && length <= bytes.size - pos - 12)
            val kind = bytes.copyOfRange(pos + 4,pos + 8).toString(Charsets.US_ASCII)
            if(kind in listOf("tEXt","iTXt","zTXt")) {
                val data = bytes.copyOfRange(pos + 8,pos + 8 + length)
                val split = data.indexOf(0.toByte()); require(split >= 0)
                var text = data.copyOfRange(split + 1,data.size); var compressed = false
                if(kind == "zTXt") { require(text.isNotEmpty() && text[0].toInt() == 0); text = text.copyOfRange(1,text.size); compressed = true }
                if(kind == "iTXt") {
                    require(text.size >= 2 && text[0].toInt() in 0..1 && text[1].toInt() == 0)
                    compressed = text[0].toInt() == 1; text = text.copyOfRange(2,text.size)
                    repeat(2) { val end = text.indexOf(0.toByte()); require(end >= 0); text = text.copyOfRange(end + 1,text.size) }
                }
                if(compressed) text = InflaterInputStream(ByteArrayInputStream(text)).use { input ->
                    val output = ByteArrayOutputStream(); val buffer = ByteArray(1024); var count = input.read(buffer)
                    while(count != -1) { require(output.size() + count <= 16384); output.write(buffer,0,count); count = input.read(buffer) }; output.toByteArray()
                }
                require(text.size <= 16384)
                result.add(data.copyOfRange(0,split).toString(Charsets.UTF_8) + "\u0000" + text.toString(Charsets.UTF_8))
            }
            pos += length + 12
        }
        return result
    }
}
