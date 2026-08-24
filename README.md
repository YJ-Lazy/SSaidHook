# QQ SSAID 一键修改

[![libxposed](https://img.shields.io/badge/libxposed-API%20102-brightgreen)](https://github.com/libxposed/api)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Branch](https://img.shields.io/badge/branch-ssaidhookQQ-orange)](https://github.com/YJ-Lazy/SSaidHook/tree/ssaidhookQQ)

基于 [SSaidHook](https://github.com/YJ-Lazy/SSaidHook) 改造的 **QQ 专用** LSPosed 模块。  
一键修改 QQ（`com.tencent.mobileqq`）的 `ANDROID_ID`（SSAID），并在 QQ「关于」页面实时显示当前生效值。

**包名**：`com.example.ssaidhookQQ`  
**当前版本**：1.0

---

## 功能特性

- **一键生成随机 SSAID**：点击按钮即可生成 16 位十六进制随机值并保存
- **实时生效显示**：在 QQ「设置 → 关于 QQ」页面底部显示当前被 Hook 后的 SSAID
- **一键结束后台**：方便快速重启 QQ 使配置生效
- **无需 Root 编辑配置**：即使 LSPosed 未激活也可打开模块进行配置
- **仅针对 QQ**：作用域只需勾选 `com.tencent.mobileqq`，不影响其他应用

---

## 环境要求

| 项目 | 要求 |
|------|------|
| 系统 | Android 8.0+（minSdk 26） |
| 框架 | 支持 **libxposed API 102** 的 LSPosed |
| 编译 | Android Studio / AGP 8.x |
| 目标应用 | QQ（`com.tencent.mobileqq`） |

---

## 快速使用

1. 用 **Android Studio** 打开本项目根目录，同步 Gradle 后执行  
   `Build → Build Bundle(s) / APK(s) → Build APK(s)`
2. 安装生成的 APK
3. 打开 **LSPosed 管理器** → 启用本模块 → **作用域只勾选 QQ**（`com.tencent.mobileqq`）
4. 打开本模块，点击 **「一键生成并应用随机 SSAID」**
5. 点击 **「结束 QQ 后台进程」**（或手动强停 QQ）
6. 重新打开 QQ，进入 **设置 → 关于 QQ**  
   页面底部会出现蓝色提示条，显示当前生效的 SSAID

> 提示：修改后必须重启 QQ 进程才能生效。

---

## 项目结构

```
QQSSaidHook/
├── app/
│   ├── build.gradle                 # 应用级构建配置
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/ssaidhookQQ/
│       │   ├── App.java             # Application + 配置常量
│       │   ├── MainActivity.java    # 一键操作界面
│       │   └── MainHook.java        # 核心 Hook 逻辑
│       ├── res/
│       │   ├── drawable/ic_launcher.png
│       │   ├── mipmap-*/ic_launcher.png
│       │   └── values/strings.xml
│       └── resources/META-INF/xposed/
│           ├── java_init.list       # 模块入口
│           └── module.prop          # 模块元信息
├── build.gradle
├── settings.gradle
├── gradle.properties
├── LICENSE
└── README.md
```

---

## 技术说明

| 项目 | 说明 |
|------|------|
| 包名 | `com.example.ssaidhookQQ` |
| Xposed API | `io.github.libxposed:api:102.0.0`（compileOnly） |
| 入口类 | `com.example.ssaidhookQQ.MainHook` |
| Hook 点 | `Settings.Secure.getString(ContentResolver, String)` |
| UI 注入 | Hook `AboutActivity.doOnCreate` / `onCreate`，在底部添加 TextView |
| 配置存储 | SharedPreferences（`qqssaid_config`），通过 RemotePreferences 跨进程读取 |
| 图标来源 | 原项目 [YJ-Lazy/SSaidHook](https://github.com/YJ-Lazy/SSaidHook) |

---

## 构建命令

```bash
# 使用 Android Studio 打开后同步，或命令行：
./gradlew assembleDebug

# 产物路径
app/build/outputs/apk/debug/app-debug.apk
```

---

## 发布为 SSaidHook 分支的建议步骤

```bash
# 1. 克隆原仓库
git clone https://github.com/YJ-Lazy/SSaidHook.git
cd SSaidHook

# 2. 创建并切换到新分支
git checkout -b ssaidhookQQ

# 3. 清空原内容（保留 .git）
find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +

# 4. 将本项目所有文件复制到当前目录后
git add .
git commit -m "feat: QQ 专用一键修改 SSAID 分支 (com.example.ssaidhookQQ)

- 包名改为 com.example.ssaidhookQQ
- 仅针对 com.tencent.mobileqq
- 一键生成随机 16 位 SSAID
- 在「关于QQ」页面显示当前生效 SSAID
- 使用原项目图标
- 基于 libxposed API 102"

# 5. 推送分支
git push -u origin ssaidhookQQ
```

推送后访问：  
https://github.com/YJ-Lazy/SSaidHook/tree/ssaidhookQQ

---

## 注意事项

- 仅对 `com.tencent.mobileqq` 生效，请勿勾选其他应用
- 修改后必须强制停止并重新打开 QQ 才能生效
- 「关于 QQ」页面类名可能随版本变化，若显示失败请查看 LSPosed 日志（过滤 `QQSSaidHook`）
- 本模块仅供学习与个人研究使用，请遵守相关法律法规及 QQ 用户协议

---

## License

[MIT](LICENSE)  
基于 [YJ-Lazy/SSaidHook](https://github.com/YJ-Lazy/SSaidHook) 修改。
