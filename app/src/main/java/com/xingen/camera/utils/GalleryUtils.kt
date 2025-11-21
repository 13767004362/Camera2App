package com.xingen.camera.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.xingen.camera.MainActivity.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Created by HeXinGen  on 2025/11/21
 * Description:.
 */
object GalleryUtils {
    val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    /**
     * 保存bitmap 到相册中
     */
    fun  saveImageToGallery(context: Context, bitmap: Bitmap): Uri{
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            val outputStream: OutputStream? = uri?.let { context.contentResolver.openOutputStream(it) }
            outputStream?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            return  uri!!
        } catch (e: Exception) {
           throw  e
        }
    }

    /**
     * 保存mp4文件到相册中
     */
    fun  saveMp4FileToGallery(context: Context, sourceFilePath:String): Uri{
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            val outputStream: OutputStream? = uri?.let { context.contentResolver.openOutputStream(it) }
            outputStream?.use { outStream ->
                val inputStream = FileInputStream(sourceFilePath)
                inputStream.use { input ->
                    input.copyTo(outStream)
                }
            }
            return  uri!!
        } catch (e: Exception) {
            throw  e
        }
    }
}