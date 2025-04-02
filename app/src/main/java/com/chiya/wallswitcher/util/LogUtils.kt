package com.chiya.wallswitcher.util

import android.os.Environment
import android.util.Log
import com.chiya.wallswitcher.WallSwitcherApp
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志工具类
 */
object LogUtils {
    private var enableLogging = true
    private const val TAG = "WallSwitcher"
    private const val LOG_FILE_NAME = "WallSwitcher_log.txt"
    
    /**
     * 设置是否启用日志记录
     */
    fun setEnableLogging(enable: Boolean) {
        enableLogging = enable
    }
    
    /**
     * 记录日志
     */
    fun log(message: String) {
        // 始终输出到Logcat
        android.util.Log.d(TAG, message)
        
        if (enableLogging) {
            // 写入文件
            writeToFile(message)
        }
    }
    
    /**
     * 清除日志文件
     */
    fun clearLogs(): Boolean {
        return try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "清除日志文件失败: ${e.message}")
            false
        }
    }
    
    /**
     * 写入日志到文件
     */
    private fun writeToFile(message: String) {
        try {
            val logFile = getLogFile()
            
            // 确保父目录存在
            logFile.parentFile?.mkdirs()
            
            // 如果文件不存在，则创建
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            
            // 获取当前时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            
            // 写入日志
            FileWriter(logFile, true).use { writer ->
                writer.append("[$timestamp] $message\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入日志文件失败: ${e.message}")
        }
    }
    
    /**
     * 获取日志文件
     */
    private fun getLogFile(): File {
        // 使用应用的私有外部文件目录，不需要特殊权限
        val context = WallSwitcherApp.instance.applicationContext
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        // 使用日期作为文件名，方便区分
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        // 明确指定File构造函数参数类型，避免歧义
        return File(logDir.absolutePath, "wallswitcher_$today.log")
    }
} 