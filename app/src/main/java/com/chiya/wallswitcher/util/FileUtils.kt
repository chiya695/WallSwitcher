package com.chiya.wallswitcher.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/**
 * 文件工具类
 */
object FileUtils {
    
    /**
     * 支持的图片格式
     */
    private val SUPPORTED_IMAGE_EXTENSIONS = listOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    )
    
    /**
     * 检查文件是否为支持的图片格式
     */
    fun isSupportedImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_IMAGE_EXTENSIONS
    }
    
    /**
     * 从 Uri 获取文件路径
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        // 处理 file:// 类型的 Uri
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            return uri.path
        }
        
        // 处理 content:// 类型的 Uri
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            // 处理 DocumentsProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 处理 ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    
                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${context.getExternalFilesDir(null)?.path}/${split[1]}"
                    }
                }
                // 处理 MediaProvider
                else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
            // 处理 MediaStore (普通 content:// Uri)
            else {
                return getDataColumn(context, uri, null, null)
            }
        }
        
        return null
    }
    
    /**
     * 获取数据列
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        uri ?: return null
        
        val column = "_data"
        val projection = arrayOf(column)
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        
        return null
    }
    
    /**
     * 检查是否为外部存储文档
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    /**
     * 检查是否为媒体文档
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
    
    /**
     * 获取文件夹名称
     */
    fun getFolderName(path: String): String {
        return try {
            val file = File(path)
            file.name
        } catch (e: Exception) {
            path.substringAfterLast('/', "未知文件夹")
        }
    }
} 