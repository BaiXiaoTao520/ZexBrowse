/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import com.zex.zexbrowse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 柠泽资源站（柠枺镜像）高速下载服务接入。
 *
 * API 基础地址与启动器 ID 通过构建期注入（见 app/build.gradle.kts 的
 * LEMWOOD_API_BASE / LEMWOOD_LAUNCHER_ID），源码中不出现，避免公开仓库暴露接入点。
 * 未注入时 [isAvailable] 为 false，界面上不显示该入口。
 *
 * 下载一律交给浏览器：站点在发放文件前要求完成 PoW 人机验证，
 * 该验证由站点前端在网页中完成，应用侧只提供跳转入口，不代为求解。
 *
 * 相关端点（官方文档 https://miawa.cn/apidocs）：
 * - GET /latest/{id}        版本检查（JSON 信封，data.version）；
 * - GET /download/{path}    下载直链，未带令牌时会 302 到 /verify 验证页。
 */
object LemwoodMirror {

    /** API 基础地址（形如 https://example.com/api/v2）；构建期注入，为空表示未配置 */
    private val apiBase: String get() = BuildConfig.LEMWOOD_API_BASE.trim().trimEnd('/')

    /** 本应用在该平台上的启动器 ID；构建期注入 */
    private val launcherId: String get() = BuildConfig.LEMWOOD_LAUNCHER_ID.trim()

    /** 功能是否可用：配置齐备即可，不依赖任何接口调用成功 */
    val isAvailable: Boolean get() = apiBase.isNotBlank() && launcherId.isNotBlank()

    /** 站点入口 */
    fun siteUrl(): String = apiBase.substringBefore("/api")

    /**
     * 查询本应用的最新版本号（去掉 v 前缀）。
     *
     * 该端点返回 JSON 信封 `{"data":{"version":"v6.3.6"}}`，比版本列表接口轻量；
     * 失败时返回 null 由调用方回退 GitHub。
     */
    suspend fun latestVersion(client: OkHttpClient): String? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$apiBase/latest/$launcherId")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val text = response.body?.string()?.trim().orEmpty()
                if (text.isBlank()) return@use null
                val raw = if (text.startsWith("{")) {
                    val root = runCatching { JSONObject(text) }.getOrNull() ?: return@use null
                    if (!root.isNull("error")) return@use null
                    // 形如 {"data":{"version":"v6.3.6"}}；兼容纯文本响应
                    root.optJSONObject("data")?.optString("version") ?: text
                } else text
                raw.trim().removePrefix("v").removePrefix("V").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    /**
     * 指定版本的 APK 下载直链。
     *
     * 浏览器打开该地址时会先跳转到 /verify 完成 PoW 验证，通过后才开始下载。
     */
    fun apkDownloadUrl(version: String): String =
        "${siteUrl()}/download/$launcherId/v$version/app-release.apk"
}
