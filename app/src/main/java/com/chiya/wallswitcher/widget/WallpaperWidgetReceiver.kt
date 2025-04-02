package com.chiya.wallswitcher.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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
                    
                    // 设置壁纸
                    val success = WallpaperUtils.setWallpaper(context, nextWallpaper, settings)
                    if (success) {
                        // 更新壁纸使用信息
                        app.wallpaperRepository.updateWallpaperUsage(nextWallpaper)
                        LogUtils.log("小组件: 壁纸切换成功: ${nextWallpaper.name}")
                    } else {
                        LogUtils.log("小组件: 壁纸切换失败")
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
    
    companion object {
        const val ACTION_SWITCH_WALLPAPER = "com.chiya.wallswitcher.ACTION_SWITCH_WALLPAPER"
    }
} 