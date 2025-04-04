package com.chiya.wallswitcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chiya.wallswitcher.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Context 扩展属性，用于获取 DataStore
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置仓库
 */
class SettingsRepository(private val context: Context) {
    
    /**
     * 设置相关的键
     */
    private object PreferencesKeys {
        val FOLDER_PATH = stringPreferencesKey("folder_path")
        val SWITCH_MODE = intPreferencesKey("switch_mode")
        val SWITCH_ORDER = intPreferencesKey("switch_order")
        val AVOID_REPEAT = booleanPreferencesKey("avoid_repeat")
        val INTERVAL_VALUE = intPreferencesKey("interval_value")
        val INTERVAL_UNIT = intPreferencesKey("interval_unit")
        val SET_LOCK_SCREEN = booleanPreferencesKey("set_lock_screen")
        val SHOW_TOAST = booleanPreferencesKey("show_toast")
        val CROP_METHOD = intPreferencesKey("crop_method")
        val ENABLE_LOGGING = booleanPreferencesKey("enable_logging")
        val PAGE_SIZE = intPreferencesKey("page_size")
        val PRELOAD_THRESHOLD = intPreferencesKey("preload_threshold")
        val ENABLE_IMAGE_COMPRESSION = booleanPreferencesKey("enable_image_compression")
        val IMAGE_QUALITY = intPreferencesKey("image_quality")
    }
    
    /**
     * 获取设置流
     */
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            folderPath = preferences[PreferencesKeys.FOLDER_PATH] ?: "",
            switchMode = preferences[PreferencesKeys.SWITCH_MODE] ?: 0,
            switchOrder = preferences[PreferencesKeys.SWITCH_ORDER] ?: 0,
            avoidRepeat = preferences[PreferencesKeys.AVOID_REPEAT] ?: true,
            intervalValue = preferences[PreferencesKeys.INTERVAL_VALUE] ?: 30,
            intervalUnit = preferences[PreferencesKeys.INTERVAL_UNIT] ?: 0,
            setLockScreen = preferences[PreferencesKeys.SET_LOCK_SCREEN] ?: false,
            showToast = preferences[PreferencesKeys.SHOW_TOAST] ?: true,
            cropMethod = preferences[PreferencesKeys.CROP_METHOD] ?: 0,
            enableLogging = preferences[PreferencesKeys.ENABLE_LOGGING] ?: false,
            pageSize = preferences[PreferencesKeys.PAGE_SIZE] ?: 100,
            preloadThreshold = preferences[PreferencesKeys.PRELOAD_THRESHOLD] ?: 20,
            enableImageCompression = preferences[PreferencesKeys.ENABLE_IMAGE_COMPRESSION] ?: true,
            imageQuality = preferences[PreferencesKeys.IMAGE_QUALITY] ?: 1
        )
    }
    
    /**
     * 更新文件夹路径
     */
    suspend fun updateFolderPath(folderPath: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_PATH] = folderPath
        }
    }
    
    /**
     * 更新切换方式
     */
    suspend fun updateSwitchMode(switchMode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SWITCH_MODE] = switchMode
        }
    }
    
    /**
     * 更新切换顺序
     */
    suspend fun updateSwitchOrder(switchOrder: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SWITCH_ORDER] = switchOrder
        }
    }
    
    /**
     * 更新是否避免重复
     */
    suspend fun updateAvoidRepeat(avoidRepeat: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVOID_REPEAT] = avoidRepeat
        }
    }
    
    /**
     * 更新间隔值
     */
    suspend fun updateIntervalValue(intervalValue: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL_VALUE] = intervalValue
        }
    }
    
    /**
     * 更新间隔单位
     */
    suspend fun updateIntervalUnit(intervalUnit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL_UNIT] = intervalUnit
        }
    }
    
    /**
     * 更新是否设置锁屏壁纸
     */
    suspend fun updateSetLockScreen(setLockScreen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SET_LOCK_SCREEN] = setLockScreen
        }
    }
    
    /**
     * 更新是否显示通知
     */
    suspend fun updateShowToast(showToast: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TOAST] = showToast
        }
    }
    
    /**
     * 更新裁剪方式
     */
    suspend fun updateCropMethod(cropMethod: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CROP_METHOD] = cropMethod
        }
    }
    
    /**
     * 更新是否启用日志
     */
    suspend fun updateEnableLogging(enableLogging: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_LOGGING] = enableLogging
        }
    }
    
    /**
     * 更新所有设置
     */
    suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDER_PATH] = settings.folderPath
            preferences[PreferencesKeys.SWITCH_MODE] = settings.switchMode
            preferences[PreferencesKeys.SWITCH_ORDER] = settings.switchOrder
            preferences[PreferencesKeys.AVOID_REPEAT] = settings.avoidRepeat
            preferences[PreferencesKeys.INTERVAL_VALUE] = settings.intervalValue
            preferences[PreferencesKeys.INTERVAL_UNIT] = settings.intervalUnit
            preferences[PreferencesKeys.SET_LOCK_SCREEN] = settings.setLockScreen
            preferences[PreferencesKeys.SHOW_TOAST] = settings.showToast
            preferences[PreferencesKeys.CROP_METHOD] = settings.cropMethod
            preferences[PreferencesKeys.ENABLE_LOGGING] = settings.enableLogging
            preferences[PreferencesKeys.PAGE_SIZE] = settings.pageSize
            preferences[PreferencesKeys.PRELOAD_THRESHOLD] = settings.preloadThreshold
            preferences[PreferencesKeys.ENABLE_IMAGE_COMPRESSION] = settings.enableImageCompression
            preferences[PreferencesKeys.IMAGE_QUALITY] = settings.imageQuality
        }
    }

    /**
     * 更新图片压缩
     */
    suspend fun updateImageCompression(enableCompression: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_IMAGE_COMPRESSION] = enableCompression
        }
    }

    /**
     * 更新图片质量
     */
    suspend fun updateImageQuality(quality: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_QUALITY] = quality
        }
    }
} 