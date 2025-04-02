package com.chiya.wallswitcher.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chiya.wallswitcher.WallSwitcherApp
import com.chiya.wallswitcher.data.model.Settings
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.service.WallpaperSwitchService
import com.chiya.wallswitcher.util.FileUtils
import com.chiya.wallswitcher.util.LogUtils
import com.chiya.wallswitcher.util.WallpaperUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * 主视图模型
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as WallSwitcherApp
    
    // 壁纸列表
    val wallpapers = app.wallpaperRepository.getAllWallpapers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // 设置
    val settings = app.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())
    
    // 服务状态
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning = _serviceRunning.asStateFlow()
    
    // 加载状态
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    
    // 当前文件夹名称
    val folderName = settings.combine(wallpapers) { settings, _ ->
        if (settings.folderPath.isEmpty()) {
            ""
        } else {
            FileUtils.getFolderName(settings.folderPath)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    
    // 初始化
    init {
        LogUtils.log("MainViewModel 初始化")
        checkServiceRunning()
    }
    
    /**
     * 检查服务是否正在运行
     */
    private fun checkServiceRunning() {
        // 这里简单实现，实际应用中可能需要更复杂的检查
        _serviceRunning.value = false
    }
    
    /**
     * 从文件夹加载壁纸
     */
    fun loadWallpapersFromFolder(folderPath: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 更新设置中的文件夹路径
                app.settingsRepository.updateFolderPath(folderPath)
                
                // 加载壁纸
                app.wallpaperRepository.loadWallpapersFromFolderPath(folderPath)
                
                LogUtils.log("从文件夹加载壁纸成功: $folderPath")
            } catch (e: Exception) {
                LogUtils.log("从文件夹加载壁纸失败: ${e.message}")
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 从 Uri 加载壁纸
     */
    fun loadWallpapersFromUri(uri: Uri) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 获取路径
                val context = getApplication<Application>()
                val path = FileUtils.getPathFromUri(context, uri)
                
                if (path != null) {
                    // 更新设置中的文件夹路径
                    app.settingsRepository.updateFolderPath(path)
                    
                    // 加载壁纸
                    app.wallpaperRepository.loadWallpapersFromFolderPath(path)
                    
                    LogUtils.log("从 Uri 加载壁纸成功: $path")
                } else {
                    // 尝试从 DocumentFile 加载
                    val count = app.wallpaperRepository.loadWallpapersFromUri(context, uri)
                    if (count > 0) {
                        // 更新设置中的文件夹路径（使用 Uri 字符串）
                        app.settingsRepository.updateFolderPath(uri.toString())
                        LogUtils.log("从 DocumentFile 加载壁纸成功: $uri")
                    } else {
                        LogUtils.log("从 Uri 加载壁纸失败: 无法获取路径")
                    }
                }
            } catch (e: Exception) {
                LogUtils.log("从 Uri 加载壁纸失败: ${e.message}")
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 设置壁纸
     */
    fun setWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val success = WallpaperUtils.setWallpaper(context, wallpaper, settings.value)
                
                if (success) {
                    // 更新壁纸使用信息
                    app.wallpaperRepository.updateWallpaperUsage(wallpaper)
                    LogUtils.log("设置壁纸成功: ${wallpaper.name}")
                } else {
                    LogUtils.log("设置壁纸失败")
                }
            } catch (e: Exception) {
                LogUtils.log("设置壁纸时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 随机切换壁纸
     */
    fun switchRandomWallpaper() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                
                // 获取当前壁纸
                val lastWallpaper = app.wallpaperRepository.getLastUsedWallpaper()
                val currentId = lastWallpaper?.id ?: -1
                
                // 获取随机壁纸
                val nextWallpaper = app.wallpaperRepository.getRandomWallpaper(
                    settings.value.avoidRepeat,
                    currentId
                )
                
                if (nextWallpaper != null) {
                    // 设置壁纸
                    val success = WallpaperUtils.setWallpaper(context, nextWallpaper, settings.value)
                    
                    if (success) {
                        // 更新壁纸使用信息
                        app.wallpaperRepository.updateWallpaperUsage(nextWallpaper)
                        LogUtils.log("随机切换壁纸成功: ${nextWallpaper.name}")
                    } else {
                        LogUtils.log("随机切换壁纸失败")
                    }
                } else {
                    LogUtils.log("获取随机壁纸失败")
                }
            } catch (e: Exception) {
                LogUtils.log("随机切换壁纸时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 启动壁纸切换服务
     */
    fun startWallpaperService() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val serviceIntent = Intent(context, WallpaperSwitchService::class.java)
                context.startService(serviceIntent)
                _serviceRunning.value = true
                LogUtils.log("启动壁纸切换服务")
            } catch (e: Exception) {
                LogUtils.log("启动壁纸切换服务时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 停止壁纸切换服务
     */
    fun stopWallpaperService() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val serviceIntent = Intent(context, WallpaperSwitchService::class.java)
                context.stopService(serviceIntent)
                _serviceRunning.value = false
                LogUtils.log("停止壁纸切换服务")
            } catch (e: Exception) {
                LogUtils.log("停止壁纸切换服务时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 更新设置
     */
    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            try {
                app.settingsRepository.updateSettings(settings)
                
                // 更新日志设置
                LogUtils.setEnableLogging(settings.enableLogging)
                
                LogUtils.log("更新设置成功")
            } catch (e: Exception) {
                LogUtils.log("更新设置时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 清除日志
     */
    fun clearLogs() {
        viewModelScope.launch {
            try {
                val success = LogUtils.clearLogs()
                if (success) {
                    LogUtils.log("清除日志成功")
                } else {
                    LogUtils.log("清除日志失败")
                }
            } catch (e: Exception) {
                LogUtils.log("清除日志时出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 分享日志文件
     */
    fun shareLogs() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val logDir = File(context.getExternalFilesDir(null), "logs")
                
                if (!logDir.exists() || !logDir.isDirectory) {
                    LogUtils.log("日志目录不存在")
                    return@launch
                }
                
                // 获取最新的日志文件
                val logFiles = logDir.listFiles()?.filter { it.name.endsWith(".log") }
                    ?.sortedByDescending { it.lastModified() }
                
                if (logFiles.isNullOrEmpty()) {
                    LogUtils.log("没有找到日志文件")
                    return@launch
                }
                
                // 创建分享意图
                val latestLogFile = logFiles.first()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    latestLogFile
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "壁纸切换器日志")
                    putExtra(Intent.EXTRA_TEXT, "请查看附件中的日志文件")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // 启动分享活动
                val shareIntent = Intent.createChooser(intent, "分享日志文件")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                
                LogUtils.log("分享日志文件: ${latestLogFile.name}")
            } catch (e: Exception) {
                LogUtils.log("分享日志文件失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
} 