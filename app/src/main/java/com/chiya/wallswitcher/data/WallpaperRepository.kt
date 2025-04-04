package com.chiya.wallswitcher.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.chiya.wallswitcher.data.dao.WallpaperDao
import com.chiya.wallswitcher.data.model.Wallpaper
import com.chiya.wallswitcher.util.FileUtils
import com.chiya.wallswitcher.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 壁纸仓库
 */
class WallpaperRepository(private val wallpaperDao: WallpaperDao) {
    
    /**
     * 获取所有壁纸
     */
    fun getAllWallpapers(): Flow<List<Wallpaper>> {
        return wallpaperDao.getAllWallpapers()
    }
    
    /**
     * 获取壁纸数量
     */
    suspend fun getWallpaperCount(): Int {
        return wallpaperDao.getWallpaperCount()
    }
    
    /**
     * 根据ID获取壁纸
     */
    suspend fun getWallpaperById(id: Long): Wallpaper? {
        return wallpaperDao.getWallpaperById(id)
    }
    
    /**
     * 获取最近使用的壁纸
     */
    suspend fun getLastUsedWallpaper(): Wallpaper? {
        return wallpaperDao.getLastUsedWallpaper()
    }
    
    /**
     * 获取下一张壁纸（顺序模式）
     */
    suspend fun getNextWallpaper(currentId: Long): Wallpaper? {
        // 尝试获取ID大于当前ID的壁纸
        val nextWallpaper = wallpaperDao.getNextWallpaper(currentId)
        
        // 如果没有更大ID的壁纸，则返回第一张壁纸（循环）
        return nextWallpaper ?: wallpaperDao.getFirstWallpaper()
    }
    
    /**
     * 获取随机壁纸
     */
    suspend fun getRandomWallpaper(avoidRepeat: Boolean = true, currentId: Long = -1): Wallpaper? {
        return if (avoidRepeat && currentId > 0) {
            // 避免重复，排除当前壁纸
            wallpaperDao.getRandomWallpaperExcluding(currentId)
        } else {
            // 完全随机
            wallpaperDao.getRandomWallpaper()
        }
    }
    
    /**
     * 更新壁纸使用信息
     */
    suspend fun updateWallpaperUsage(wallpaper: Wallpaper) {
        val updatedWallpaper = wallpaper.copy(
            lastUsed = System.currentTimeMillis(),
            useCount = wallpaper.useCount + 1
        )
        wallpaperDao.update(updatedWallpaper)
    }
    
    /**
     * 从文件夹加载壁纸
     */
    suspend fun loadWallpapersFromFolder(context: Context, folderUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            LogUtils.log("开始从文件夹加载壁纸: $folderUri")
            
            // 清空现有壁纸
            wallpaperDao.deleteAll()
            
            val wallpapers = mutableListOf<Wallpaper>()
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            
            if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
                LogUtils.log("无效的文件夹URI: $folderUri")
                return@withContext 0
            }
            
            // 遍历文件夹中的所有文件
            for (file in documentFile.listFiles()) {
                if (file.isFile && FileUtils.isSupportedImageFile(file.name ?: "")) {
                    val filePath = FileUtils.getPathFromUri(context, file.uri)
                    if (filePath != null) {
                        wallpapers.add(Wallpaper(path = filePath))
                        LogUtils.log("添加壁纸: $filePath")
                    }
                }
            }
            
            // 批量插入壁纸
            if (wallpapers.isNotEmpty()) {
                wallpaperDao.insertAll(wallpapers)
            }
            
            LogUtils.log("壁纸加载完成，共 ${wallpapers.size} 张")
            return@withContext wallpapers.size
        } catch (e: Exception) {
            LogUtils.log("加载壁纸时出错: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }
    
    /**
     * 从文件夹路径加载壁纸（传统文件系统）
     */
    suspend fun loadWallpapersFromFolderPath(folderPath: String): Int = withContext(Dispatchers.IO) {
        try {
            LogUtils.log("开始从文件夹路径加载壁纸: $folderPath")
            
            // 清空现有壁纸
            wallpaperDao.deleteAll()
            
            val folder = File(folderPath)
            if (!folder.exists() || !folder.isDirectory) {
                LogUtils.log("无效的文件夹路径: $folderPath")
                return@withContext 0
            }
            
            val wallpapers = mutableListOf<Wallpaper>()
            
            // 遍历文件夹中的所有文件
            folder.listFiles()?.forEach { file ->
                if (file.isFile && FileUtils.isSupportedImageFile(file.name)) {
                    wallpapers.add(Wallpaper(path = file.absolutePath))
                    LogUtils.log("添加壁纸: ${file.absolutePath}")
                }
            }
            
            // 批量插入壁纸
            if (wallpapers.isNotEmpty()) {
                wallpaperDao.insertAll(wallpapers)
            }
            
            LogUtils.log("壁纸加载完成，共 ${wallpapers.size} 张")
            return@withContext wallpapers.size
        } catch (e: Exception) {
            LogUtils.log("加载壁纸时出错: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }

    /**
     * 从DocumentUri加载壁纸
     */
    suspend fun loadWallpapersFromUri(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            LogUtils.log("开始从DocumentUri加载壁纸: $uri")
            
            // 清空现有壁纸
            wallpaperDao.deleteAll()
            LogUtils.log("已清空现有壁纸")
            
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            LogUtils.log("DocumentFile获取结果: ${documentFile != null}")
            
            if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
                LogUtils.log("无效的DocumentUri: $uri, 存在: ${documentFile?.exists()}, 是目录: ${documentFile?.isDirectory}")
                return@withContext 0
            }
            
            val files = documentFile.listFiles()
            LogUtils.log("文件夹中的文件数量: ${files.size}")
            
            val wallpapers = mutableListOf<Wallpaper>()
            
            // 遍历文件夹中的所有文件
            files.forEach { file ->
                LogUtils.log("检查文件: ${file.name}, 类型: ${file.type}, 是文件: ${file.isFile}")
                if (file.isFile && file.type?.startsWith("image/") == true) {
                    val name = file.name ?: "Unknown"
                    val path = file.uri.toString()
                    LogUtils.log("添加壁纸: $name, 路径: $path")
                    wallpapers.add(Wallpaper(
                        id = 0,
                        path = path,
                        name = name,
                        lastUsed = 0
                    ))
                }
            }
            
            // 批量插入壁纸
            if (wallpapers.isNotEmpty()) {
                LogUtils.log("插入壁纸到数据库: ${wallpapers.size}张")
                wallpaperDao.insertAll(wallpapers)
            } else {
                LogUtils.log("没有找到有效的壁纸图片")
            }
            
            LogUtils.log("壁纸加载完成，共 ${wallpapers.size} 张")
            return@withContext wallpapers.size
        } catch (e: Exception) {
            LogUtils.log("加载壁纸时出错: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }

    /**
     * 分页加载壁纸
     */
    suspend fun loadWallpapersFromUriPaged(context: Context, uri: Uri, pageSize: Int = 100): Int = withContext(Dispatchers.IO) {
        try {
            LogUtils.log("开始从DocumentUri分页加载壁纸: $uri")
            
            // 清空现有壁纸
            wallpaperDao.deleteAll()
            LogUtils.log("已清空现有壁纸")
            
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            LogUtils.log("DocumentFile获取结果: ${documentFile != null}")
            
            if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory) {
                LogUtils.log("无效的DocumentUri: $uri, 存在: ${documentFile?.exists()}, 是目录: ${documentFile?.isDirectory}")
                return@withContext 0
            }
            
            val files = documentFile.listFiles()
            LogUtils.log("文件夹中的文件数量: ${files.size}")
            
            // 只获取图片文件
            val imageFiles = files.filter { file -> 
                file.isFile && file.type?.startsWith("image/") == true 
            }
            
            // 分批处理图片文件
            val totalImages = imageFiles.size
            var processedCount = 0
            
            imageFiles.chunked(pageSize).forEach { batch ->
                val wallpapers = batch.map { file ->
                    val name = file.name ?: "Unknown"
                    val path = file.uri.toString()
                    Wallpaper(
                        id = 0,
                        path = path,
                        name = name,
                        lastUsed = 0
                    )
                }
                
                // 批量插入壁纸
                wallpaperDao.insertAll(wallpapers)
                
                processedCount += batch.size
                LogUtils.log("已处理 $processedCount/$totalImages 张图片")
            }
            
            LogUtils.log("分页加载壁纸完成，共加载 $processedCount 张图片")
            return@withContext processedCount
        } catch (e: Exception) {
            LogUtils.log("分页加载壁纸时出错: ${e.message}")
            e.printStackTrace()
            return@withContext 0
        }
    }

    /**
     * 获取预加载的壁纸
     */
    suspend fun getWallpapersForPreload(limit: Int): List<Wallpaper> {
        return wallpaperDao.getWallpapersForPreload(limit)
    }

    /**
     * 获取所有壁纸
     */
    suspend fun getWallpapers(): List<Wallpaper> {
        return wallpaperDao.getAllWallpapersAsList()
    }

    /**
     * 获取下一张顺序壁纸
     */
    suspend fun getNextSequentialWallpaper(currentId: Long): Wallpaper? {
        return wallpaperDao.getNextWallpaper(currentId)
    }

    /**
     * 获取第一张壁纸
     */
    suspend fun getFirstWallpaper(): Wallpaper? {
        return wallpaperDao.getFirstWallpaper()
    }
} 