package com.chiya.wallswitcher.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.WallSwitcherApp
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.data.model.SwitchOrder
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.util.LogUtils
import com.chiya.wallswitcher.util.WallpaperUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 壁纸小部件接收器
 */
class WallpaperWidgetReceiver : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        LogUtils.log("更新壁纸小部件")
        
        // 为每个小部件实例更新
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_SWITCH_WALLPAPER) {
            LogUtils.log("小组件: 收到切换壁纸请求")
            
            // 使用协程在后台线程中执行壁纸切换
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 获取应用实例
                    val app = context.applicationContext as WallSwitcherApp
                    
                    // 获取设置
                    val settings = app.settingsRepository.settingsFlow.first()
                    
                    // 检查是否有壁纸
                    val wallpaperCount = app.wallpaperRepository.getWallpaperCount()
                    if (wallpaperCount == 0) {
                        LogUtils.log("小组件: 没有可用的壁纸")
                        return@launch
                    }
                    
                    // 获取当前壁纸
                    val lastWallpaper = app.wallpaperRepository.getLastUsedWallpaper()
                    val currentId = lastWallpaper?.id ?: -1
                    
                    // 获取下一张壁纸
                    val nextWallpaper = if (settings.switchOrder == SwitchOrder.RANDOM.ordinal) {
                        // 随机切换
                        app.wallpaperRepository.getRandomWallpaper(settings.avoidRepeat, currentId)
                    } else {
                        // 顺序切换
                        app.wallpaperRepository.getNextWallpaper(currentId)
                    }
                    
                    if (nextWallpaper == null) {
                        LogUtils.log("小组件: 获取下一张壁纸失败")
                        return@launch
                    }
                    
                    // 设置壁纸，传递false给showNotification参数，避免WallpaperUtils显示通知
                    val success = WallpaperUtils.setWallpaper(context, nextWallpaper, settings.copy(showToast = false))
                    if (success) {
                        // 更新壁纸使用信息
                        app.wallpaperRepository.updateWallpaperUsage(nextWallpaper)
                        LogUtils.log("小组件: 壁纸切换成功: ${nextWallpaper.name}")
                        
                        // 只在设置中启用了通知时显示
                        if (settings.showToast) {
                            showNotification(context, "壁纸切换成功", "已设置壁纸: ${nextWallpaper.name}")
                        }
                    } else {
                        LogUtils.log("小组件: 壁纸切换失败")
                        
                        // 失败时总是显示通知
                        showNotification(context, "壁纸切换失败", "无法设置壁纸")
                    }
                } catch (e: Exception) {
                    LogUtils.log("小组件: 切换壁纸时出错: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * 更新小部件
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // 创建远程视图
        val views = RemoteViews(context.packageName, R.layout.widget_wallpaper)
        
        // 创建点击意图
        val intent = Intent(context, WallpaperWidgetReceiver::class.java).apply {
            action = ACTION_SWITCH_WALLPAPER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 设置点击事件
        views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
        
        // 更新小部件
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    // 添加显示通知的辅助方法
    private fun showNotification(context: Context, title: String, message: String) {
        try {
            // 创建通知渠道（Android 8.0+需要）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "wallpaper_change_channel"
                val channelName = "壁纸切换"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, channelName, importance)
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                
                // 创建通知
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                
                // 显示通知
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            } else {
                // 对于Android 8.0以下的设备，尝试使用Toast
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "$title: $message", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            LogUtils.log("显示通知失败: ${e.message}")
        }
    }
    
    companion object {
        const val ACTION_SWITCH_WALLPAPER = "com.chiya.wallswitcher.ACTION_SWITCH_WALLPAPER"
    }
} 