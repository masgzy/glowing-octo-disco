# 项目概述

**项目名称：** 连点器

**项目类型：** Android 应用

**项目状态：** 初始阶段，基本框架已完成

## 技术栈

- **构建工具：** Gradle 7.0.2
- **编译 SDK：** 36 (Android 16)
- **最小 SDK：** 19 (Android 4.4)
- **目标 SDK：** 36 (Android 16)
- **开发语言：** Java
- **依赖管理：** Gradle
- **是否使用 AndroidX：** 否

## 项目结构

```
连点器/
├── app/
│   ├── build.gradle              # 应用模块构建配置
│   ├── proguard-rules.pro        # ProGuard 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单文件
│       ├── java/com/kryp/test/
│       │   └── MainActivity.java # 主 Activity
│       └── res/                  # 资源文件
│           ├── drawable/         # 图片资源
│           ├── layout/           # 布局文件
│           ├── values/           # 值资源（字符串、颜色、样式）
│           └── values-v21/       # API 21+ 特定资源
├── build.gradle                  # 项目根构建配置
├── settings.gradle               # Gradle 设置
├── gradle.properties             # Gradle 属性配置
├── gradlew                       # Gradle Wrapper 脚本（Unix）
└── gradlew.bat                   # Gradle Wrapper 脚本（Windows）

运行时目录：
├── /sdcard/tmp/                  # 截图临时文件目录
│   └── screenshot.png            # 截图文件（保存图片模式）
```

## 包名和应用信息

- **应用包名：** `com.kryp.test`
- **应用名称：** 连点器
- **主 Activity：** `com.kryp.test.MainActivity`
- **应用图标：** `@drawable/ic_launcher`
- **应用主题：** `@style/AppTheme`

## 构建和运行

### 前置要求

- JDK 8 或更高版本
- Android SDK（API 36）
- Gradle（项目使用 Gradle Wrapper，无需单独安装）

### 构建命令

在项目根目录执行：

```bash
# 构建 Debug 版本 APK
./gradlew assembleDebug

# 构建 Release 版本 APK
./gradlew assembleRelease

# 清理构建产物
./gradlew clean

# 重新构建
./gradlew clean assembleDebug

# 安装到连接的设备（需要先连接设备或启动模拟器）
./gradlew installDebug
```

### 输出位置

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release.apk`

## 仓库配置

项目使用阿里云镜像加速依赖下载，包括：
- 公共仓库
- Google 仓库
- Gradle 插件仓库
- JitPack.io

## 开发约定

### 代码风格

- 使用 Java 语言开发
- Activity 继承自 `android.app.Activity`（非 AppCompatActivity）
- 遵循 Android 官方代码规范

### 构建配置

- 混淆：Release 版本默认不启用混淆（`minifyEnabled false`）
- 分辨率适配：应用可调整大小（`android:resizeableActivity="true"`）
- 屏幕比例：最大宽高比 4.0（`android.max_aspect`）

### 注意事项

- 项目未启用 AndroidX 和 Jetifier
- 使用传统的 Android 支持库
- 当前项目为初始状态，仅包含基本框架

## 当前状态

项目处于初始开发阶段：
- ✅ 基本项目结构已完成
- ✅ Gradle 构建配置已完成
- ✅ 基础 Activity 和布局已创建
- ⏳ 核心功能待开发
- ⏳ UI 待完善
- ⏳ 依赖待添加

## 功能实现

### 核心功能概述

应用通过识别屏幕文字实现自动点击功能，主要技术栈包括：
- **Shizuku**：用于执行系统级操作（点击、截图）
- **Google ML Kit**：用于文字识别（OCR）
- **悬浮窗 + 无障碍服务**：用于用户交互和坐标配置

### 功能逻辑流程

```
1. 初始化阶段
   ↓
   用户通过悬浮窗设置点击目标位置
   ↓
   开启自动识别循环

2. 运行阶段（持续循环）
   ↓
   使用 Shizuku 获取屏幕截图
   ↓
   截取左上角区域进行 OCR 识别
   ↓
   根据识别结果执行操作：
   ├─ 识别到"自动" → 点击预设位置
   ├─ 识别到"进行中" → 停止点击
   └─ 无上述文字 → 暂停，等待"自动"出现
```

### 技术实现细节

#### 1. Shizuku 集成

**作用**：提供系统级权限，执行点击和截图操作

**集成步骤**：
- 添加 Shizuku API 依赖（`dev.rikka.shizuku:api`）
- 应用启动时检查 Shizuku 是否已授权
- 通过 `ShizukuBinderWrapper` 执行系统命令

**点击实现**：
```java
// 使用 input 命令模拟点击
String cmd = String.format("input tap %d %d", x, y);
ShizukuBinderWrapper.exec(cmd);
```

**截图实现**：

支持两种截图方式，用户可在主界面选择：

**方式1：保存图片（默认）**
```java
// 保存到 /sdcard/tmp/ 目录
String cmd = "screencap -p /sdcard/tmp/screenshot.png";
ShizukuBinderWrapper.exec(cmd);
// 从文件读取 Bitmap
Bitmap bitmap = BitmapFactory.decodeFile("/sdcard/tmp/screenshot.png");
```

**方式2：管道传输（更高效）**
```java
// 直接从管道读取数据
String cmd = "screencap -p";
Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
InputStream inputStream = process.getInputStream();
Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
inputStream.close();
process.destroy();
```

**说明**：
- 保存图片方式：兼容性好，适合调试，会产生临时文件
- 管道方式：无临时文件，性能更好，内存占用稍高

**参考文档**：https://github.com/RikkaApps/Shizuku-API

#### 2. Google ML Kit 文字识别

**作用**：识别截图中的文字内容

**集成步骤**：
- 添加 ML Kit 依赖
- 配置中文识别器（TextRecognition.CHINESE）
- 处理识别结果

**实现要点**：
- 识别区域：左上角（例如屏幕前 30% 高度区域）
- 识别语言：中文
- 处理流程：
  1. 裁剪截图左上角区域
  2. 将 Bitmap 传递给 ML Kit
  3. 提取识别到的文本
  4. 判断文本内容

**依赖配置**：
```gradle
dependencies {
    implementation 'com.google.mlkit:text-recognition-chinese:16.0.0'
}
```

#### 3. 悬浮窗功能

**作用**：提供用户交互界面，用于：
- 设置点击目标位置
- 显示当前状态（运行/停止/暂停）
- 启动/停止自动识别功能

**实现要点**：
- 使用 `WindowManager` 创建悬浮窗
- 添加 `SYSTEM_ALERT_WINDOW` 权限
- 提供拖拽功能选择点击位置
- 显示当前识别的文字内容

#### 4. Shizuku 授权栏

**作用**：显示 Shizuku 授权状态，提供授权请求入口

**UI 布局**：
- 位于主界面最上方，全宽长条栏
- 未授权状态：显示 "Shizuku 未授权"（可点击请求）
- 已授权状态：显示 ✓ 对号图标 + "已准备就绪"（在左下角）

**实现要点**：
- 应用启动时自动检查 Shizuku 授权状态
- 首次启动自动请求 Shizuku 权限
- 监听 Shizuku 授权状态变化
- 点击未授权栏可重新请求权限
- 使用 SharedPreferences 记录授权状态

**UI 示例**：

未授权状态：
```
+---------------------------------+
|  Shizuku 未授权（点击授权）      |
+---------------------------------+
| 循环检测间隔 (秒): [1.0 ]       |
| 点击间隔 (ms):    [500 ]        |
|         [保存设置]              |
+---------------------------------+
```

已授权状态：
```
+---------------------------------+
|  ✓        已准备就绪             |
+---------------------------------+
| 循环检测间隔 (秒): [1.0 ]       |
| 点击间隔 (ms):    [500 ]        |
|         [保存设置]              |
+---------------------------------+
```

**代码实现**：
```java
// 检查 Shizuku 授权状态
if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
    // 已授权，显示 ✓ 和"已准备就绪"
    updateAuthBar(true);
} else {
    // 未授权，显示未授权状态
    updateAuthBar(false);
    // 自动请求权限
    Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
}

// 点击未授权栏重新请求
authBar.setOnClickListener(v -> {
    Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
});
```

#### 5. 主界面设置

**作用**：提供参数配置界面，用于：
- 选择截图方式（保存图片 / 管道）
- 设置循环检测间隔（秒，支持小数点）
- 设置点击间隔（毫秒）

**实现要点**：
- 在 MainActivity 中添加设置控件
- 截图方式：RadioGroup 或 Spinner 选择
  - 保存图片：兼容性好，适合调试
  - 管道：性能更好，无临时文件
- 循环检测间隔：控制 OCR 识别和检测的频率（单位：秒，支持小数点）
- 点击间隔：控制每次点击之间的延迟时间（单位：毫秒）
- 参数保存：使用 SharedPreferences 持久化存储
- 默认值：
  - 截图方式：保存图片
  - 循环检测间隔：1.0 秒
  - 点击间隔：500ms

**UI 示例**：
```
+---------------------------------+
|  ✓        已准备就绪             |
+---------------------------------+
| 截图方式: ○保存图片 ●管道       |
| 循环检测间隔 (秒): [1.0 ]       |
| 点击间隔 (ms):    [500 ]        |
|         [保存设置]              |
+---------------------------------+
```

#### 6. 无障碍服务

**作用**：辅助悬浮窗操作，提供更完善的权限支持

**实现要点**：
- 继承 `AccessibilityService`
- 配置无障碍服务在 AndroidManifest.xml
- 用于获取屏幕信息辅助坐标选择
- 配合悬浮窗提供更好的用户体验

**开启无障碍服务**：

通过 Shizuku 执行系统命令开启无障碍服务：

```java
// 获取当前已启用的无障碍服务列表
String currentServices = ShizukuBinderWrapper.exec("settings get secure enabled_accessibility_services");

// 添加本应用的无障碍服务
String packageName = "com.kryp.test";
String serviceName = packageName + "/.MyAccessibilityService";
String newServices = currentServices + ":" + serviceName;

// 设置新的无障碍服务列表
ShizukuBinderWrapper.exec("settings put secure enabled_accessibility_services \"" + newServices + "\"");
```

**完整命令**：
```bash
settings put secure enabled_accessibility_services "$(settings get secure enabled_accessibility_services):com.kryp.test/.MyAccessibilityService"
```

**关闭无障碍服务**：
```bash
# 移除本应用的无障碍服务
settings put secure enabled_accessibility_services "$(settings get secure enabled_accessibility_services | sed 's/:com.kryp.test\/.MyAccessibilityService//')"
```

### 权限配置

需要在 `AndroidManifest.xml` 中添加以下权限：

```xml
<!-- Shizuku 权限 -->
<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />

<!-- 悬浮窗权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 存储权限（保存截图到 /sdcard/tmp/） -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                 android:maxSdkVersion="32" />
```

### 识别逻辑伪代码

```java
void autoClickLoop() {
    // 获取用户设置的参数
    boolean usePipeMode = getScreenshotMode();    // 截图方式（true=管道，false=保存图片）
    float detectionInterval = getDetectionInterval(); // 循环检测间隔（秒）
    int clickInterval = getClickInterval();           // 点击间隔（毫秒）

    while (isRunning) {
        // 1. 获取截图（根据用户选择的方式）
        Bitmap screenshot;
        if (usePipeMode) {
            // 管道方式：直接从命令输出流读取
            screenshot = captureScreenByPipe();
        } else {
            // 保存图片方式：保存到 /sdcard/tmp/screenshot.png
            screenshot = captureScreenToFile();
        }

        // 2. 截取左上角区域
        Bitmap topLeftArea = cropTopLeft(screenshot);

        // 3. OCR 识别
        String text = recognizeText(topLeftArea);

        // 4. 根据识别结果执行操作
        if (text.contains("自动")) {
            // 点击预设位置
            performClick(targetX, targetY);
            // 等待点击间隔
            Thread.sleep(clickInterval);
        } else if (text.contains("进行中")) {
            // 停止点击
            stopClicking();
        } else {
            // 暂停，等待"自动"出现
            pauseUntil("自动");
        }

        // 5. 等待循环检测间隔后继续（秒转毫秒）
        Thread.sleep((long)(detectionInterval * 1000));
    }
}

// 管道方式获取截图
Bitmap captureScreenByPipe() {
    String cmd = "screencap -p";
    Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
    InputStream inputStream = process.getInputStream();
    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
    inputStream.close();
    process.destroy();
    return bitmap;
}

// 保存图片方式获取截图
Bitmap captureScreenToFile() {
    String cmd = "screencap -p /sdcard/tmp/screenshot.png";
    ShizukuBinderWrapper.exec(cmd);
    Bitmap bitmap = BitmapFactory.decodeFile("/sdcard/tmp/screenshot.png");
    return bitmap;
}
```

### 配置参数

- **截图方式**：保存图片 / 管道（用户在主界面选择，默认：保存图片）
- **循环检测间隔**：建议 0.5 - 2.0 秒（用户在主界面设置，支持小数点，默认 1.0 秒）
- **点击间隔**：建议 100ms - 1000ms（用户在主界面设置，默认 500ms）
- **识别区域**：屏幕左上角（例如宽度 100%，高度 30%）
- **点击目标**：用户通过悬浮窗设置的坐标

**参数说明**：
- **截图方式**：获取屏幕截图的方法
  - 保存图片：将截图保存到 `/sdcard/tmp/screenshot.png`，兼容性好，便于调试，但会有临时文件
  - 管道：直接从 `screencap` 命令的输出流读取，无临时文件，性能更好，但内存占用稍高
  - 建议在性能要求较高的场景使用管道方式

- **循环检测间隔**：每次 OCR 识别和屏幕检测之间的时间间隔（单位：秒）
  - 过小可能导致性能问题和电量消耗增加
  - 过大可能导致响应延迟
  - 支持小数点设置，可精确到毫秒级别（如 0.5 秒 = 500ms）
  - 建议根据应用需求调整（通常 0.5-1.5 秒较为合适）

- **点击间隔**：连续点击之间的时间间隔（单位：毫秒）
  - 过小可能导致系统无法识别所有点击
  - 过大可能影响点击效率
  - 建议根据目标应用的响应速度调整（通常 200-800ms 较为合适）

## 功能开发方向

根据项目名称"连点器"，预期功能可能包括：
- 自动点击功能 ✅（通过 OCR 识别触发）
- 连点设置（点击间隔、次数）
- 屏幕坐标选择 ✅（通过悬浮窗）
- 悬浮窗控制 ✅
- 点击历史记录
- 多目标位置配置
- 识别区域自定义

*注：上述功能实现文档已根据用户需求完成，具体代码待开发实现。*