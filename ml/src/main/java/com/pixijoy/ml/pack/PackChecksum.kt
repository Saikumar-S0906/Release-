package com.pixijoy.ml.pack

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object PackChecksum {
    private const val HEX = "0123456789abcdef"

    fun sha256(bytes: ByteArray): String = hex(digest().digest(bytes))

    fun sha256(file: File): String = file.inputStream().use { sha256(it) }

    fun sha256(stream: InputStream): String {
        val md = digest()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = stream.read(buf)
            if (n <= 0) break
            md.update(buf, 0, n)
        }
        return hex(md.digest())
    }

    fun matches(file: File, expected: String): Boolean {
        if (!file.isFile) return false
        return sha256(file).equals(expected.trim(), ignoreCase = true)
    }

    /** FlatBuffer identifier for `.tflite` sits at bytes 4–7. */
    fun looksLikeTflite(file: File): Boolean {
        if (!file.isFile || file.length() < 8) return false
        val header = ByteArray(8)
        file.inputStream().use { it.read(header) }
        return header[4] == 'T'.code.toByte() &&
            header[5] == 'F'.code.toByte() &&
            header[6] == 'L'.code.toByte() &&
            header[7] == '3'.code.toByte()
    }

    private fun digest(): MessageDigest = MessageDigest.getInstance("SHA-256")

    private fun hex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xff
            out[i++] = HEX[v ushr 4]
            out[i++] = HEX[v and 0x0f]
        }
        return String(out)
    }
}
