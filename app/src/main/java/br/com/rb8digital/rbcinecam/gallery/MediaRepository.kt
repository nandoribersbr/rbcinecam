package br.com.rb8digital.rbcinecam.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoItem(val uri: Uri, val name: String, val durationMs: Long, val dateAdded: Long)

class MediaRepository(private val context: Context) {
    suspend fun videos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?",
            arrayOf("RB_CineCam_%"),
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val duration = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val date = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (c.moveToNext()) {
                result += VideoItem(
                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(id)),
                    name = c.getString(name),
                    durationMs = c.getLong(duration),
                    dateAdded = c.getLong(date)
                )
            }
        }
        result
    }
}
