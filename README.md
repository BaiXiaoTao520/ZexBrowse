# ZexBrowse

ZexBrowse 是一款基于 **GeckoView（Firefox 内核）**、Kotlin 和 Jetpack Compose Material 3 的开源 Android 浏览器。

- 许可证：[Mozilla Public License 2.0](LICENSE)

## 功能

- Compose MD3 首页、动态取色、深浅色主题与半透明材质界面
- GeckoView 网页渲染、地址搜索、前进/后退/刷新和多标签页
- 快捷访问、无痕标签、Cookie/缓存控制和 Room 浏览历史/书签数据层
- 网页下载捕获、APK SHA-256 校验和 GitHub Release 更新检查
- 不内置广告、追踪统计、代理/VPN 或 WebExtension

## 构建与发布

项目提供 [GitHub Actions 工作流](.github/workflows/android-release.yml)。

Release 会附带 `SHA256SUMS.txt`。本地 ARM64 容器不构建 Release APK；请使用 GitHub Actions 完成远程 Release 构建。
