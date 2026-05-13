# WallSwitcher 适配 Android 16 记录

> 2026-05-13 · v1.2.1 → v1.3.0 · targetSdk 35 → 36

---

## 为什么要做这次适配

Google 要求 2026 年上架 Play Store 的应用 targetSdk 必须到 36。WallSwitcher 停在 35 很久了，趁着这次把依赖也一起升了，顺便修了几个一直想修但没动的小问题。

---

## 具体改了什么

### 构建配置和依赖升级

把 compileSdk 和 targetSdk 都拉到 36，versionCode 改成 2，版本号 1.3.0。

依赖方面升了这些：

| 依赖 | 之前 | 现在 |
|------|------|------|
| coreKtx | 1.15.0 | 1.16.0 |
| lifecycleRuntimeKtx | 2.8.7 | 2.9.0 |
| composeBom | 2024.09.00 | 2025.05.00 |
| navigationCompose | 2.7.7 | 2.9.0 |
| workVersion | 2.9.0 | 2.10.0 |
| accompanistPermissions | 0.32.0 | 0.37.0 |
| coilVersion | 2.5.0 | 2.7.0 |

AGP 和 Kotlin 版本没动，当前版本够用。

### Edge-to-Edge

Android 16 不让退出了，`windowOptOutEdgeToEdgeEnforcement` 直接被无视。所以在 `MainActivity.onCreate()` 里加了 `enableEdgeToEdge()`，放在 `setContent` 之前调用。

看了一下现有的界面，HomeScreen / GalleryScreen / SettingsScreen 都有 Scaffold 包着，padding 传递是正确的，不需要额外处理 insets。但建议真机上多转转确认一下，特别是手势导航和刘海屏的情况。

### 前台服务类型声明

Android 14 开始就要求前台服务声明类型了，Android 16 更严格。WallSwitcher 的壁纸切换服务不属于任何标准类型（不是媒体播放、不是位置、不是蓝牙），所以用了 `specialUse`，同时在 Manifest 里加了个 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 说明用途。

改了这些：
- 加了 `FOREGROUND_SERVICE_SPECIAL_USE` 权限
- Service 声明加了 `android:foregroundServiceType="specialUse"`
- 加了 `<property>` 标签说明是壁纸切换服务

### 预测性返回手势

升了 Navigation Compose 到 2.9.0 就行了，这个版本内置支持。代码不用改，因为项目没有覆盖 `onBackPressed()` 也没有自定义返回逻辑。

### WorkManager 初始化冲突

`WallSwitcherApp` 同时实现了 `Configuration.Provider` 接口又手动调了 `WorkManager.initialize()`，会冲突。把手动初始化那行删了，保留 Provider 接口和 Manifest 里的 `tools:node="remove"` 就行，WorkManager 自己会找到 Provider。

### CropMethod 重复定义

`CropMethod.kt` 和 `Settings.kt` 里各定义了一个 CropMethod 枚举，前者有 NONE，后者没有。删了 Settings 里的那份，统一用 CropMethod.kt 的。

### GalleryScreen 日志刷屏

`WallpaperItem` 里的 `LogUtils.log()` 在每次 Composable 重组时都会执行，图库划几下日志文件就爆了。直接删掉了，图片加载的调试信息交给 Coil 自己处理。

### Bitmap 内存泄漏

`WallpaperUtils.setWallpaperInternal()` 里裁剪之后原始位图没 recycle，裁剪后的位图倒是会走到后面的 `bitmap.recycle()`，但 originalBitmap 就那么占着内存。在两个分支（content URI 和文件路径）的 `cropBitmap()` 调用之后都加了 `originalBitmap.recycle()`。

### 硬编码中文

SettingsScreen 的图片质量设置和 GalleryScreen 的壁纸计数都是直接写的中文字符串。换成了 `stringResource()`，对应的字符串资源加到 `strings.xml` 里了。以后要是想做国际化也方便。

### 服务状态检测

`MainViewModel.checkServiceRunning()` 之前直接硬编码返回 false，现在改成了用 `ActivityManager.getRunningServices()` 查询。不过要注意，高版本 Android 上这个 API 可能返回不完整的结果，后面可以考虑换成静态标志位方案。

---

## 没动的部分

以下是 Android 16 的一些变更，但和这个项目没关系：

- **16KB 页面大小**：项目没有 Native 代码，纯 Kotlin，不受影响
- **JobScheduler 配额**：用的是 WorkManager，已升到 2.10.0
- **大屏自适应**：没有锁死 screenOrientation，也没有 resizeableActivity 限制
- **健康权限细化**：不用 BODY_SENSORS
- **Safer Intents**：目前还是 opt-in
- **本地网络权限**：不访问本地网络

---

## 文件改动一览

| 文件 | 改了什么 |
|------|---------|
| `gradle/libs.versions.toml` | 7 个依赖版本升级 |
| `app/build.gradle.kts` | compileSdk/targetSdk/versionCode/versionName |
| `app/src/main/AndroidManifest.xml` | 前台服务权限和类型声明 |
| `MainActivity.kt` | 加了 enableEdgeToEdge() |
| `WallSwitcherApp.kt` | 删了手动 WorkManager.initialize() |
| `Settings.kt` | 删了重复的 CropMethod 枚举 |
| `GalleryScreen.kt` | 删了日志调用，硬编码中文改 stringResource |
| `SettingsScreen.kt` | 硬编码中文改 stringResource |
| `MainViewModel.kt` | checkServiceRunning() 实际实现了 |
| `WallpaperUtils.kt` | Bitmap 回收 |
| `strings.xml` | 加了图片质量和图库字符串资源 |

---

## 测试的时候注意什么

基本功能肯定要过一遍：选文件夹、手动切换、启动服务、停服务、小部件切换。

Android 16 相关的重点看这几个：
1. Edge-to-Edge 下内容有没有被状态栏或导航条挡住
2. 前台服务能不能正常启动和通知
3. 滑动返回有没有预览动画
4. 权限申请是否正常弹出

兼容性建议测 Android 7（最低支持）到 Android 16。

已知一个坑：`ActivityManager.getRunningServices()` 在新版系统上可能拿不到完整列表。如果发现服务状态显示不准，后续可以换成 Service 里的静态标志位。

---

## 以后可以做的

- **Glance 小组件**：依赖加了但一直没迁移，传统 RemoteViews 写起来太痛苦了
- **Hilt 依赖注入**：现在都是手动 new 的，项目大了不好维护
- **服务状态改成静态标志**：比 ActivityManager 靠谱
- **Compose Preview**：能大幅加速 UI 开发
- **R8 混淆**：Release 版本没开，包体积还能再压一压
