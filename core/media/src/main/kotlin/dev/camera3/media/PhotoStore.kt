package dev.camera3.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    // Binder calls for the pending entry and file descriptor are short; the engine calls
    // this on its camera handler thread, not the main thread.
    fun openVideoOutput(): VideoOutput {
        val name = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}_${UUID.randomUUID().toString().take(8)}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera3")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Unable to create a video entry in MediaStore")
        val fd = try { resolver.openFileDescriptor(uri, "rw") } catch (error: Throwable) {
            try { resolver.delete(uri, null, null) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            throw error
        }
        if (fd == null) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) { }
            error("Unable to open the video output")
        }
        return VideoOutput(uri, fd)
    }

    suspend fun publishVideo(uri: Uri, fd: ParcelFileDescriptor): Uri = withContext(Dispatchers.IO) {
        runCatching { fd.close() }
        check(resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1) {
            "Unable to publish the saved recording"
        }
        uri
    }

    suspend fun discardVideo(uri: Uri, fd: ParcelFileDescriptor): Unit = withContext(Dispatchers.IO) {
        runCatching { fd.close() }
        try { resolver.delete(uri, null, null) } catch (_: Exception) { }
        Unit
    }

    suspend fun saveJpeg(bytes: ByteArray): Uri = withContext(Dispatchers.IO) {
        require(bytes.size >= 4 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte()) {
            "Camera did not produce a JPEG buffer"
        }
        insert("image/jpeg", "jpg") { it.write(bytes) }
    }

    // A DNG is written straight from the sensor buffer; it is too large to materialize first.
    suspend fun saveDng(write: (OutputStream) -> Unit): Uri = withContext(Dispatchers.IO) {
        insert("image/x-adobe-dng", "dng", write)
    }

    private fun insert(mime: String, extension: String, write: (OutputStream) -> Unit): Uri {
        val name = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}_${UUID.randomUUID().toString().take(8)}.$extension"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera3")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Unable to create an image in MediaStore")
        return try {
            resolver.openOutputStream(uri, "w")?.use { write(it) }
                ?: error("Unable to open photo output")
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) == 1) {
                "Unable to publish the saved photo"
            }
            uri
        } catch (error: Throwable) {
            try { resolver.delete(uri, null, null) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            throw error
        }
    }
}

data class VideoOutput(val uri: Uri, val descriptor: ParcelFileDescriptor)
