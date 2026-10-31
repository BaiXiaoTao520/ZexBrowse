# ZexBrowse

> [!CAUTION]
> **本项目所有软件均开源免费，没有任何付费解锁项**
>
> ZexBrowse 及本项目发布的其它开源软件，一律免费提供，不存在内购、会员、卡密或任何形式的「付费解锁」功能。
>
> 源码公开意味着任何人都可以编译、篡改后重新打包分发。若你使用的版本提示付费解锁（例如要求输入卡密、购买激活码、付费才能开启功能），**一律属于未经授权的盗版篡改版**，并非本项目发布。请立即停止使用并卸载，改为从本仓库的 Releases 获取。

ZexBrowse 是一款基于 **GeckoView（Firefox 内核）**、Kotlin 和 Jetpack Compose Material 3 的开源 Android 浏览器。

- 支持 Android 8.0（API 28）及以上，已适配 Android 17（API 37）
- 许可证：[Mozilla Public License 2.0](LICENSE)

## 功能

**浏览**

- GeckoView 网页渲染、地址与搜索合一输入栏、前进/后退/刷新与多标签页
- 无痕标签：退出即关闭并清除 Cookie 与缓存
- 快捷访问书签、本地保存的浏览历史与书签数据
- 自定义浏览器标识（UA），内置桌面模式一键切换

**界面**

- Material 3 动态取色，跟随系统 / 浅色 / 深色三种模式
- 8 套内置颜色主题 + 自定义取色器（色相/饱和度面板 + 明度滑条 + HEX 输入）
- 简体中文、繁体中文、English 三套界面语言，可在应用内切换

**安全与隐私**

- 网址安全扫描：钓鱼、仿冒、punycode、可疑后缀、IP 直连、证书异常与混合内容，全部本地判定
- HTTPS-Only：优先加密连接，可按全部标签页或仅隐私标签页生效
- 广告拦截：请求层拦截广告与追踪域名，并隐藏页面内常见广告位，可开关并显示累计拦截次数
- 剪贴板、外部 APP 跳转、麦克风三类网页权限可分别设为询问 / 允许 / 拒绝

**应用推荐**

- 精选工具与创作类应用，APK 直链走应用内下载，网页/网盘类给出打开说明

**下载**

- 网页下载捕获，支持断点续传、暂停与继续（暂停后继续从断点接续，不会重头下载）
- 下载进度通知，在 Android 16+ 上自动升级为实时活动（Live Update），锁屏与状态栏即可查看
- APK SHA-256 校验，下载完成后自动计算并可对比官方哈希
- 下载目录可选内部或任意可授权的外部目录

## 构建

项目使用 Gradle 构建，发布产物由仓库内配置的 GitHub Actions 工作流自动完成并附带校验文件。

```bash
# 调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease
```

发布所需的签名与可选服务配置由维护者在构建环境中自行提供，不随仓库分发。

## 引用的开源项目

本项目基于以下开源项目构建，感谢它们的作者与贡献者：

| 项目 | 用途 | 许可证 |
| --- | --- | --- |
| [GeckoView](https://gitlab.com/oseifert/geckoview) / [Mozilla Firefox](https://github.com/mozilla/gecko-dev) | 网页渲染内核 | MPL 2.0 |
| [Jetpack Compose](https://github.com/androidx/androidx) | 声明式 UI 框架 | Apache 2.0 |
| [Material Design 3](https://github.com/material-components/material-components-android) | 界面设计规范与组件 | Apache 2.0 |
| [OkHttp](https://github.com/square/okhttp) | 网络请求 | Apache 2.0 |
| [Coil](https://github.com/coil-kt/coil) | 图片加载 | Apache 2.0 |
| [Room](https://github.com/androidx/androidx) | 本地数据库 | Apache 2.0 |
| [DataStore](https://github.com/androidx/androidx) | 偏好存储 | Apache 2.0 |
| [WorkManager](https://github.com/androidx/androidx) | 后台下载任务 | Apache 2.0 |
| [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 异步与并发 | Apache 2.0 |

## 附加条款

在 [Mozilla Public License 2.0](LICENSE) 的基础上，分发本程序的修改版本时，**不得**：

1. **移除或篡改版权声明、许可证文本与作者署名**，包括本 README、应用内「关于」页与本仓库历史记录中的相关信息。
2. **隐瞒修改事实**：不得将修改后的版本表述为未经修改的原版，也不得使用与原版相同或近似的名称、图标、包名导致用户混淆。
3. **添加任何形式的付费解锁机制**，包括但不限于内购、会员、卡密、激活码。本程序及其衍生版本必须保持完全免费。
4. **移除或绕过安全与隐私相关功能**，包括网址安全扫描、HTTPS-Only、广告拦截与无痕浏览的数据清除逻辑。
5. **植入广告、追踪代码或未经用户同意的数据收集**，包括但不限于遥测、统计 SDK 与剪贴板/通讯录等权限的滥用。
6. **以本项目名义发布**，暗示其为你本人或本项目的官方发布。

修改版本必须保留上述限制，并且任何再分发者有义务将这些条款一并传递给下游接收者。

## 赞助与支持

如果我的分享对你有一点点帮助，一杯奶茶钱就是对我最大的鼓励 ☕

<img width="1213" height="1213" alt="Image" src="https://github.com/user-attachments/assets/51c3e27e-d97f-4229-8f6e-1a33c3256776" />
