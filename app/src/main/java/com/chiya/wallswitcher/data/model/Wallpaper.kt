package com.chiya.wallswitcher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

/**
 * 壁纸实体类
 * 
 * @property id 壁纸ID
 * @property path 壁纸文件路径
 * @property name 壁纸名称
 * @property lastUsed 上次使用时间
 * @property useCount 使用次数
 */
@Entity(tableName = "wallpapers")
data class Wallpaper(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val name: String = File(path).name,
    val lastUsed: Long = 0,
    val useCount: Int = 0
) 