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
     * 获取最新版本信息。
     *
     * 版本来源优先柠枺镜像（国内连通性好、带官方 SHA-256），拿不到时回退 GitHub Releases。
     * 两者都失败时返回 null。
     */
    suspend fun latest(): ReleaseInfo? {
        val lemwood = lemwoodLatest()
        val github = githubLatest()

        // 柠枺可用：以它为版本来源（与 GitHub 比较，取更高的一方，避免镜像同步滞后掩盖新版本）
        if (lemwood != null) {
            if (github == null || !isNewer(github.version, lemwood.version)) return lemwood
            // GitHub 版本更高：版本号以 GitHub 为准，但保留柠枺的站点入口
            return github.copy(lemwoodSiteUrl = lemwood.lemwoodSiteUrl)
        }
        return github
    }

    /**
     * 从柠枺镜像获取最新版本及其 APK 资产信息。
     *
     * 站点未配置或请求失败时返回 null，由调用方回退 GitHub。
     */
    private suspend fun lemwoodLatest(): ReleaseInfo? {
        if (!LemwoodMirror.isAvailable) return null
        val releases = LemwoodMirror.releases(client) ?: return null
        val siteUrl = LemwoodMirror.siteUrl().takeIf { it.isNotBlank() }
        val tag = releases.assetsByTag.keys.firstOrNull() ?: return null
        val asset = releases.assetsByTag[tag]
            ?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return ReleaseInfo(
            version = releases.latestVersion,
            // 镜像站不提供更新说明，留给 GitHub 补全（同版本时）
            notes = "",
            downloadUrl = asset?.url.orEmpty(),
            lemwoodAssetUrl = asset?.url?.takeIf { it.isNotBlank() },
            lemwoodSha256 = asset?.sha256?.takeIf { it.isNotBlank() },
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
