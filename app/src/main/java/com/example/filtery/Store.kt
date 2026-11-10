package com.example.filtery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every setting lives here (plain SharedPreferences, nothing leaves the phone). */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("filtery", Context.MODE_PRIVATE)

    private fun bool(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun setBool(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply() }

    var front: Boolean get() = bool("front", true); set(v) = setBool("front", v)
    var mirror: Boolean get() = bool("mirror", true); set(v) = setBool("mirror", v)
    var saveToPhone: Boolean get() = bool("saveToPhone", true); set(v) = setBool("saveToPhone", v)
    var photoHigh: Boolean get() = bool("photoHigh", false); set(v) = setBool("photoHigh", v)
    var videoSound: Boolean get() = bool("videoSound", true); set(v) = setBool("videoSound", v)
    var autoUpdate: Boolean get() = bool("autoUpdate", true); set(v) = setBool("autoUpdate", v)
    var wifiOnly: Boolean get() = bool("wifiOnly", true); set(v) = setBool("wifiOnly", v)
    var grid: Boolean get() = bool("grid", false); set(v) = setBool("grid", v)
    var previewQ: Int get() = sp.getInt("previewQ", 1); set(v) { sp.edit().putInt("previewQ", v).apply() }
    var flash: Int get() = sp.getInt("flash", 0); set(v) { sp.edit().putInt("flash", v).apply() }
    var timer: Int get() = sp.getInt("timer", 0); set(v) { sp.edit().putInt("timer", v).apply() }
    var lastFilter: String get() = sp.getString("lastFilter", "") ?: ""; set(v) { sp.edit().putString("lastFilter", v).apply() }
    var lastIntensity: Float get() = sp.getFloat("lastIntensity", 1f); set(v) { sp.edit().putFloat("lastIntensity", v).apply() }
    var favorites: List<String>
        get() = (sp.getString("favs", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) { sp.edit().putString("favs", v.joinToString(",")).apply() }
    var recents: List<String>
        get() = (sp.getString("recents", "") ?: "").split(",").filter { it.isNotBlank() }
        set(v) { sp.edit().putString("recents", v.joinToString(",")).apply() }
    var lastCheck: Long get() = sp.getLong("lastCheck", 0L); set(v) { sp.edit().putLong("lastCheck", v).apply() }
}

/** Photos and videos are kept in the app's own gallery folder and, if allowed, copied to the phone gallery (Pictures/Filtery). */
object Saver {
    fun dir(ctx: Context): File = File(ctx.filesDir, "gallery").also { it.mkdirs() }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    fun newPhoto(ctx: Context): File = File(dir(ctx), "IMG_${stamp()}.jpg")
    fun newVideo(ctx: Context): File = File(dir(ctx), "VID_${stamp()}.mp4")
    fun tempPhoto(ctx: Context): File = File(ctx.cacheDir, "shot_${stamp()}.jpg")

    fun isVideo(f: File) = f.name.endsWith(".mp4", true)

    fun list(ctx: Context): List<File> =
        (dir(ctx).listFiles() ?: emptyArray()).filter { it.isFile && it.length() > 0 && (it.name.endsWith(".jpg") || it.name.endsWith(".mp4")) }
            .sortedByDescending { it.lastModified() }

    fun saveJpeg(ctx: Context, bmp: Bitmap, quality: Int = 92): File {
        val f = newPhoto(ctx)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return f
    }

    /** Copies the file into the phone's gallery. Returns false if Android refused. */
    fun publish(ctx: Context, f: File): Boolean {
        val video = isVideo(f)
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (video) "video/mp4" else "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, (if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/Filtery")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val coll = if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = ctx.contentResolver.insert(coll, values) ?: return false
                ctx.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val base = Environment.getExternalStoragePublicDirectory(if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
                val d = File(base, "Filtery"); d.mkdirs()
                val dst = File(d, f.name)
                f.copyTo(dst, true)
                MediaScannerConnection.scanFile(ctx, arrayOf(dst.absolutePath), null, null)
                true
            }
        } catch (e: Exception) { false }
    }

    /** A first-frame picture of a video, for the gallery grid. */
    fun videoFrame(f: File): Bitmap? {
        val r = MediaMetadataRetriever()
        return try { r.setDataSource(f.absolutePath); r.getFrameAtTime(0) } catch (e: Exception) { null } finally { try { r.release() } catch (_: Exception) { } }
    }
}
