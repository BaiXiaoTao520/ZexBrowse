/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 网页/网盘类下载的二次说明弹窗内容。
 *
 * 这类链接无法在应用内直接下载，需要在跳转浏览器前先说明下载步骤或网盘信息
 * （例如蓝奏云的提取码）。为 null 表示无需说明，直接交给系统打开。
 */
data class BrowserNotice(
    /** 弹窗标题 */
    val title: String,
    /** 弹窗正文（换行分隔） */
    val content: String,
    /** 确认按钮文案 */
    val confirmLabel: String
)

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
    browserNotice = BrowserNotice(
        title = "AppShare 下载说明",
        content = "具体下载步骤：\n\n" +
            "1. 点击页面中的“正式版”。\n" +
            "2. 页面会跳转到蓝奏云。\n" +
            "3. 在蓝奏云页面点击下载即可。\n\n" +
            "建议使用电脑 UA 访问。手机端网页可能会提示开通会员才能下载。",
        confirmLabel = "打开浏览器"
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

val reveriePaintRecommendation = RecommendedApp(
    name = "ReveriePaint",
    version = "v1.4.0",
    summary = "基于 Krita 核心引擎打造的 Android 原生数字绘画应用",
    downloadUrl = "https://v4.gh-proxy.org/https://github.com/LanRhyme/ReveriePaint/releases/download/v1.4.0/ReveriePaint-v1.4.0.apk",
    icon = Icons.Rounded.Brush,
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

val starRingBrowserRecommendation = RecommendedApp(
    name = "星环浏览器-标准版",
    version = "",
    summary = "新一代星环，不止是浏览器，更是你的生活伴侣。",
    downloadUrl = "https://roamexplore.rth1.xyz/",
    icon = Icons.Rounded.Public,
    description = "新一代星环，不止是浏览器，更是你的生活伴侣。"
)

val tomatoRecommendation = RecommendedApp(
    name = "Tomato",
    version = "v2.0.1",
    summary = "开源简洁的番茄钟软件",
    downloadUrl = "https://v4.gh-proxy.org/https://github.com/nsh07/Tomato/releases/download/v2.0.1/tomato-v2.0.1-release.apk",
    icon = Icons.Rounded.Timer,
    description = "开源简洁的番茄钟软件"
)

val komiStoreRecommendation = RecommendedApp(
    name = "Komi Store",
    version = "v1.9.2",
    summary = "开源 Github 仓库应用商店",
    downloadUrl = "https://v4.gh-proxy.org/https://github.com/komi-store/komi-store/releases/download/v1.9.2/Komi-Store-1.9.2.apk",
    icon = Icons.Rounded.Storefront,
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
    browserNotice = BrowserNotice(
        title = "CoolMonitor 网盘下载",
        content = "即将跳转至浏览器\n" +
            "网盘商：蓝奏云\n" +
            "提取码：1c37\n" +
            "建议更换为电脑 UA 访问下载文件",
        confirmLabel = "网盘下载"
    ),
    description = "CoolMonitor 安卓设备健康监控诊断工具，实时监测CPU/电池温度、后台耗电应用、WiFi网络风险，支持App权限审计、垃圾营销通知过滤、电池健康评估，内置AI智能分析设备异常并给出优化建议，纯本地数据处理不上传隐私，无广告免费使用"
)

/** 与 ZexNote 一致的推荐顺序 */
val recommendedApps = listOf(
    appShareRecommendation,
    reveriePaintRecommendation,
    starRingBrowserRecommendation,
    tomatoRecommendation,
    komiStoreRecommendation,
    coolMonitorRecommendation
)
