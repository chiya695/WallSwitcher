package com.chiya.wallswitcher.data.model

/**
 * 应用设置数据类
 */
data class Settings(
    // 壁纸文件夹路径
    val folderPath: String = "",
    
    // 切换方式：0-后台服务自动切换，1-桌面小部件手动切换
    val switchMode: Int = 0,
    
    // 切换顺序：0-随机切换，1-顺序切换
    val switchOrder: Int = 0,
    
    // 是否避免重复（仅在随机模式下有效）
    val avoidRepeat: Boolean = true,
    
    // 切换间隔值
    val intervalValue: Int = 30,
    
    // 切换间隔单位：0-分钟，1-小时，2-天
    val intervalUnit: Int = 0,
    
    // 是否同时设置锁屏壁纸
    val setLockScreen: Boolean = false,
    
    // 是否显示切换通知
    val showToast: Boolean = true,
    
    // 裁剪方式：0=不裁剪(NONE), 1=居中裁剪(CENTER), 2=顶部裁剪(TOP), 3=底部裁剪(BOTTOM)
    val cropMethod: Int = 0,
    
    // 是否启用日志记录
    val enableLogging: Boolean = false,
    
    // 新增分页加载设置
    val pageSize: Int = 100,  // 每页加载的图片数量
    val preloadThreshold: Int = 20,  // 预加载阈值
    
    // 新增图片质量设置
    val enableImageCompression: Boolean = true,  // 默认启用压缩
    val imageQuality: Int = 1  // 0=高质量, 1=平衡, 2=省内存
)

/**
 * 时间单位枚举
 */
enum class TimeUnit(val multiplier: Long) {
    MINUTE(60 * 1000),
    HOUR(60 * 60 * 1000),
    DAY(24 * 60 * 60 * 1000)
}

/**
 * 切换模式枚举
 */
enum class SwitchMode {
    SERVICE,
    WIDGET
}

/**
 * 切换顺序枚举
 */
enum class SwitchOrder {
    RANDOM,
    SEQUENTIAL
} 