# Changelog

## [1.0] - 2026-08-24

### 新增
- 基于 SSaidHook 改造为 QQ 专用一键模块
- 包名：`com.example.ssaidhookQQ`
- 一键生成随机 16 位十六进制 SSAID
- 在 QQ「关于」页面底部显示当前生效 SSAID
- 一键结束 QQ 后台进程
- 使用原 SSaidHook 项目图标
- 支持 libxposed API 102

### 说明
- 仅针对 `com.tencent.mobileqq`
- 配置通过 SharedPreferences + RemotePreferences 同步
