package com.chiya.wallswitcher.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.data.model.CropMethod
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.data.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 壁纸工具类
 */
object WallpaperUtils {
    
    /**
     * 设置壁纸并显示通知
     */
    fun setWallpaper(context: Context, wallpaper: Wallpaper, settings: Settings): Boolean {
        try {
            // 原有的设置壁纸逻辑
            val success = setWallpaperInternal(context, wallpaper, settings)
            
            // 显示通知
            if (settings.showToast) {
                // 显示Toast消息
                val message = if (success) {
                    context.getString(R.string.toast_wallpaper_changed)
                } else {
                    context.getString(R.string.home_toast_wallpaper_failed)
                }
                
                // 在主线程显示Toast
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                
                // 显示通知
                showWallpaperChangeNotification(context, success, wallpaper.name)
            }
            
            return success
        } catch (e: Exception) {
            LogUtils.log("设置壁纸时出错: ${e.message}")
            e.printStackTrace()
            
            // 显示失败通知
            if (settings.showToast) {
                // 在主线程显示Toast
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, context.getString(R.string.home_toast_wallpaper_failed), Toast.LENGTH_SHORT).show()
                }
                
                // 显示通知
                showWallpaperChangeNotification(context, false, wallpaper.name)
            }
            
            return false
        }
    }
    
    /**
     * 内部设置壁纸方法
     */
    private fun setWallpaperInternal(context: Context, wallpaper: Wallpaper, settings: Settings): Boolean {
        try {
            LogUtils.log("开始设置壁纸: ${wallpaper.path}")
            
            // 检查是否是内容URI
            val bitmap = if (wallpaper.path.startsWith("content://")) {
                LogUtils.log("检测到内容URI，使用ContentResolver加载")
                val inputStream = context.contentResolver.openInputStream(Uri.parse(wallpaper.path))
                if (inputStream == null) {
                    LogUtils.log("无法打开输入流: ${wallpaper.path}")
                    return false
                }
                
                // 获取屏幕尺寸
                val screenSize = getScreenSize(context)
                
                // 加载并裁剪壁纸
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (originalBitmap == null) {
                    LogUtils.log("解码位图失败: ${wallpaper.path}")
                    return false
                }
                
                // 裁剪图片
                val cropMethodOrdinal = settings.cropMethod
                val cropMethod = when (cropMethodOrdinal) {
                    1 -> CropMethod.CENTER
                    2 -> CropMethod.TOP
                    3 -> CropMethod.BOTTOM
                    else -> null  // 使用null代替CropMethod.NONE
                }
                
                if (cropMethodOrdinal == 0) {
                    // 使用原始位图设置壁纸
                    val result = setWallpaperWithBitmap(context, originalBitmap, wallpaper.name, settings)
                    // 回收位图
                    originalBitmap.recycle()
                    return result
                }
                
                cropBitmap(originalBitmap, screenSize, cropMethod)
            } else {
                // 检查文件是否存在
                val file = File(wallpaper.path)
                if (!file.exists()) {
                    LogUtils.log("壁纸文件不存在: ${wallpaper.path}")
                    return false
                }
                
                // 获取屏幕尺寸
                val screenSize = getScreenSize(context)
                
                // 加载并裁剪壁纸
                val originalBitmap = BitmapFactory.decodeFile(wallpaper.path)
                
                if (originalBitmap == null) {
                    LogUtils.log("加载壁纸失败: ${wallpaper.path}")
                    return false
                }
                
                // 裁剪图片
                val cropMethodOrdinal = settings.cropMethod
                val cropMethod = when (cropMethodOrdinal) {
                    1 -> CropMethod.CENTER
                    2 -> CropMethod.TOP
                    3 -> CropMethod.BOTTOM
                    else -> null  // 使用null代替CropMethod.NONE
                }
                
                if (cropMethodOrdinal == 0) {
                    // 使用原始位图设置壁纸
                    val result = setWallpaperWithBitmap(context, originalBitmap, wallpaper.name, settings)
                    // 回收位图
                    originalBitmap.recycle()
                    return result
                }
                
                cropBitmap(originalBitmap, screenSize, cropMethod)
            }
            
            if (bitmap == null) {
                LogUtils.log("加载壁纸失败: ${wallpaper.path}")
                return false
            }
            
            // 获取壁纸管理器
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // 设置壁纸
            if (settings.setLockScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 同时设置主屏幕和锁屏壁纸
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                LogUtils.log("已设置主屏幕和锁屏壁纸: ${wallpaper.name}")
            } else {
                // 仅设置主屏幕壁纸
                wallpaperManager.setBitmap(bitmap)
                LogUtils.log("已设置主屏幕壁纸: ${wallpaper.name}")
            }
            
            // 回收位图
            bitmap.recycle()
            
            return true
        } catch (e: IOException) {
            LogUtils.log("设置壁纸时出错: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 获取屏幕尺寸
     */
    private fun getScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }
    
    /**
     * 加载并裁剪壁纸
     */
    private fun loadAndCropWallpaper(path: String, screenSize: Point, cropMethodValue: Int): Bitmap? {
        try {
            // 获取图片原始尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            
            // 计算采样率，避免OOM
            val sampleSize = calculateInSampleSize(options, screenSize.x, screenSize.y)
            
            // 加载图片
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val originalBitmap = BitmapFactory.decodeFile(path, loadOptions) ?: return null
            
            // 裁剪图片
            val cropMethodOrdinal = cropMethodValue
            val cropMethod = when (cropMethodOrdinal) {
                2 -> CropMethod.TOP
                3 -> CropMethod.BOTTOM
                1 -> CropMethod.CENTER
                else -> null  // 使用null代替CropMethod.NONE
            }
            
            // 如果不需要裁剪，直接返回原图
            if (cropMethod == null) {
                return originalBitmap
            }
            
            return cropBitmap(originalBitmap, screenSize, cropMethod)
        } catch (e: Exception) {
            LogUtils.log("加载壁纸时出错: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 裁剪位图
     */
    private fun cropBitmap(bitmap: Bitmap, screenSize: Point, cropMethod: CropMethod?): Bitmap {
        // 如果cropMethod为null，直接返回原图的缩放版本
        if (cropMethod == null) {
            return Bitmap.createScaledBitmap(bitmap, screenSize.x, screenSize.y, true)
        }
        
        val screenWidth = screenSize.x
        val screenHeight = screenSize.y
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        
        // 计算缩放比例
        val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        
        // 如果比例相同，直接缩放
        if (screenRatio == bitmapRatio) {
            return Bitmap.createScaledBitmap(bitmap, screenWidth, screenHeight, true)
        }
        
        // 如果比例不同，需要裁剪
        val scaledBitmap: Bitmap
        val x: Int
        val y: Int
        
        if (screenRatio > bitmapRatio) {
            // 屏幕比图片宽，需要裁剪图片高度
            val scaledHeight = (bitmapWidth * screenHeight / screenWidth).toInt()
            
            y = when (cropMethod) {
                CropMethod.TOP -> 0
                CropMethod.BOTTOM -> bitmapHeight - scaledHeight
                CropMethod.CENTER -> (bitmapHeight - scaledHeight) / 2
                else -> (bitmapHeight - scaledHeight) / 2  // 默认居中
            }
            
            // 裁剪
            scaledBitmap = Bitmap.createBitmap(bitmap, 0, y, bitmapWidth, scaledHeight)
        } else {
            // 屏幕比图片高，需要裁剪图片宽度
            val scaledWidth = (bitmapHeight * screenWidth / screenHeight).toInt()
            
            x = when (cropMethod) {
                CropMethod.TOP, CropMethod.BOTTOM -> (bitmapWidth - scaledWidth) / 2
                CropMethod.CENTER -> (bitmapWidth - scaledWidth) / 2
                else -> (bitmapWidth - scaledWidth) / 2  // 默认居中
            }
            
            // 裁剪
            scaledBitmap = Bitmap.createBitmap(bitmap, x, 0, scaledWidth, bitmapHeight)
        }
        
        // 缩放到屏幕尺寸
        val result = Bitmap.createScaledBitmap(scaledBitmap, screenWidth, screenHeight, true)
        
        // 回收中间位图
        if (scaledBitmap != result) {
            scaledBitmap.recycle()
        }
        
        return result
    }
    
    /**
     * 显示壁纸切换通知
     */
    private fun showWallpaperChangeNotification(context: Context, success: Boolean, wallpaperName: String) {
        try {
            // 创建通知渠道
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "wallpaper_change"
                val channelName = "壁纸切换"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, channelName, importance)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
            
            // 创建通知
            val notificationBuilder = NotificationCompat.Builder(context, "wallpaper_change")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(
                    if (success) R.string.notification_title_wallpaper_success 
                    else R.string.notification_title_wallpaper_failed
                ))
                .setContentText(context.getString(
                    if (success) R.string.notification_content_wallpaper_success 
                    else R.string.notification_content_wallpaper_failed,
                    wallpaperName
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)  // 提高优先级
                .setAutoCancel(true)
            
            // 显示通知
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
            
            LogUtils.log("显示壁纸切换通知: ${if (success) "成功" else "失败"}")
        } catch (e: Exception) {
            LogUtils.log("显示通知时出错: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 使用位图设置壁纸
     */
    private fun setWallpaperWithBitmap(context: Context, bitmap: Bitmap, wallpaperName: String, settings: Settings): Boolean {
        try {
            // 获取壁纸管理器
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // 设置壁纸
            if (settings.setLockScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 同时设置主屏幕和锁屏壁纸
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                LogUtils.log("已设置主屏幕和锁屏壁纸: $wallpaperName")
            } else {
                // 仅设置主屏幕壁纸
                wallpaperManager.setBitmap(bitmap)
                LogUtils.log("已设置主屏幕壁纸: $wallpaperName")
            }
            
            return true
        } catch (e: Exception) {
            LogUtils.log("设置壁纸时出错: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
} 