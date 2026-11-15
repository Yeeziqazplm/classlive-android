# ClassLive 1.0

Android ARM64 课堂转写与翻译工具，基于已验证的 0.4.0 Hex / Parakeet 离线测试版。

## 本版变化

- 使用 endfield-ui 重做听课、记录、设置、模型管理、收音测试、关于及操作对话框。
- 近黑背景、深灰面板、薄荷绿操作提示；中文翻译优先阅读。
- 修复翻译异步返回后不跟随、末句被底部听课区域挤出视野的问题。
- 手动滑动自动关闭跟随；重新开启后返回最新字幕。
- 听课 / 记录 / 设置三级主导航；课堂操作集中在更多菜单。
- 四档本地模型支持下载、校验、导入、切换及删除。删除不影响课堂记录。
- 下载失败或取消清理临时文件，已安装模型保持不变。

## 本地模型

| 档位 | 下载大小 | 简短介绍 |
|---|---:|---|
| Q4_K_M | 477 MB | 推荐课堂，最小占用，延续测试版配置。 |
| Q5_K_M | 541 MB | 均衡，量化损失更小，内存略增。 |
| Q6_K | 602 MB | 保留更多权重精度，运行负担更高。 |
| Q8_0 | 731 MB | 低量化损失，适合对比，占用最高。 |

四档均为 Parakeet Unified English 0.6B，只识别英文。文件大小按十进制 MB；运行需额外内存。精度更高不保证每句话更准，也不代表更快。默认 Q4。F16/F32 和不同架构模型未纳入手机端本次支持范围。
模型源固定在 [此版本](https://huggingface.co/handy-computer/parakeet-unified-en-0.6b-gguf/tree/843b1b175ace04f5a89d379c79e2518c0d065b25)，按每个文件的 SHA-256 完整校验。

## 安装和使用

1. 安装 ClassLive1.0.apk。支持 Android 8.0+，ARM64 且支持 ARMv8.2-A dot product（包括目标 vivo X300 Pro）。
2. 设置 → 部署本地模型 → 下载或导入 Q4。可保留多个模型，随时在暂停后切换。
3. 设置 → 中文翻译 → 填写自己的 DeepSeek 密钥；留空只做离线英文转写。
4. 听课 → 开始听课。更多菜单提供新建课堂、改名、生成 TXT、补译及收音详情。
5. 记录自动保存；TXT 可选双语、仅英文或仅中文。

本版包名为 com.yeezi.classlive.v1，显示为「ClassLive 1.0」，可与「ClassLive 离线实验」并存。已核实旧测试版 CI 签名不同，不能直接覆盖；采用独立包名保护旧应用的数据。**无需卸载旧版。** 旧课堂记录仍在旧应用中，新版首次需部署模型及填写翻译密钥。旧版 TXT 可照常导出，本版未加入 TXT 导回课堂功能。

## 识别与隐私

保留测试版的收音增强、8 秒左右分段、1.2 秒重叠及词时间拼接；本次重点修显示与菜单，不声称改善识别准确率或断句。离开应用或锁屏暂停收音。只保存文字，不保存或上传录音。

中文翻译仍调用 DeepSeek，按该账户用量收费。开启后发送目标英文、最近三段上下文和术语表。API 密钥通过 Android Keystore 加密保存。

## 构建和测试

GitHub Actions 使用 Java 17、Android SDK 35、Gradle 8.9、NDK r28c。运行核心测试、模型安装完整性测试、Android Lint，并在 360 / 390 / 430 dp 模拟器上检查字幕跟随、回看、删除模型、导航及截图。360 dp 同时使用 1.3 倍字体。

本机 Linux 构建：

```sh
CLASSLIVE_ANDROID_NDK=/path/to/android-ndk-r28c bash native/build-android.sh
python3 build_apk.py
python3 test_core.py
```

原生引擎固定在 [transcribe.cpp bcb639f](https://github.com/anomalyco/transcribe.cpp/tree/bcb639faa4c74a2300ba19e30360cb2355b1663a)。模型权重不打进 APK。许可随包附带，可在设置 → 关于中查看。
