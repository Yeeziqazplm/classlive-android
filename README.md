# ClassLive 课堂翻译 · Android 0.1.0

手机麦克风 → AssemblyAI 实时英语识别 → DeepSeek 中文翻译。支持 Android 8.0 及以上。

## 初版功能

- 英语实时字幕与逐句中文翻译。
- 在手机上填写自己的 AssemblyAI 和 DeepSeek API 密钥，使用 Android Keystore 加密保存。
- 本次课堂文字自动保存在应用私有目录；点击按钮才生成并保存 TXT。
- 前台收音；切换应用或锁屏会暂停。不保存原始音频文件。

[下载初版 APK](releases/v0.1.0/ClassLive-0.1.0.apk)。这是调试签名的测试版本，需填写自己的 API 密钥才能识别、翻译；服务分别计费。

## 构建

Java 17、Gradle 8.9、Android SDK 35、AGP 8.7.3。Android Studio 导入工程，或 Linux 执行 `python3 build_apk.py`。本地生成的新签名不能覆盖原来交付的 APK。

## 归档说明

本提交是将此前交付的 0.1.0 补录到 Git。恢复源码构建的 `classes.dex`、`resources.arsc` 和 `AndroidManifest.xml` 与原 APK 逐字节一致。文档按已交付功能整理；提交时间是归档时间，未伪造早期提交。
