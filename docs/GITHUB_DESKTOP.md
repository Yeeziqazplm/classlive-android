# 使用 GitHub Desktop 发布

## 本地添加

1. 将 `ClassLive-GitHubDesktop.zip` 完整解压。文件夹 `classlive-android` 内包含 Git 历史；不要只提取源文件，也不要在压缩包里直接操作。
2. 在已登录的 GitHub Desktop 选择 **File → Add local repository…**。
3. 选择解压后的 `classlive-android` 文件夹，点击 **Add repository**。
4. 点击 **Publish repository**，仓库名用 `classlive-android`，建议保留 **Keep this code private** 勾选，然后发布到自己的账户。

当前项目已包含两个提交、两个注释版本标签，以及两版 APK 与更新日志，无需再次建立初始提交。本地 Git 配置 `push.followTags=true`，推送时携带可达的注释标签。

发布后在 GitHub 仓库检查 Code、Tags 和 `releases/` 目录。若客户端未推送标签，可在 Desktop 的 **Repository → Open in Terminal** 中执行：

```bash
git push origin --tags
```

之后把仓库链接发回，便可继续检查在线版本是否齐全。`releases/` 文件夹中的安装包已随代码上传；网站里的 GitHub Releases 条目是另外的展示形式。

## 后续迭代

改动源码并更新 CHANGELOG，测试通过后提交。新版本增加 versionCode，记录新的 versionName 与版本说明。需要标签时在终端创建新的注释标签再推送。

API 密钥只在手机上配置。原交付 APK 的私有签名备份另行保存，未纳入仓库；不要提交签名私钥。Actions 和重新构建生成的其他签名 APK 无法覆盖现有安装。
