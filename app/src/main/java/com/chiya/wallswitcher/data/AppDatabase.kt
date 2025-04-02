package com.chiya.wallswitcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chiya.wallswitcher.data.dao.WallpaperDao
import com.chiya.wallswitcher.data.model.Wallpaper

/**
 * 应用数据库
 */
@Database(entities = [Wallpaper::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 获取壁纸DAO
     */
    abstract fun wallpaperDao(): WallpaperDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallswitcher_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 