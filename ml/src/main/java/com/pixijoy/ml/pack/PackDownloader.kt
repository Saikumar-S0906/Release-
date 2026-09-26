package com.pixijoy.ml.pack

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object PackDownloader {
    const val MAX_BYTES = 80L * 1024L * 1024L

    fun download(url: String, dest: File, onProgress: (Float) -> Unit): Result<Unit> {
        if (!url.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Pack download must use HTTPS."))
        }
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                // FILM-class packs are ~70 MB; allow a slow mid-range link.
                readTimeout = 300_000
                setRequestProperty("User-Agent", "PixiJoy/2.0")
                setRequestProperty("Accept", "*/*")
            }
            conn.connect()
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    return Result.failure(IOException("Pack download failed ($code)."))
                }
                val declared = conn.contentLengthLong
                if (declared > MAX_BYTES) {
                    return Result.failure(IOException("Pack is larger than the download limit."))
                }
                var copied = 0L
                conn.inputStream.use { input ->
                    dest.outputStream().use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            copied += n
                            if (copied > MAX_BYTES) {
                                return Result.failure(IOException("Pack is larger than the download limit."))
                            }
                            output.write(buf, 0, n)
                            val progress = if (declared > 0) {
                                (copied.toFloat() / declared.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.35f
                            }
                            onProgress(progress)
                        }
                    }
                }
                Result.success(Unit)
            } finally {
                conn.disconnect()
            }
        } catch (err: Exception) {
            Result.failure(err)
        }
    }
}
