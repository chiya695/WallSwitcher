# WallSwitcher Android 16 (API 36) 适配日志

> 适配日期：2026-05-13  
> 版本变更：v1.2.1 → v1.3.0  
> targetSdk/compileSdk：35 → 36

---

## 一、适配概述

本次适配将 WallSwitcher 应用升级至 Android 16 (API 36) 目标平台，主要涉及以下变更：

| 序号 | 适配项 | 优先级 | 状态 |
|------|--------|--------|------|
| 1 | 构建配置升级 (compileSdk/targetSdk/依赖) | 🔴 最高 | ✅ 完成 |
| 2 | Edge-to-Edge 强制执行 | 🔴 最高 | ✅ 完成 |
| 3 | 前台服务类型声明 (foregroundServiceType) | 🔴 最高 | ✅ 完成 |
| 4 | 预测性返回手势 | 🟡 高 | ✅ 完成 |
| 5 | 已知 Bug 修复 | 🟡 高 | ✅ 完成 |
| 6 | 代码质量改进 (硬编码中文/日志泛滥等) | 🟠 中 | ✅ 完成 |

---

## 二、变更详情

### 2.1 构建配置升级

#### 变更原因
Android 16 要求 `targetSdk >= 36`，Google Play 将于 2026 年强制要求。

#### 变更内容

**`gradle/libs.versions.toml`**
| 配置项 | 旧版本 | 新版本 | 说明 |
|--------|--------|--------|------|
| coreKtx | 1.15.0 | 1.16.0 | AndroidX Core 更新 |
| lifecycleRuntimeKtx | 2.8.7 | 2.9.0 | Lifecycle 更新 |
| composeBom | 2024.09.00 | 2025.05.00 | Compose 组件库统一升级 |
| navigationCompose | 2.7.7 | 2.9.0 | 支持预测性返回手势 |
| workVersion | 2.9.0 | 2.10.0 | WorkManager 更新 |
| coilVersion | 2.5.0 | 2.7.0 | 图片加载库更新 |
| accompanistPermissions | 0.32.0 | 0.37.0 | 权限库更新 |

**`app/build.gradle.kts`**
| 配置项 | 旧值 | 新值 | 说明 |
|--------|------|------|------|
| compileSdk | 35 | 36 | 编译 SDK 升级 |
| targetSdk | 35 | 36 | 目标 SDK 升级 |
| versionCode | 1 | 2 | 版本号递增 |
| versionName | "1.0" | "1.3.0" | 版本名更新 |

#### 注意事项
- AGP 保持 8.9.1，已满足 Android 16 要求 (≥ 8.5.1)
- Kotlin 保持 2.0.21，与 Compose Compiler 兼容
- `composeOptions.kotlinCompilerExtensionVersion` 保持 "1.5.8"，与 Kotlin 2.0.21 + Compose Plugin 兼容

---

### 2.2 Edge-to-Edge 强制执行

#### 变更原因
Android 16 (targetSdk=36) **完全禁止退出 Edge-to-Edge 模式**。`windowOptOutEdgeToEdgeEnforcement` 标志被忽略。应用必须正确处理系统栏 insets。

#### 变更内容

**`MainActivity.kt`**
```kotlin
// 新增 import
import androidx.activity.enableEdgeToEdge

// 在 onCreate() 中 setContent 之前添加
enableEdgeToEdge()
```

`enableEdgeToEdge()` 会自动完成以下操作：
1. 调用 `setDecorFitsSystemWindows(false)` — 内容延伸到系统栏后面
2. 设置状态栏和导航栏为透明
3. 根据深色/浅色主题调整系统栏图标颜色

#### 已有代码的兼容性分析
- **Scaffold**：MainScreen 使用 `Scaffold` 的 `paddingValues` 传递给 NavHost，Scaffold 默认处理 `systemBars` insets，**无需额外修改**
- **HomeScreen**：使用 `verticalScroll` + `Modifier.padding(16.dp)`，外层已有 Scaffold padding，**无需修改**
- **GalleryScreen**：`LazyVerticalGrid` 在 `Column` 中，外层已有 Scaffold padding，**无需修改**
- **SettingsScreen**：同上，`verticalScroll` + 外层 Scaffold padding，**无需修改**

#### 测试要点
- [ ] 手势导航模式下，底部内容不被导航条遮挡
- [ ] 三键导航模式下，显示正常
- [ ] 刘海屏/打孔屏设备上，顶部内容不被遮挡
- [ ] 横屏模式下显示正常
- [ ] 深色/浅色主题下系统栏图标可读

---

### 2.3 前台服务类型声明

#### 变更原因
Android 14 (API 34) 引入要求：所有前台服务必须声明 `foregroundServiceType`。Android 16 进一步要求 targetSdk=36 的应用必须声明，否则服务启动会崩溃。

#### 变更内容

**`AndroidManifest.xml`**

新增权限声明：
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

修改服务声明：
```xml
<service
    android:name=".service.WallpaperSwitchService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="wallpaper_switching_service" />
</service>
```

#### 选择 `specialUse` 的理由
WallSwitcher 的前台服务用于定时切换壁纸，不属于 Android 定义的任何标准前台服务类型（如 location、mediaPlayback、connectedDevice 等）。因此使用 `specialUse` 并通过 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 属性说明具体用途。

#### 测试要点
- [ ] 启动壁纸切换服务后，通知栏显示前台服务通知
- [ ] 服务在 Android 14+ 设备上正常启动和运行
- [ ] Google Play 审核时能通过前台服务类型审查

---

### 2.4 预测性返回手势

#### 变更原因
Android 16 (targetSdk=36) 默认启用预测性返回手势动画。`onBackPressed()` 不再被调用。

#### 变更内容
升级 `navigation-compose` 到 2.9.0，该版本内置支持预测性返回。

无需额外代码修改，因为：
1. 应用使用 Navigation Compose 管理页面导航，已正确处理返回栈
2. 没有覆盖 `onBackPressed()`
3. 没有使用 `OnBackPressedDispatcher` 进行自定义返回逻辑

#### 测试要点
- [ ] 从边缘滑动返回时，显示预测性返回动画（页面缩小预览）
- [ ] 在设置页面点击返回，正确返回主页
- [ ] 在图库页面点击返回，正确返回主页

---

### 2.5 已知 Bug 修复

#### 2.5.1 CropMethod 重复定义

**问题**：`data/model/CropMethod.kt` 和 `data/model/Settings.kt` 中各定义了一个 CropMethod 枚举，前者包含 NONE/CENTER/TOP/BOTTOM，后者只有 CENTER/TOP/BOTTOM。

**修复**：删除 `Settings.kt` 中的重复枚举定义，统一使用 `CropMethod.kt` 中的定义（含 NONE）。更新 `cropMethod` 注释为 `0=不裁剪(NONE), 1=居中裁剪(CENTER), 2=顶部裁剪(TOP), 3=底部裁剪(BOTTOM)`。

**影响文件**：`Settings.kt`

#### 2.5.2 GalleryScreen 日志泛滥

**问题**：`WallpaperItem` Composable 中 `LogUtils.log("加载图片: ${wallpaper.path}")` 在每次重组时被调用，导致日志文件急剧膨胀和性能下降。

**修复**：移除该日志调用。图片加载的调试信息应由图片加载库 (Coil) 自行处理。

**影响文件**：`GalleryScreen.kt`

#### 2.5.3 WorkManager 双重初始化

**问题**：`WallSwitcherApp` 同时手动调用了 `WorkManager.initialize()` 并实现了 `Configuration.Provider` 接口，导致双重初始化冲突。

**修复**：移除 `onCreate()` 中的手动 `WorkManager.initialize()` 调用，保留 `Configuration.Provider` 接口和 Manifest 中的 `tools:node="remove"` 配置。WorkManager 会自动检测 `Configuration.Provider` 并使用自定义配置。

**影响文件**：`WallSwitcherApp.kt`

#### 2.5.4 WallpaperUtils 中 Bitmap 泄露

**问题**：`setWallpaperInternal()` 方法中，当需要裁剪时（cropMethodOrdinal != 0），`cropBitmap()` 返回的新位图使用后未被 recycle，且原始位图也未 recycle。

**修复**：
- 裁剪后立即 recycle 原始位图 `originalBitmap.recycle()`
- 裁剪后的位图在 `setBitmap()` 后由已有的 `bitmap.recycle()` 回收

**影响文件**：`WallpaperUtils.kt`

#### 2.5.5 SettingsScreen 硬编码中文

**问题**：`ImageQualitySettings` 组件中的文字直接硬编码中文字符串，未使用 `stringResource`。

**修复**：将所有硬编码字符串替换为 `stringResource(id = R.string.xxx)` 调用，并在 `strings.xml` 中添加对应条目。

新增字符串资源：
- `settings_image_quality_title` → 图片质量设置
- `settings_enable_image_compression` → 启用图片压缩
- `settings_image_quality` → 图片质量
- `settings_image_quality_high` → 高质量 (占用更多内存)
- `settings_image_quality_balanced` → 平衡 (推荐)
- `settings_image_quality_low` → 省内存 (质量较低)
- `settings_image_quality_high_warning` → 注意：高质量模式可能导致内存占用增加...
- `settings_image_compression_disabled_warning` → 禁用压缩将使用原始图片质量...

**影响文件**：`SettingsScreen.kt`、`strings.xml`

#### 2.5.6 服务运行状态检测不准确

**问题**：`MainViewModel.checkServiceRunning()` 硬编码为 `_serviceRunning.value = false`，无法反映真实状态。

**修复**：使用 `ActivityManager.getRunningServices()` 查询服务是否正在运行。

**影响文件**：`MainViewModel.kt`

#### 2.5.7 GalleryScreen 硬编码中文

**问题**：`"共 ${wallpapers.size} 张壁纸"` 直接硬编码。

**修复**：替换为 `stringResource(id = R.string.gallery_wallpaper_count, wallpapers.size)`，并添加对应字符串资源。

**影响文件**：`GalleryScreen.kt`、`strings.xml`

---

## 三、未涉及的 Android 16 变更

以下 Android 16 变更与本项目无关，无需处理：

| 变更项 | 原因 |
|--------|------|
| 16KB 页面大小支持 | 项目无 Native 代码 (C/C++)，纯 Kotlin 开发，不受影响 |
| JobScheduler 配额优化 | 项目使用 WorkManager（已升级到 2.10.0），不受直接影响 |
| 大屏自适应布局 | 应用无 screenOrientation 锁定、resizeableActivity 限制 |
| 健康与健身权限细化 | 应用不使用 BODY_SENSORS 权限 |
| Safer Intents | 当前为 opt-in，非强制 |
| 本地网络权限 | 应用不访问本地网络 |
| setImportantWhileForeground 失效 | 应用未使用此 API |

---

## 四、完整文件变更清单

| 文件路径 | 变更类型 | 说明 |
|----------|----------|------|
| `gradle/libs.versions.toml` | 修改 | 依赖版本升级 |
| `app/build.gradle.kts` | 修改 | compileSdk/targetSdk/versionCode/versionName |
| `app/src/main/AndroidManifest.xml` | 修改 | 前台服务权限+类型声明 |
| `app/src/main/java/.../MainActivity.kt` | 修改 | 添加 enableEdgeToEdge() |
| `app/src/main/java/.../WallSwitcherApp.kt` | 修改 | 移除 WorkManager 手动初始化 |
| `app/src/main/java/.../data/model/Settings.kt` | 修改 | 移除重复 CropMethod 枚举 |
| `app/src/main/java/.../ui/screen/GalleryScreen.kt` | 修改 | 移除日志调用+硬编码中文 |
| `app/src/main/java/.../ui/screen/SettingsScreen.kt` | 修改 | 硬编码中文改用 stringResource |
| `app/src/main/java/.../ui/viewmodel/MainViewModel.kt` | 修改 | 服务状态检测实现 |
| `app/src/main/java/.../util/WallpaperUtils.kt` | 修改 | Bitmap 泄露修复 |
| `app/src/main/res/values/strings.xml` | 修改 | 添加图片质量+图库字符串资源 |

---

## 五、测试建议

### 5.1 基本功能测试
1. 选择壁纸文件夹 → 正确加载图片列表
2. 手动切换壁纸 → 壁纸成功设置
3. 启动自动切换服务 → 按设定间隔切换
4. 停止服务 → 服务正确停止
5. 桌面小部件切换 → 壁纸正确切换

### 5.2 Android 16 特性测试
1. **Edge-to-Edge**：状态栏/导航栏透明，内容不被遮挡
2. **前台服务**：服务启动时显示通知，不崩溃
3. **预测性返回**：从边缘滑动返回有动画预览
4. **权限**：首次启动正确请求存储和通知权限

### 5.3 兼容性测试
- Android 7.0 (API 24) 最低支持版本
- Android 14 (API 34) 前台服务类型要求
- Android 15 (API 35) Edge-to-Edge 过渡
- Android 16 (API 36) 目标版本

### 5.4 已知限制
- `ActivityManager.getRunningServices()` 在高版本 Android 上可能受系统限制，返回结果不完整。生产环境建议改用 `Service.isRunning` 静态标志位方案。
- 预测性返回手势的完整体验需要系统支持（Android 14+ 设备）

---

## 六、后续优化建议

1. **迁移到 Glance 小组件**：已引入 Glance 依赖但未使用，建议将传统 RemoteViews 小组件迁移到 Glance
2. **引入 Hilt 依赖注入**：替代手动创建仓库实例
3. **服务状态改用静态标志**：替代 ActivityManager 查询方案
4. **添加 Compose Preview**：利用 Android Studio 的预览功能加速 UI 开发
5. **启用 R8/ProGuard**：当前 release 构建未启用代码混淆和压缩
