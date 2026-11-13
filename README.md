# ClassLive 0.4.0 · 离线实验版

这一分支用本地 **Parakeet Unified English 0.6B Q4_K_M** 替代 AssemblyAI。原来的 0.3.0 云端版在 main，可以和实验版同时安装。两个应用各自保存设置、密钥与课堂记录。

- 实验版包名：`com.yeezi.classlive.offline`，适用于支持 ARMv8.2-A dot product 的 ARM64 手机（目标 vivo X300 Pro）。
- 英文转写完全本地完成；不需要 AssemblyAI 密钥，不产生语音 API 费用。
- 若填写 DeepSeek 密钥，英文内容会联网翻译，仍按 DeepSeek 文本用量计费。不填写即可测纯英文离线转写。
- 实验版在自然停顿处尝试分段，最长约 8 秒强制分段并保留 1.2 秒重叠；英文按段显示，不是逐字更新。切换应用或锁屏会结束收音。
- 只自动保存文字。TXT 由用户点击“生成 TXT”导出。

## 手机测试

1. 安装 `ClassLive-Offline-0.4.0-arm64.apk`，打开 **ClassLive 离线实验**。
2. 点“开始听课”或“设置 → 准备本地模型”，在 Wi-Fi 下载 477 MB 的模型。也可以从 [模型文件](https://huggingface.co/handy-computer/parakeet-unified-en-0.6b-gguf/resolve/843b1b175ace04f5a89d379c79e2518c0d065b25/parakeet-unified-en-0.6b-Q4_K_M.gguf) 下载后，点“导入 GGUF”；两条路径验证相同的 SHA-256。
3. 模型就绪后可以断网测试。持续说几句英语，再按暂停，检查最后一句是否出来。要看中文时，在实验版的设置中单独填写 DeepSeek 密钥。
4. 上课建议先连续试 10–15 分钟：看漏词、术语准确性、字幕延迟、连续讲话的接缝，以及发热。“本地待处理”持续增长说明手机得加长延迟或换更轻的模型；本版队列满会暂停并提示，不悄悄覆盖前面文字。

## 构建

Linux，Java 17+（含编译器工具）、Python 3、CMake 3.18+、Android NDK r28c。运行：

```sh
CLASSLIVE_ANDROID_NDK=/path/to/android-ndk-r28c bash native/build-android.sh
python3 build_apk.py
python3 test_core.py
```

模型权重不打进 APK，也不提交 GitHub。识别引擎使用 Hex 3.4.0 实际依赖的 [transcribe.cpp 分支](https://github.com/anomalyco/transcribe.cpp/tree/bcb639faa4c74a2300ba19e30360cb2355b1663a)，手机版使用 CPU 分段推理。APK 内含依赖许可。模型按 [NVIDIA Open Model License](https://huggingface.co/handy-computer/parakeet-unified-en-0.6b-gguf/blob/843b1b175ace04f5a89d379c79e2518c0d065b25/LICENSE) 单独提供。

脚本产生自签名调试 APK。由其他机器或 CI 用不同调试密钥生成的 APK，不能直接覆盖本 APK；请先导出需保留的文字。旧版云端 ClassLive 的包名不同，不受影响。
