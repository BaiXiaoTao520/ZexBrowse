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
 * 官方文档（https://miawa.cn/apidocs）的两个接口：
 * 1. GET /api/v2/latest                   → 全站版本表，data[{启动器 ID}] 为最新版本号；
 * 2. GET /api/v2/launchers/{id}          → 本应用的版本列表，assets[].url 为下载直链。
 *
 * 检查更新时先查 [latestVersion]（响应体小、字段少），确认有新版本后才查 [releases] 取资产。
 * 未带 token 的直链会 302 到 /verify 验证页，下载需由浏览器完成站点的人机验证。
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

    /** 查询结果：版本列表（最新在前） */
    data class Releases(
        /** 各版本资产；key 为 "v6.3.5" 这类带 v 前缀的 tag 名 */
        val assetsByTag: Map<String, List<Asset>>
    )

    data class Asset(
        val name: String,
        val size: Long,
        val sha256: String,
        /** 下载直链（未带 token，访问会 302 到验证页） */
        val url: String
    )

    /**
     * 查询本应用在镜像站上的最新版本号（去掉 v 前缀）。
     *
     * 走全站版本表 `/api/v2/latest`，一次拿到所有启动器的版本，响应比资产列表小得多；
     * 任何失败都不抛出，返回 null 由调用方回退。
     */
    suspend fun latestVersion(client: OkHttpClient): String? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$apiBase/latest")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                val root = runCatching { JSONObject(body.string()) }.getOrNull() ?: return@use null
                if (!root.isNull("error")) return@use null
                val data = root.optJSONObject("data") ?: return@use null
                data.optString(launcherId).trim()
                    .removePrefix("v").removePrefix("V")
                    .takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    /**
     * 查询本应用的版本列表。任何失败都不抛出，返回 null 由调用方回退。
     */
    suspend fun releases(client: OkHttpClient): Releases? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$apiBase/launchers/$launcherId")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                parseReleases(body.string())
            }
        }.getOrNull()
    }

    private fun parseReleases(raw: String): Releases? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        // error 字段非 null 表示业务失败
        if (!root.isNull("error")) return null
        val data = root.optJSONArray("data") ?: return null
        if (data.length() == 0) return null

        val assetsByTag = LinkedHashMap<String, List<Asset>>()
        for (i in 0 until data.length()) {
            val release = data.optJSONObject(i) ?: continue
            val tag = release.optString("tag_name").trim()
            if (tag.isBlank()) continue
            val assetsJson = release.optJSONArray("assets") ?: continue
            val assets = ArrayList<Asset>(assetsJson.length())
            for (j in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(j) ?: continue
                val url = asset.optString("url").trim()
                if (url.isBlank()) continue
                assets.add(
                    Asset(
                        name = asset.optString("name").trim(),
                        size = asset.optLong("size", 0L),
                        sha256 = asset.optString("sha256").trim(),
                        url = url
                    )
                )
            }
            assetsByTag[tag] = assets
        }
        if (assetsByTag.isEmpty()) return null
        return Releases(assetsByTag = assetsByTag)
    }
}
