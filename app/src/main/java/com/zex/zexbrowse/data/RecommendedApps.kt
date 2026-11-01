/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.zex.zexbrowse.R

/**
 * 网页/网盘类下载的二次说明弹窗内容。
 *
 * 这类链接无法在应用内直接下载，需要在跳转浏览器前先说明下载步骤或网盘信息
 * （例如蓝奏云的提取码）。为 null 表示无需说明，直接交给系统打开。
 */
data class BrowserNotice(
    /** 弹窗标题（字符串资源，按当前语言显示） */
    @StringRes val title: Int,
    /** 弹窗正文（换行分隔）；为第三方应用的下载说明，保留原文 */
    val content: String,
    /** 确认按钮文案 */
    @StringRes val confirmLabel: Int
)

/**
 * 推荐分类。
 *
 * 条目不多时分类仍值得做：用户找工具和应用商店类需求差异很大，
 * 混在一列里只能凭图标搏记忆。分类页各自独立，点击后才进入，不改变下载行为。
 */
enum class RecommendCategory {
    /** 工具与效率 */
    TOOLS,
    /** 应用商店 */
    STORE,
    /** 游戏与游戏相关工具 */
    GAME,
    /** 创作与设计 */
    CREATE,
    /** 内容分享与社区 */
    SHARE
}

/**
 * 应用推荐条目。
 *
 * 字段与本应用的下载流程保持一致：
 * - [downloadUrl] 为 APK 直链时走应用内下载（遵循「下载设置」中的目录配置），完成后可安装；
 * - 否则为网页/网盘类资源，交给系统处理（由系统默认浏览器或用户选择的应用打开）；
 *   配置了 [browserNotice] 时，打开浏览器前先弹出说明。
 */
data class RecommendedApp(
    val name: String,
    val version: String,
    val summary: String,
    val description: String,
    val downloadUrl: String,
    val icon: ImageVector,
    /** 所属分类，用于推荐页的分组展示 */
    val category: RecommendCategory,
    /** 网页/网盘类下载的二次说明弹窗；为 null 表示直接打开浏览器 */
    val browserNotice: BrowserNotice? = null
) {
    /** 卡片/弹窗标题：有版本号时拼接版本号显示 */
    val displayName: String get() = if (version.isEmpty()) name else "$name $version"

    /** 列表副标题：有版本号时「版本 · 简介」 */
    val subtitle: String get() = if (version.isEmpty()) summary else "$version · $summary"

    /** 是否为可直接下载的 APK 安装包（与浏览器的下载链接判定规则一致） */
    val isApk: Boolean get() = downloadUrl.substringBefore('?').substringBefore('#').endsWith(".apk", ignoreCase = true)

    /** 下载文件名：从链接末段取，取不到时回退为「应用名.apk」 */
    val fileName: String get() = downloadUrl.substringAfterLast('/').substringBefore('?').ifBlank { "$name.apk" }
}

val appShareRecommendation = RecommendedApp(
    name = "AppShare",
    version = "",
    summary = "App 多版本讨论、评分与资源分享平台",
    downloadUrl = "https://app.sharess.cn/download",
    icon = Icons.Rounded.Apps,
    category = RecommendCategory.STORE,
    browserNotice = BrowserNotice(
        title = R.string.notice_appshare_title,
        content = "具体下载步骤：\n\n" +
            "1. 点击页面中的“正式版”。\n" +
            "2. 页面会跳转到蓝奏云。\n" +
            "3. 在蓝奏云页面点击下载即可。\n\n" +
            "建议使用电脑 UA 访问。手机端网页可能会提示开通会员才能下载。",
        confirmLabel = R.string.dl_open_browser
    ),
    description = """
        AppShare 平台介绍
        一个提供 App 多版本讨论及评分的平台

        排行系统
        AppShare 提供了用户资产排行、应用下载排行、分类排行等一些榜单，榜单数据实时更新。

        应用分类
        AppShare 现存 31 个大分类和 104 个小分类，您可以快速又精准地检索需要的资源。

        任务中心
        AppShare 日渐完善的任务中心，可以使普通用户无限制地使用所有功能。

        应用更新
        AppShare 会在用户上传新版本后提醒您更新应用，没有先后顺序，每个人都可以在第一时间更新应用。
        您也可以选择忽略不想更新的应用。

        日渐完善的资产系统
        您可以通过一些 App 任务获取积分或分享值，从而无限制下载 AppShare 内的资源；资源上传者也可以通过其他人的下载获得分享值，用于兑换高级功能或奖励。
    """.trimIndent()
)

val fluffychatRecommendation = RecommendedApp(
    name = "FluffyChat",
    version = "2.11.3",
    summary = "The cutest instant messenger in the [matrix]",
    downloadUrl = "https://github.com/krille-chan/fluffychat/releases/download/v2.11.3/fluffychat.apk",
    icon = Icons.Rounded.Chat,
    category = RecommendCategory.SHARE,
    description = "FluffyChat is an open source, nonprofit and cute [matrix] client written in Flutter. The goal of the app is to create an easy to use instant messenger which is open source and accessible for everyone."
)

val reveriePaintRecommendation = RecommendedApp(
    name = "ReveriePaint",
    version = "v1.4.0",
    summary = "基于 Krita 核心引擎打造的 Android 原生数字绘画应用",
    downloadUrl = "https://github.com/LanRhyme/ReveriePaint/releases/download/v1.4.0/ReveriePaint-v1.4.0.apk",
    icon = Icons.Rounded.Brush,
    category = RecommendCategory.CREATE,
    description = """
        基于 Krita 核心引擎打造的 Android 原生现代数字绘画应用

        融合 Jetpack Compose 现代化界面与 Krita C++ 原生图像处理内核，专为平板与触控设备优化的专业创作工作流。

        绘画引擎与笔刷系统
        • Krita 官方内核集成：复用 Krita 核心笔刷引擎，支持真实物理笔触与颜料混合模拟。
        • 内置丰富笔刷库：内置 240+ 官方笔刷预设，涵盖铅笔、钢笔、墨水、水彩、油画、喷枪、纹理与马克笔。
        • 笔刷工坊：支持实时调整尺寸、不透明度、流量、间距、软硬度、混色比与压感动态曲线。
        • 硬件压感适配：适配 Android 压感手写笔，支持抖动修正、子帧平滑插值与悬浮光标预览。

        专业图层与合成管理
        • 无限图层与分组树：支持无限图层创建、图层组嵌套与层级折叠。
        • 丰富混合模式：支持正常、正片叠底、滤色、叠加、柔光、强光、颜色减淡等 25 种混合模式。
        • 图层操作全功能：支持剪贴蒙版、Alpha 锁定、图层锁定、隐藏/显示、快速合并与色彩标签。
        • 直观交互：图层面板支持长按拖拽排序、左滑操作菜单与批量图层管理。

        创作工具箱
        • 包含画笔、橡皮擦、涂抹、模糊、液化变形、渐变填充与文字排版工具。
        • 支持直线、矩形、椭圆与多边形工具，以及快速吸附与辅助对齐。
        • 支持套索、矩形、椭圆、魔棒与颜色选区，并支持加选、减选、反选和羽化。
        • 支持自由变换、等比缩放、旋转、透视扭曲与画布裁剪。

        录制与延时回放
        • 记录笔迹、笔刷参数、图层变动、滤镜与色彩变迁全过程。
        • 录制数据随 .revp 工程文件自动存储压缩，方便跨设备共享。
        • 支持播放、暂停、进度拖动与 0.5x - 4x 倍速调节。

        自动保存与工程安全
        • 支持 1/3/5/10/15/30 分钟多档静默自动保存。
        • 应用切至后台或意外中断时自动保留最后创作状态，支持快速恢复未保存草稿。

        现代触控与个性化主题
        • 支持双指缩放、旋转、平移画布，提供 120 FPS 视口变换。
        • 支持双指点击撤销、三指点击重做、长按快速吸色。
        • 提供莫兰迪全局主题，支持 Material You 动态取色。
        • 支持自适应、深灰、纯黑、纯白及自定义 Hex 工作区底色。
    """.trimIndent()
)

val zalithLauncherRecommendation = RecommendedApp(
    name = "Zalith Launcher 2",
    version = "",
    summary = "A Minecraft: Java Edition Launcher for Android",
    downloadUrl = "https://docs.zalithlauncher.cn/",
    icon = Icons.Rounded.SportsEsports,
    category = RecommendCategory.GAME,
    description = """
Zalith Launcher 2 是一个全新设计、面向 Android 设备 的 Minecraft: Java Edition 启动器。项目使用 PojavLauncher 作为启动核心，采用 Jetpack Compose 与 Material Design 3 构建现代化 UI 体验。
    """.trimIndent()
)

val tomatoRecommendation = RecommendedApp(
    name = "Tomato",
    version = "v2.0.1",
    summary = "开源简洁的番茄钟软件",
    downloadUrl = "https://github.com/nsh07/Tomato/releases/download/v2.0.1/tomato-v2.0.1-release.apk",
    icon = Icons.Rounded.Timer,
    category = RecommendCategory.TOOLS,
    description = "开源简洁的番茄钟软件"
)

val komiStoreRecommendation = RecommendedApp(
    name = "Komi Store",
    version = "v1.9.2",
    summary = "开源 Github 仓库应用商店",
    downloadUrl = "https://github.com/komi-store/komi-store/releases/download/v1.9.2/Komi-Store-1.9.2.apk",
    icon = Icons.Rounded.Storefront,
    category = RecommendCategory.STORE,
    description = """
        Komi Store only helps you discover and download release assets that are already published on GitHub by third-party developers. The contents, safety, and behavior of those downloads are entirely the responsibility of their respective authors and distributors, not this project.

        By using Komi Store, you understand and agree that you install and run any downloaded software at your own risk. This project does not review, validate, or guarantee that any installer is safe, free of malware, or fit for any particular purpose.
    """.trimIndent()
)

val coolMonitorRecommendation = RecommendedApp(
    name = "CoolMonitor",
    version = "v5.3.2",
    summary = "开源设备健康监控诊断工具",
    downloadUrl = "https://wwbaa.lanzoub.com/b01gicpghc",
    icon = Icons.Rounded.MonitorHeart,
    category = RecommendCategory.TOOLS,
    browserNotice = BrowserNotice(
        title = R.string.notice_coolmonitor_title,
        content = "即将跳转至浏览器\n" +
            "网盘商：蓝奏云\n" +
            "提取码：1c37\n" +
            "建议更换为电脑 UA 访问下载文件",
        confirmLabel = R.string.notice_pan_download
    ),
    description = "CoolMonitor 安卓设备健康监控诊断工具，实时监测CPU/电池温度、后台耗电应用、WiFi网络风险，支持App权限审计、垃圾营销通知过滤、电池健康评估，内置AI智能分析设备异常并给出优化建议，纯本地数据处理不上传隐私，无广告免费使用"
)

val twilightToolboxRecommendation = RecommendedApp(
    name = "暮光工具箱",
    version = "",
    summary = "以 Material Design 3 打造的开源多功能工具箱",
    downloadUrl = "https://qisuhe.pages.dev/",
    icon = Icons.Rounded.Construction,
    category = RecommendCategory.TOOLS,
    description = "一款秉承互联网分享精神以 Material Design 3 风格进行设计开发的多功能工具聚合工具箱"
)

val aEmulatorRecommendation = RecommendedApp(
    name = "AEmulator",
    version = "v0.0.0.3",
    summary = "一款能够模拟旧版 Android 系统固件的 Android 模拟器",
    downloadUrl = "https://github.com/uxazu/AEmulator/releases/download/0.0.0.3/app.aemu.apk",
    icon = Icons.Rounded.Memory,
    category = RecommendCategory.GAME,
    description = "AEmulator boots a real Android 2.3–17 system image straight from a firmware file you already have: a recovery ZIP, an Odin archive, a Google factory image or a GSI. Old 32-bit firmware (2.3–4.4) runs through a patched QEMU, Android 5.0–17 runs natively on the phone’s arm64 cores. The kernel’s binder is emulated, graphics use your phone’s GPU and sound goes through Android’s audio stack — everything runs inside a normal app."
)

/**
 * cccodroid：在 Android 手机上运行 Claude Code 的第三方启动器。
 *
 * 下载走 GitHub Release 直链；
 * 说明文案是作者原始声明，涉及第三方与 Anthropic 的关系，不做改写。
 */
val cccodroidRecommendation = RecommendedApp(
    name = "cccodroid",
    version = "0.4.17",
    summary = "A development launcher that runs Claude Code on Android phones. No root, no custom ROM, no chroot / proot / VM.",
    downloadUrl = "https://github.com/greyoak111/cccodroid/releases/download/v0.4.17/cccodroid-36.apk",
    icon = Icons.Rounded.Construction,
    category = RecommendCategory.TOOLS,
    description = "cccodroid is a third-party project and is not affiliated with Anthropic. It runs the official, unmodified Claude Code: downloaded from Anthropic's official channel at install time (or, when that is unreachable, the identical file Anthropic publishes on npm, via a mirror), verified against the official sha256 manifest, never modified."
)

val astralGameRecommendation = RecommendedApp(
    name = "Astral Game",
    version = "1.6.5",
    summary = "Flutter 游戏联机客户端",
    downloadUrl = "https://github.com/AstralNext/AstralGame/releases/download/v1.6.5/astral-game-1.6.5-android-arm64.apk",
    icon = Icons.Rounded.SportsEsports,
    category = RecommendCategory.GAME,
    description = """
        Astral Game 一款基于 Flutter 构建的游戏联机客户端。

        跨平台一致体验
        • 基于 Flutter 体系打造，界面流畅统一，操作手感细腻。

        联机开黑
        • 为多人联机游戏提供一站式的房间创建、对战与串联体验，邀上好友随时开局。

        轻量高效
        • 安装包精简，启动迅速，运行占用低，中低端机型也能稳定流畅。

        简洁无干扰
        • 去除冗余权限与广告干扰，专注于让你快速进入游戏、专注对战本身。
    """.trimIndent()
)

/** 与 ZexNote 一致的推荐顺序 */
val recommendedApps = listOf(
    appShareRecommendation,
    fluffychatRecommendation,
    reveriePaintRecommendation,
    zalithLauncherRecommendation,
    tomatoRecommendation,
    komiStoreRecommendation,
    cccodroidRecommendation,
    coolMonitorRecommendation,
    twilightToolboxRecommendation,
    aEmulatorRecommendation,
    astralGameRecommendation
)

/**
 * 分类的展示元信息：分类值 + 标题字符串资源 + 图标。
 *
 * 顺序即推荐页上的排列顺序：工具与商店在前（日常需求最高），
 * 创作、游戏、社区在后。
 */
data class RecommendCategoryMeta(
    val category: RecommendCategory,
    @StringRes val titleRes: Int,
    val icon: ImageVector
)

val recommendCategoryMeta: List<RecommendCategoryMeta> = listOf(
    RecommendCategoryMeta(RecommendCategory.TOOLS, R.string.recommend_cat_tools, Icons.Rounded.Construction),
    RecommendCategoryMeta(RecommendCategory.STORE, R.string.recommend_cat_store, Icons.Rounded.Storefront),
    RecommendCategoryMeta(RecommendCategory.GAME, R.string.recommend_cat_game, Icons.Rounded.SportsEsports),
    RecommendCategoryMeta(RecommendCategory.CREATE, R.string.recommend_cat_create, Icons.Rounded.Brush),
    RecommendCategoryMeta(RecommendCategory.SHARE, R.string.recommend_cat_share, Icons.Rounded.Apps)
)

/** 按分类分组，保持 recommendedApps 内部的原始顺序 */
fun appsInCategory(category: RecommendCategory): List<RecommendedApp> =
    recommendedApps.filter { it.category == category }

/** 分类下的条目数量，用于卡片副标题 */
fun countInCategory(category: RecommendCategory): Int =
    recommendedApps.count { it.category == category }
