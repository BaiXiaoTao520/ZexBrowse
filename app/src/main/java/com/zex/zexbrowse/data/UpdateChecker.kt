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
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    /** 柠枺镜像提供的下载直链（未带令牌，在浏览器中访问会自动 302 到验证页） */
    val lemwoodAssetUrl: String? = null,
    /** 柠枺镜像对应资产的官方 SHA-256，可用于下载校验 */
    val lemwoodSha256: String? = null,
    /** 柠枺镜像站点入口，供用户查看发布页 */
    val lemwoodSiteUrl: String? = null
)

data class Contributor(val name: String, val avatarUrl: String, val contributions: Int)

class UpdateChecker(
    private val client: OkHttpClient = defaultClient()
) {

    /**
     * 检查是否有新版本。本地已是最新或所有来源都不可用时返回 null。
     *
     * 版本号以镜像站的轻量版本表为准（只取版本号，不拉资产列表）；
     * 确认有新版本后才去取资产，避免每次检查都拉完整版本列表。
     * 镜像站不可用时回退 GitHub Releases。
     */
    suspend fun latest(): ReleaseInfo? {
        val github = githubLatest()

        // 镜像站有更新时优先用它：国内连通性好，且带官方 SHA-256
        val lemwoodVersion = LemwoodMirror.latestVersion(client)
        val lemwood = if (lemwoodVersion != null && isNewer(lemwoodVersion)) {
            lemwoodRelease(lemwoodVersion)
        } else null

        return when {
            // 镜像站取不到资产：回退 GitHub
            lemwood == null -> github?.takeIf { isNewer(it.version) }
            // GitHub 版本更高：以 GitHub 为准，避免镜像同步滞后掩盖新版本
            github != null && isNewer(github.version, lemwood.version) -> github
            // 走镜像站下载，更新说明仍用 GitHub 的（镜像站不提供）
            else -> lemwood.copy(notes = github?.notes.orEmpty())
        }
    }

    /**
     * 按已知版本号取镜像站的资产信息（直链与官方 SHA-256）。
     *
     * 资产列表查不到时返回 null，由调用方回退 GitHub。
     */
    private suspend fun lemwoodRelease(version: String): ReleaseInfo? {
        val releases = LemwoodMirror.releases(client) ?: return null
        val siteUrl = LemwoodMirror.siteUrl().takeIf { it.isNotBlank() }
        // tag 形如 v6.3.6，与 version 去掉 v 后的形式对齐
        val tag = releases.assetsByTag.keys.firstOrNull {
            it.removePrefix("v").removePrefix("V") == version
        } ?: return null
        val asset = releases.assetsByTag[tag]
            ?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null
        return ReleaseInfo(
            version = version,
            // 镜像站不提供更新说明，留空时界面显示「暂无更新说明」
            notes = "",
            downloadUrl = asset.url,
            lemwoodAssetUrl = asset.url.takeIf { it.isNotBlank() },
            lemwoodSha256 = asset.sha256.takeIf { it.isNotBlank() },
            lemwoodSiteUrl = siteUrl
        )
    }

    /** GitHub Releases 最新版本 */
    private suspend fun githubLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/BaiXiaoTao520/ZexBrowse/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@use null
            check(response.isSuccessful) { "更新检查失败：${response.code}" }
            val responseBody = response.body ?: error("GitHub Release API 返回空响应")
            val json = JSONObject(responseBody.string())
            val assets = json.optJSONArray("assets")
            val apkAsset = assets?.let { array ->
                (0 until array.length())
                    .map { array.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            }
            ReleaseInfo(
                version = json.optString("tag_name").removePrefix("v"),
                notes = json.optString("body"),
                downloadUrl = apkAsset?.optString("browser_download_url").orEmpty()
                    // 部分 release 未上传 assets：回退到 releases/latest 的固定下载路径
                    .ifBlank { "https://github.com/BaiXiaoTao520/ZexBrowse/releases/latest/download/app-release.apk" }
            )
        }
    }

    suspend fun contributors(): List<Contributor> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.github.com/repos/BaiXiaoTao520/ZexBrowse/contributors").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "贡献者加载失败：${response.code}" }
            val body = response.body ?: error("贡献者响应为空")
            val array = org.json.JSONArray(body.string())
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Contributor(item.optString("login"), item.optString("avatar_url"), item.optInt("contributions"))
            }
        }
    }

    fun isNewer(remote: String, local: String = BuildConfig.VERSION_NAME): Boolean {
        val remoteParts = versionParts(remote)
        val localParts = versionParts(local)
        val count = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until count) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }

    private fun versionParts(version: String): List<Int> =
        version.substringBefore('+')
            .substringBefore('-')
            .split('.')
            .map { part -> part.toIntOrNull() ?: 0 }

    companion object {
        // 更新检查与镜像查询都需要等待网络，统一设置较短超时，避免长时间无响应
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
