package com.pixijoy.ai.sticker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppStickerExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun exportAndLaunchWhatsApp(stickers: List<Bitmap>): Boolean = withContext(Dispatchers.IO) {
        if (stickers.isEmpty()) return@withContext false
        val packDir = File(context.cacheDir, "wa_stickers/${System.currentTimeMillis()}").apply { mkdirs() }
        val stickerFiles = stickers.take(30).mapIndexed { index, bitmap ->
            val file = File(packDir, "sticker_$index.webp")
            FileOutputStream(file).use { out ->
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
            }
            file
        }
        val tray = File(packDir, "tray.webp")
        FileOutputStream(tray).use { out ->
            val trayBitmap = Bitmap.createScaledBitmap(stickers.first(), 96, 96, true)
            @Suppress("DEPRECATION")
            trayBitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
            trayBitmap.recycle()
        }

        val packId = packDir.name
        val authority = "${context.packageName}.stickerprovider"
        writeContentsJson(packDir, packId, stickerFiles)

        val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").apply {
            putExtra("sticker_pack_id", packId)
            putExtra("sticker_pack_authority", authority)
            putExtra("sticker_pack_name", context.getString(R.string.app_name))
            setPackage(WHATSAPP_PACKAGE)
        }
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrElse {
            // Fallback: share WebP files if WhatsApp intent unavailable
            val uris = stickerFiles.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/webp"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(WHATSAPP_PACKAGE)
            }
            runCatching {
                context.startActivity(share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
        }
    }

    private fun writeContentsJson(packDir: File, packId: String, stickers: List<File>) {
        val stickerArray = JSONArray()
        stickers.forEachIndexed { index, file ->
            stickerArray.put(
                JSONObject()
                    .put("image_file", file.name)
                    .put("emojis", JSONArray().put("😀"))
            )
        }
        val json = JSONObject()
            .put("android_play_store_link", "")
            .put("ios_app_store_link", "")
            .put("sticker_pack_identifier", packId)
            .put("sticker_pack_name", context.getString(R.string.app_name))
            .put("sticker_pack_publisher", "PixiJoy AI")
            .put("sticker_pack_tray_image_file", "tray.webp")
            .put("stickers", stickerArray)
        File(packDir, "contents.json").writeText(json.toString())
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
}
