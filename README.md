# SSaidHook

[![API](https://img.shields.io/badge/libxposed-API%20102-brightgreen)](https://github.com/libxposed/api)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![QQ Group](https://img.shields.io/badge/QQ%20Group-764468055-blue)](https://qm.qq.com/)

基于 **libxposed API 102** 的 LSPosed 模块：按包名 Hook `Settings.Secure.ANDROID_ID`（SSAID）。

## 功能

- 多应用配置（不同包名可设置不同 SSAID）
- 搜索已安装应用并一键填入包名
- 配置列表实时筛选
- 结束后台进程 / 跳转系统应用详情
- 未启用 LSPosed 时仍可从桌面打开并编辑配置
- QQ 群快捷入口：`764468055`

## 环境要求

- Android 8.0+（minSdk 26）
- 已安装并激活支持 **现代 Xposed API（libxposed）** 的 LSPosed
- Android Studio / AGP 8.x 用于编译

## 构建

```bash
# 用 Android Studio 打开本仓库根目录，同步后：
# Build → Build Bundle(s) / APK(s) → Build APK(s)
```

产物路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 安装 APK  
2. 在 LSPosed 管理器中启用本模块，并勾选目标应用作用域  
3. 打开 SSaidHook，添加包名与 SSAID，点击「保存所有配置」  
4. 结束后台或强制停止目标应用后重新打开，使 Hook 生效  

## 技术说明

| 项目 | 说明 |
|------|------|
| API | `io.github.libxposed:api:102.0.0` |
| Service | `io.github.libxposed:service:102.0.0`（配置同步 / 框架检测） |
| 入口 | `META-INF/xposed/java_init.list` → `com.example.ssaidhook.MainHook` |
| Hook 点 | `Settings.Secure.getString(ContentResolver, String)` |

## QQ 交流群

群号：**764468055**


## CI / GitHub Actions

推送到 `main` / `master` 或提交 PR 时，会自动编译 Debug 与 Release APK。

- 构建产物：Actions 页面 → 对应 workflow run → **Artifacts**
- 打标签发布：`git tag v3.0.0 && git push origin v3.0.0`  
  会自动创建 GitHub Release 并附带 APK

## License

[MIT](LICENSE)


## GitHub Actions

推送到 `main` 或手动在 **Actions** 页运行 **Build APK**：

1. 打开 https://github.com/YJ-Lazy/SSaidHook/actions
2. 若提示启用工作流，点击 **I understand my workflows, go ahead and enable them**
3. 构建完成后，在对应 run 的 **Artifacts** 中下载：
   - `SSaidHook-debug`
   - `SSaidHook-release-unsigned`

打标签发布：

```bash
git tag v4.0
git push origin v4.0
```

会自动创建 GitHub Release 并附带 APK。
