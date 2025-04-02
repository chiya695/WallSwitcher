package com.chiya.wallswitcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chiya.wallswitcher.MainActivity
import com.chiya.wallswitcher.R
import com.chiya.wallswitcher.WallSwitcherApp
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.data.model.SwitchOrder
import com.chiya.wallswitcher.data.model.TimeUnit
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.util.LogUtils
import com.chiya.wallswitcher.util.WallpaperUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 壁纸切换服务
 */
class WallpaperSwitchService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var switchJob: Job? = null
    private var currentSettings: Settings? = null
    private var currentWallpaperId: Long = -1
    
    override fun onCreate() {
        super.onCreate()
        LogUtils.log("壁纸切换服务已创建")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.log("壁纸切换服务已启动")
        
        // 创建前台服务通知
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 启动壁纸切换任务
        startWallpaperSwitchTask()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 取消壁纸切换任务
        switchJob?.cancel()
        
        LogUtils.log("壁纸切换服务已销毁")
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, "wallpaper_service")
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    /**
     * 启动壁纸切换任务
     */
    private fun startWallpaperSwitchTask() {
        // 取消现有任务
        switchJob?.cancel()
        
        // 创建新任务
        switchJob = serviceScope.launch {
            try {
                // 获取应用实例
                val app = application as WallSwitcherApp
                
                // 获取设置
                currentSettings = app.settingsRepository.settingsFlow.first()
                
                // 获取当前壁纸
                val lastWallpaper = app.wallpaperRepository.getLastUsedWallpaper()
                currentWallpaperId = lastWallpaper?.id ?: -1
                
                // 计算切换间隔（毫秒）
                val intervalMs = calculateIntervalMs(currentSettings!!)
                
                LogUtils.log("壁纸切换任务已启动，间隔: $intervalMs 毫秒")
                
                // 循环切换壁纸
                while (true) {
                    // 等待指定时间
                    delay(intervalMs)
                    
                    // 重新获取最新设置
                    currentSettings = app.settingsRepository.settingsFlow.first()
                    
                    // 检查是否有壁纸可用
                    val wallpaperCount = app.wallpaperRepository.getWallpaperCount()
                    if (wallpaperCount == 0) {
                        LogUtils.log("没有可用的壁纸，跳过切换")
                        continue
                    }
                    
                    // 获取下一张壁纸
                    val nextWallpaper = getNextWallpaper(app, currentSettings!!)
                    if (nextWallpaper == null) {
                        LogUtils.log("获取下一张壁纸失败，跳过切换")
                        continue
                    }
                    
                    // 设置壁纸
                    val success = WallpaperUtils.setWallpaper(this@WallpaperSwitchService, nextWallpaper, currentSettings!!)
                    if (success) {
                        // 更新壁纸使用信息
                        app.wallpaperRepository.updateWallpaperUsage(nextWallpaper)
                        currentWallpaperId = nextWallpaper.id
                        
                        LogUtils.log("壁纸切换成功: ${nextWallpaper.name}")
                    } else {
                        LogUtils.log("壁纸切换失败")
                    }
                    
                    // 重新计算间隔（设置可能已更改）
                    val newIntervalMs = calculateIntervalMs(currentSettings!!)
                    if (newIntervalMs != intervalMs) {
                        LogUtils.log("切换间隔已更新: $newIntervalMs 毫秒")
                        break  // 退出循环，重新启动任务
                    }
                }
                
                // 如果间隔时间已更改，重新启动任务
                startWallpaperSwitchTask()
                
            } catch (e: Exception) {
                LogUtils.log("壁纸切换任务出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 计算切换间隔（毫秒）
     */
    private fun calculateIntervalMs(settings: Settings): Long {
        val baseInterval = settings.intervalValue.toLong()
        val multiplier = when (settings.intervalUnit) {
            TimeUnit.MINUTE.ordinal -> TimeUnit.MINUTE.multiplier
            TimeUnit.HOUR.ordinal -> TimeUnit.HOUR.multiplier
            TimeUnit.DAY.ordinal -> TimeUnit.DAY.multiplier
            else -> TimeUnit.MINUTE.multiplier
        }
        
        return baseInterval * multiplier
    }
    
    /**
     * 获取下一张壁纸
     */
    private suspend fun getNextWallpaper(app: WallSwitcherApp, settings: Settings): Wallpaper? {
        return withContext(Dispatchers.IO) {
            try {
                when (settings.switchOrder) {
                    SwitchOrder.SEQUENTIAL.ordinal -> {
                        // 顺序切换
                        app.wallpaperRepository.getNextWallpaper(currentWallpaperId)
                    }
                    else -> {
                        // 随机切换
                        app.wallpaperRepository.getRandomWallpaper(settings.avoidRepeat, currentWallpaperId)
                    }
                }
            } catch (e: Exception) {
                LogUtils.log("获取下一张壁纸时出错: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1
    }
} 