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
    /** 镜像站已同步该版本，可在浏览器中走高速通道下载 */
    val lemwoodAvailable: Boolean = false
)

data class Contributor(val name: String, val avatarUrl: String, val contributions: Int)

class UpdateChecker(
    private val client: OkHttpClient = defaultClient()
) {

    /**
     * 检查是否有新版本。本地已是最新或所有来源都不可用时返回 null。
     *
     * 版本号以镜像站的轻量接口为准；确认有新版本后才标记可走高速通道。
     * 镜像站不可用时回退 GitHub Releases。
     */
    suspend fun latest(): ReleaseInfo? {
        val github = githubLatest()

        val lemwoodVersion = LemwoodMirror.latestVersion(client)
        val lemwood = if (lemwoodVersion != null && isNewer(lemwoodVersion)) {
            lemwoodRelease(lemwoodVersion)
        } else null

        return when {
            // 镜像站不可用：回退 GitHub
            lemwood == null -> github?.takeIf { isNewer(it.version) }
            // GitHub 版本更高：以 GitHub 为准，避免镜像同步滞后掩盖新版本
            github != null && isNewer(github.version, lemwood.version) -> github
            // 走镜像站下载，更新说明仍用 GitHub 的（镜像站不提供）
            else -> lemwood.copy(notes = github?.notes.orEmpty())
        }
    }

    /**
     * 按已知版本号构造镜像站结果。
     *
     * 下载必须经浏览器完成站点的 PoW 验证，因此这里只标记该版本可走高速通道，
     * 实际地址由界面拼成直链后交给浏览器打开。
     */
    private fun lemwoodRelease(version: String): ReleaseInfo {
        return ReleaseInfo(
            version = version,
            // 镜像站不提供更新说明，留空时界面显示「暂无更新说明」
            notes = "",
            downloadUrl = "",
            lemwoodAvailable = true
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
