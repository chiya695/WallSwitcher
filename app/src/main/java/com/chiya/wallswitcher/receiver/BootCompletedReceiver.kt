package com.chiya.wallswitcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chiya.wallswitcher.WallSwitcherApp
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.data.model.SwitchMode
import com.chiya.wallswitcher.service.WallpaperSwitchService
import com.chiya.wallswitcher.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 开机启动接收器
 */
class BootCompletedReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            LogUtils.log("接收到开机广播")
            
            // 在协程中执行
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // 获取应用实例
                    val app = context.applicationContext as WallSwitcherApp
                    
                    // 获取设置
                    val settings = app.settingsRepository.settingsFlow.first()
                    
                    // 如果设置为后台服务模式，则启动服务
                    if (settings.switchMode == SwitchMode.SERVICE.ordinal) {
                        startWallpaperService(context, settings)
                    }
                } catch (e: Exception) {
                    LogUtils.log("开机启动服务时出错: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * 启动壁纸切换服务
     */
    private fun startWallpaperService(context: Context, settings: Settings) {
        // 检查是否有壁纸文件夹
        if (settings.folderPath.isNotEmpty()) {
            val serviceIntent = Intent(context, WallpaperSwitchService::class.java)
            context.startService(serviceIntent)
            LogUtils.log("开机后启动壁纸切换服务")
        } else {
            LogUtils.log("未设置壁纸文件夹，不启动服务")
        }
    }
} 