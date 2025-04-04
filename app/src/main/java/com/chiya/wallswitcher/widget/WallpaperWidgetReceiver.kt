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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.chiya.wallswitcher.worker.WallpaperSwitchWorker

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
            handleSwitchWallpaper(context)
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
    
    /**
     * 处理壁纸切换请求
     */
    private fun handleSwitchWallpaper(context: Context) {
        LogUtils.log("小组件: 收到切换壁纸请求")
        
        // 使用WorkManager代替直接在BroadcastReceiver中执行耗时操作
        val switchWallpaperWork = OneTimeWorkRequestBuilder<WallpaperSwitchWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        WorkManager.getInstance(context).enqueue(switchWallpaperWork)
        
        // 显示正在切换的提示
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "正在切换壁纸...", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        const val ACTION_SWITCH_WALLPAPER = "com.chiya.wallswitcher.ACTION_SWITCH_WALLPAPER"
    }
} 