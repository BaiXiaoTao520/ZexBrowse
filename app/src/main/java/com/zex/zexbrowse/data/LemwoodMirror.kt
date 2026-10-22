/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import com.zex.zexbrowse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 柠枺镜像（Lemwood）高速下载服务接入。
 *
 * API 域名与启动器 ID 通过构建期注入（见 app/build.gradle.kts 的
 * LEMWOOD_API_BASE / LEMWOOD_LAUNCHER_ID），源码中不出现，避免公开仓库暴露接入点。
 * 未注入时 [isAvailable] 为 false，界面上不显示该入口。
 */
object LemwoodMirror {

    /** API 基础地址（形如 https://example.com/api/v2）；构建期注入，为空表示未配置 */
    val apiBase: String get() = BuildConfig.LEMWOOD_API_BASE.trim().trimEnd('/')

    /** 本应用在该平台上的启动器 ID；构建期注入 */
    val launcherId: String get() = BuildConfig.LEMWOOD_LAUNCHER_ID.trim()

    /** 是否已在构建期注入全部必要配置 */
    val isConfigured: Boolean get() = apiBase.isNotBlank() && launcherId.isNotBlank()

    /**
     * 功能是否可用。
     * 与 Mirror 酱不同，柠枺镜像无需用户填写密钥，只要配置齐备即可用。
     */
    val isAvailable: Boolean get() = isConfigured

    /** 站点入口（供界面引导用户查看发布页） */
    fun siteUrl(): String = apiBase.substringBefore("/api")

    /**
     * 本应用的启动器详情页：也是站点 PoW 验证流程的入口。
     * 下载必须经此页完成验证拿到一次性 token，不能直接拼 download_path，
     * 否则会绕过站点的验证机制。
     */
    fun launcherPageUrl(): String = "${siteUrl()}/launchers/$launcherId"

    /** 查询指定启动器最新版本的接口地址 */
    fun latestUrl(): HttpUrl? =
        "$apiBase/launchers/$launcherId".toHttpUrlOrNull()

    /** 单次查询结果 */
    data class Result(
        /** 最新版本号，已去掉前缀 v */
        val version: String
    )

    /**
     * 查询柠枺镜像上本应用的最新版本。
     *
     * 任何失败都不抛出，返回 null 由调用方回退到 GitHub 直链。
     */
    suspend fun latest(client: OkHttpClient): Result? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        val url = latestUrl() ?: return@withContext null
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                parse(body.string())
            }
        }.getOrNull()
    }

    /**
     * 解析接口返回。
     *
     * 平台返回体形如：
     * `{"version":"1.2.6.3","download_path":"download/xxx/1.2.6.3","assets":[...]}`
     * 不同部署可能略有差异，这里只取版本号用于判断是否有新版本；
     * 下载地址一律走启动器详情页，由站点完成 PoW 验证后发放一次性 token。
     */
    private fun parse(raw: String): Result? {
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val version = payload.optString("version").trim().removePrefix("v").removePrefix("V")
        if (version.isBlank()) return null
        return Result(version = version)
    }
}
