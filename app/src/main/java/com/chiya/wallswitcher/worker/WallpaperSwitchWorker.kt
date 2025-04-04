package com.chiya.wallswitcher.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.WallSwitcherApp
import com.chiya.wallswitcher.data.model.SwitchOrder
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.util.LogUtils
import com.chiya.wallswitcher.util.WallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WallpaperSwitchWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        try {
            LogUtils.log("WallpaperSwitchWorker: 开始执行壁纸切换")
            
            val app = context.applicationContext as WallSwitcherApp
            val settings = app.settingsRepository.settingsFlow.first()
            
            // 获取下一张壁纸
            val currentWallpaper = app.wallpaperRepository.getLastUsedWallpaper()
            val nextWallpaper = when (settings.switchOrder) {
                SwitchOrder.SEQUENTIAL.ordinal -> {
                    // 顺序切换
                    if (currentWallpaper != null) {
                        app.wallpaperRepository.getNextWallpaper(currentWallpaper.id)
                    } else {
                        app.wallpaperRepository.getFirstWallpaper()
                    }
                }
                else -> {
                    // 随机切换
                    app.wallpaperRepository.getRandomWallpaper(settings.avoidRepeat, currentWallpaper?.id ?: -1)
                }
            }
            
            if (nextWallpaper == null) {
                LogUtils.log("WallpaperSwitchWorker: 没有可用的壁纸")
                return Result.failure()
            }
            
            // 预处理图片（降低分辨率）
            val success = setWallpaperOptimized(context, nextWallpaper, settings)
            
            return if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            LogUtils.log("WallpaperSwitchWorker: 切换壁纸时出错: ${e.message}")
            e.printStackTrace()
            return Result.failure()
        } finally {
            setForeground(
                ForegroundInfo(
                    NOTIFICATION_ID,
                    createProgressNotification("切换壁纸完成")
                )
            )
        }
    }
    
    private suspend fun setWallpaperOptimized(context: Context, wallpaper: Wallpaper, settings: Settings): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 使用新的加载方法，根据设置决定是否压缩
                val bitmap = WallpaperUtils.loadWallpaperBitmap(context, wallpaper.path, settings)
                if (bitmap != null) {
                    val success = WallpaperUtils.setWallpaperWithBitmap(context, bitmap, wallpaper.name, settings)
                    
                    // 更新壁纸使用记录
                    if (success) {
                        val app = context.applicationContext as WallSwitcherApp
                        val updatedWallpaper = wallpaper.copy(
                            lastUsed = System.currentTimeMillis(),
                            useCount = wallpaper.useCount + 1
                        )
                        app.wallpaperRepository.updateWallpaperUsage(updatedWallpaper)
                    }
                    
                    return@withContext success
                }
                return@withContext false
            } catch (e: Exception) {
                LogUtils.log("设置优化壁纸时出错: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    private fun createProgressNotification(message: String): Notification {
        val channelId = "wallpaper_switch_progress"
        
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "壁纸切换进度",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建通知
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("壁纸切换")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()
    }
} 