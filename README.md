# Shizuku

## Background

When developing apps that requires root, the most common method is to run some commands in the su shell. For example, there is an app that uses the `pm enable/disable` command to enable/disable components.

This method has very big disadvantages:

1. **Extremely slow** (Multiple process creation)
2. Needs to process texts (**Super unreliable**)
3. The possibility is limited to available commands
4. Even if ADB has sufficient permissions, the app requires root privileges to run

Shizuku uses a completely different way. See detailed description below.

## User guide & Download

<https://shizuku.rikka.app/>

## How does Shizuku work?

First, we need to talk about how app use system APIs. For example, if the app wants to get installed apps, we all know we should use `PackageManager#getInstalledPackages()`. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us.

Android uses `binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side, so that the system server can check if the app has the permission to do the operation.

Usually, if there is a "manager" (e.g., `PackageManager`) for apps to use, there should be a "service" (e.g., `PackageManagerService`) in the system server process. We can simply think if the app holds the `binder` of the "service", it can communicate with the "service". The app process will receive binders of system services on start.

Shizuku guides users to run a process, Shizuku server, with root or ADB first. When the app starts, the `binder` to Shizuku server will also be sent to the app.

The most important feature Shizuku provides is something like be a middle man to receive requests from the app, sent them to the system server, and send back the results. You can see the `transactRemote` method in `rikka.shizuku.server.ShizukuService` class, and `moe.shizuku.api.ShizukuBinderWrapper` class for the detail.

So, we reached our goal, to use system APIs with higher permission. And to the app, it is almost identical to the use of system APIs directly.

## Developer guide

### API & sample

https://github.com/RikkaApps/Shizuku-API

### Migrating from pre-v11

> Existing applications still works, of course.

https://github.com/RikkaApps/Shizuku-API#migration-guide-for-existing-applications-use-shizuku-pre-v11

### Attention

1. ADB permissions are limited

   ADB has limited permissions and different on various system versions. You can see permissions granted to ADB [here](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/packages/Shell/AndroidManifest.xml).

   Before calling the API, you can use `ShizukuService#getUid` to check if Shizuku is running user ADB, or use `ShizukuService#checkPermission` to check if the server has sufficient permissions.

2. Hidden API limitation from Android 9

   As of Android 9, the usage of the hidden APIs is limited for normal apps. Please use other methods (such as <https://github.com/LSPosed/AndroidHiddenApiBypass>).

3. Android 8.0 & ADB

   At present, the way Shizuku service gets the app process is to combine `IActivityManager#registerProcessObserver` and `IActivityManager#registerUidObserver` (26+) to ensure that the app process will be sent when the app starts. However, on API 26, ADB lacks permissions to use `registerUidObserver`, so if you need to use Shizuku in a process that might not be started by an Activity, it is recommended to trigger the send binder by starting a transparent activity.

4. Direct use of `transactRemote` requires attention

   * The API may be different under different Android versions, please be sure to check it carefully. Also, the `android.app.IActivityManager` has the aidl form in API 26 and later, and `android.app.IActivityManager$Stub` exists only on API 26.

   * `SystemServiceHelper.getTransactionCode` may not get the correct transaction code, such as `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages` does not exist on API 25 and there is `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages_47` (this situation has been dealt with, but it is not excluded that there may be other circumstances). This problem is not encountered with the `ShizukuBinderWrapper` method.

## Developing Shizuku itself

### Build

- Clone with `git clone --recurse-submodules`
- Run gradle task `:manager:assembleDebug` or `:manager:assembleRelease`

The `:manager:assembleDebug` task generates a debuggable server. You can attach a debugger to `shizuku_server` to debug the server. Be aware that, in Android Studio, "Run/Debug configurations" - "Always install with package manager" should be checked, so that the server will use the latest code.

## License

All code files in this project are licensed under Apache 2.0

Under Apache 2.0 section 6, specifically:

* You are **FORBIDDEN** to use `manager/src/main/res/mipmap*/ic_launcher*.png` image files, unless for displaying Shizuku itself.

* You are **FORBIDDEN** to use `Shizuku` as app name or use `moe.shizuku.privileged.api` as application id or declare `moe.shizuku.manager.permission.*` permission.


#ML KIT
ML Kit 文字识别 (Text Recognition) 使用指南

1. 概述

ML Kit 是 Google 推出的移动端机器学习 SDK，其文字识别 (OCR) 功能支持从图片、视频或实时摄像头流中提取文本。该 API 支持多种语言（如中文、拉丁文、日文等），并能在设备端离线运行，无需网络连接 。

2. 集成依赖

在 
"app/build.gradle" 文件中添加以下依赖。根据你的目标语言选择对应的库：

dependencies {
    // 基础拉丁文字识别
    implementation 'com.google.mlkit:text-recognition:16.0.1'
    
    // 中文识别 (可选，如需识别中文必须添加)
    implementation 'com.google.mlkit:text-recognition-chinese:16.0.0'
    
    // 其他语言 (如日文、韩文等)
    // implementation 'com.google.mlkit:text-recognition-japanese:16.0.0'
    // implementation 'com.google.mlkit:text-recognition-korean:16.0.0'
}

3. 核心代码实现

3.1 创建识别器

根据语言需求创建对应的识别器实例：

// 创建中文识别器
val recognizer = TextRecognition.getClient(
    ChineseTextRecognizerOptions.Builder().build()
)

// 或者创建通用识别器 (默认支持拉丁文)
// val recognizer = TextRecognition.getClient()

3.2 准备输入图片

将图片转换为 ML Kit 可识别的 
"InputImage" 对象：

// 从 Bitmap 创建
val image = InputImage.fromBitmap(bitmap, rotationDegrees)

// 从文件路径创建
val image = InputImage.fromFilePath(context, uri)

// 从相机流创建 (配合 CameraX)
// val image = InputImage.fromMediaImage(mediaImage, rotation)

3.3 执行识别并处理结果

调用 
"processImage()" 方法进行异步识别：

recognizer.process(image)
    .addOnSuccessListener { visionText ->
        // 获取全部识别文本
        val fullText = visionText.text
        Log.d("OCR", "识别结果: $fullText")
        
        // 判断是否包含指定文字 (如"元宝")
        if (fullText.contains("元宝")) {
            // 找到了指定文字
            showResult("图片中包含'元宝'")
        } else {
            // 没找到
            showResult("未找到指定文字")
        }
        
        // 如果需要获取文本位置信息，可以遍历结构
        for (block in visionText.textBlocks) {
            val blockText = block.text
            val boundingBox = block.boundingBox // 文本块的位置矩形
            val cornerPoints = block.cornerPoints // 四个角点坐标
            
            for (line in block.lines) {
                // 处理每一行文本
                for (element in line.elements) {
                    // 处理每个元素 (单词/字符)
                }
            }
        }
    }
    .addOnFailureListener { e ->
        // 识别失败处理
        Log.e("OCR", "识别失败", e)
    }

4. 高级功能

4.1 获取文本位置

除了文本内容，ML Kit 还提供详细的文本位置信息，包括：

- Bounding Box：文本块、行或元素的边界框 (Rect)
- Corner Points：文本区域的四个角点坐标
- Confidence Score：识别置信度 (0.0-1.0)

4.2 语言识别

ML Kit 可以自动识别文本的语言，并返回对应的语言代码 (如 "zh" 表示中文) 。

5. 最佳实践

1. 图片质量：确保图片清晰、光线充足，避免模糊或过暗。
2. 权限申请：如果从相机或相册获取图片，需申请 
"CAMERA" 和 
"READ_EXTERNAL_STORAGE" 权限。
3. 资源释放：在 Activity/Fragment 销毁时，调用 
"recognizer.close()" 释放资源。
4. 性能优化：对于实时识别，建议控制处理频率，避免频繁调用导致卡顿 。

6. 常见问题

- 中文识别不准确：确保添加了 
"text-recognition-chinese" 依赖。
- 识别速度慢：检查图片分辨率是否过高，可适当压缩图片。
- 无法识别：检查图片是否包含有效文本，或尝试调整图片角度。

通过以上步骤，你可以快速在 Android 应用中集成 ML Kit 的文字识别功能，实现图片中指定文字的检测 。