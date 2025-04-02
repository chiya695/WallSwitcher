package com.chiya.wallswitcher.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chiya.wallswitcher.data.model.Wallpaper
import kotlinx.coroutines.flow.Flow

/**
 * 壁纸数据访问对象
 */
@Dao
interface WallpaperDao {
    
    /**
     * 插入壁纸
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallpaper: Wallpaper): Long
    
    /**
     * 插入多个壁纸
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallpapers: List<Wallpaper>): List<Long>
    
    /**
     * 更新壁纸
     */
    @Update
    suspend fun update(wallpaper: Wallpaper)
    
    /**
     * 删除壁纸
     */
    @Delete
    suspend fun delete(wallpaper: Wallpaper)
    
    /**
     * 删除所有壁纸
     */
    @Query("DELETE FROM wallpapers")
    suspend fun deleteAll()
    
    /**
     * 获取所有壁纸
     */
    @Query("SELECT * FROM wallpapers")
    fun getAllWallpapers(): Flow<List<Wallpaper>>
    
    /**
     * 获取壁纸数量
     */
    @Query("SELECT COUNT(*) FROM wallpapers")
    suspend fun getWallpaperCount(): Int
    
    /**
     * 根据ID获取壁纸
     */
    @Query("SELECT * FROM wallpapers WHERE id = :id")
    suspend fun getWallpaperById(id: Long): Wallpaper?
    
    /**
     * 获取最近使用的壁纸
     */
    @Query("SELECT * FROM wallpapers ORDER BY lastUsed DESC LIMIT 1")
    suspend fun getLastUsedWallpaper(): Wallpaper?
    
    /**
     * 获取下一张壁纸（按ID顺序）
     */
    @Query("SELECT * FROM wallpapers WHERE id > :currentId ORDER BY id ASC LIMIT 1")
    suspend fun getNextWallpaper(currentId: Long): Wallpaper?
    
    /**
     * 获取第一张壁纸（按ID顺序）
     */
    @Query("SELECT * FROM wallpapers ORDER BY id ASC LIMIT 1")
    suspend fun getFirstWallpaper(): Wallpaper?
    
    /**
     * 获取随机壁纸
     */
    @Query("SELECT * FROM wallpapers ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWallpaper(): Wallpaper?
    
    /**
     * 获取随机壁纸（排除指定ID）
     */
    @Query("SELECT * FROM wallpapers WHERE id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWallpaperExcluding(excludeId: Long): Wallpaper?
    
    /**
     * 根据路径查找壁纸
     */
    @Query("SELECT * FROM wallpapers WHERE path = :path LIMIT 1")
    suspend fun findWallpaperByPath(path: String): Wallpaper?
} 