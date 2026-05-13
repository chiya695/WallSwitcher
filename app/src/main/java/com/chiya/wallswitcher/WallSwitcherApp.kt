package com.chiya.wallswitcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.chiya.wallswitcher.data.AppDatabase
import com.chiya.wallswitcher.data.SettingsRepository
import com.chiya.wallswitcher.data.WallpaperRepository
import com.chiya.wallswitcher.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WallSwitcherApp : Application(), Configuration.Provider {

    // 数据库实例
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    // 仓库实例
    val wallpaperRepository by lazy { WallpaperRepository(database.wallpaperDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    companion object {
        lateinit var instance: WallSwitcherApp
            private set
        
        const val CHANNEL_ID = "wallpaper_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 注意：不再手动调用 WorkManager.initialize()。
        // 使用 Configuration.Provider 接口（getWorkManagerConfiguration）提供自定义配置，
        // Manifest 中已移除默认初始化器（tools:node="remove"），避免双重初始化冲突。
        
        // 初始化日志设置
        initLogging()
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("wallpaper_service", name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initLogging() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                LogUtils.setEnableLogging(settings.enableLogging)
                LogUtils.log("应用启动")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 