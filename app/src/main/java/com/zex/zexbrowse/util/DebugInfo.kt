/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.zex.zexbrowse.UnsupportedSystem
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

/**
 * 调试模式读取的信息聚合。
 *
 * 全部为真实装置与运行环境数据，不含模拟值；用途是排查渲染异常、
 * 内核版本不匹配、UA 覆盖等现场问题时能直接看到实际参数。
 */
object DebugInfo {

    /** 一条「标题 → 内容」的诊断项 */
    data class Item(val label: String, val value: String)

    fun build(context: Context, runtime: GeckoRuntime?, settings: DebugSettingsSnapshot): List<Item> {
        val items = mutableListOf<Item>()

        items += Item("应用版本", "${appVersionName(context)} (${appVersionCode(context)})")
        items += Item("包名", context.packageName)
        items += Item("Android", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        items += Item("设备", "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}")
        items += Item("系统属性", systemSummary())
        items += Item("CPU 架构", Build.SUPPORTED_ABIS.joinToString(", "))

        // GeckoView 版本与实际 UA：UA 覆盖是「网页显示成手机版/桌面版」类问题的首要排查点
        val gecko = runCatching { GeckoSession.getDefaultUserAgent() }.getOrNull().orEmpty()
        val major = Regex("Firefox/([0-9]+)").find(gecko)?.groupValues?.getOrNull(1)
        items += Item("GeckoView", if (major != null) "Firefox $major" else "未知")
        items += Item("默认 UA", gecko.ifBlank { "读取失败" })

        val uaOverride = runCatching { runtime?.settings?.userAgentOverride }.getOrNull()
        items += Item("UA 覆盖", uaOverride?.ifBlank { "未覆盖" } ?: "未覆盖")

        items += Item("当前 UA 模式", settings.userAgentMode)
        items += Item("硬件加速", if (settings.hardwareAcceleration) "开启（SurfaceView）" else "关闭（TextureView）")
        items += Item("HTTPS-Only", if (settings.httpsOnlyEnabled) settings.httpsOnlyMode else "关闭")
        items += Item("安全防护", settings.securityMode)
        items += Item("广告拦截", if (settings.adBlockEnabled) "开启" else "关闭")
        items += Item("深色模式", settings.darkMode)

        return items
    }

    private fun systemSummary(): String {
        val vendor = UnsupportedSystem.detect()
        return if (vendor != null) "$vendor（已停止支持）" else "受支持"
    }

    fun appVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    @Suppress("DEPRECATION")
    fun appVersionCode(context: Context): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else info.versionCode.toLong()
    }.getOrDefault(0L)

    /** APK 安装来源与签名摘要，用于确认装的是自建包还是应用商店重签包 */
    fun installerSummary(context: Context): String = runCatching {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer?.ifBlank { null } ?: "未知（可能为本地安装）"
    }.getOrElse { "读取失败" }

    fun signatureSummary(context: Context): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@runCatching "不可用"
        val info = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo
        val certs = info?.apkContentsSigners
        val first = certs?.firstOrNull() ?: return@runCatching "不可用"
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(first.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }.getOrElse { "读取失败" }
}

/**
 * 诊断面板需要的设置快照，避免 [DebugInfo] 依赖整个 BrowserSettings。
 */
data class DebugSettingsSnapshot(
    val userAgentMode: String,
    val hardwareAcceleration: Boolean,
    val httpsOnlyEnabled: Boolean,
    val httpsOnlyMode: String,
    val securityMode: String,
    val adBlockEnabled: Boolean,
    val darkMode: String
)
