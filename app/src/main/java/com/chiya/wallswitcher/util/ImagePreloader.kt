package com.chiya.wallswitcher.util

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import com.chiya.wallswitcher.data.model.Wallpaper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 图片预加载工具
 */
object ImagePreloader {
    
    /**
     * 预加载一批壁纸的缩略图
     */
    fun preloadThumbnails(context: Context, wallpapers: List<Wallpaper>, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val imageLoader = ImageLoader(context)
            
            wallpapers.forEach { wallpaper ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(if (wallpaper.path.startsWith("content://")) Uri.parse(wallpaper.path) else wallpaper.path)
                        .size(200)  // 缩略图尺寸
                        .memoryCacheKey(wallpaper.path)
                        .diskCacheKey(wallpaper.path)
                        .build()
                    
                    imageLoader.enqueue(request)
                } catch (e: Exception) {
                    LogUtils.log("预加载缩略图失败: ${wallpaper.name}, ${e.message}")
                }
            }
        }
    }
} 