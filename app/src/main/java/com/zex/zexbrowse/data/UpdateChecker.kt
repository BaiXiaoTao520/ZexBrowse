/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import com.zex.zexbrowse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
     * 版本号优先取镜像站的轻量接口（国内连通性好），不可用时回退 GitHub。
     * GitHub 侧同时走 Releases API 与 Atom 订阅源两条通道：未认证请求有频控限制，
     * 两条通道任一成功即可，避免单点被限流时完全查不到更新。
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

    /**
     * GitHub 最新版本：Releases API 与 Atom 订阅源双通道并行。
     *
     * 两条通道信息互补：API 带 assets 下载直链，Atom 带更新说明且不受同一频控。
     * 任一成功即可返回；两者都失败时返回 null。
     */
    private suspend fun githubLatest(): ReleaseInfo? = coroutineScope {
        val viaApi = async { githubLatestViaApi() }
        val viaAtom = async { githubLatestViaAtom() }
        val api = viaApi.await()
        val atom = viaAtom.await()

        when {
            // 只有一路可用时直接用
            api == null -> atom
            atom == null -> api
            // 两路都有：版本取较高的一方，说明取非空的，下载地址优先用 API 的直链
            else -> {
                val useAtom = isNewer(atom.version, api.version)
                ReleaseInfo(
                    version = if (useAtom) atom.version else api.version,
                    notes = if (useAtom) atom.notes.ifBlank { api.notes } else api.notes.ifBlank { atom.notes },
                    downloadUrl = api.downloadUrl.ifBlank { atom.downloadUrl }
                )
            }
        }
    }

    /** 通道一：GitHub Releases REST API（未认证有每小时频控） */
    private suspend fun githubLatestViaApi(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/BaiXiaoTao520/ZexBrowse/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                // 404 表示仓库无 release；403/429 表示触发频控，交给另一条通道
                if (!response.isSuccessful) return@use null
                val responseBody = response.body ?: return@use null
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
        }.getOrNull()
    }

    /**
     * 通道二：官方 Releases Atom 订阅源。
     *
     * 不经过 api.github.com，因此不受未认证 API 的频控；但不提供 assets 下载直链，
     * 版本与说明可用。下载地址回退到 releases/latest 的固定路径。
     */
    private suspend fun githubLatestViaAtom(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://github.com/BaiXiaoTao520/ZexBrowse/releases.atom")
                .header("Accept", "application/atom+xml")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                // Atom 按发布时间倒序，第一个 entry 即最新版本
                val entry = Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
                    .find(body)?.groupValues?.getOrNull(1) ?: return@use null
                val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
                    .find(entry)?.groupValues?.getOrNull(1)?.trim() ?: return@use null
                val version = title.removePrefix("v").trim()
                if (version.isBlank()) return@use null
                // 说明是 HTML 转义后的正文；这里只取纯文本给弹窗展示
                val content = Regex("<content[^>]*>(.*?)</content>", RegexOption.DOT_MATCHES_ALL)
                    .find(entry)?.groupValues?.getOrNull(1).orEmpty()
                ReleaseInfo(
                    version = version,
                    notes = atomTextToPlain(content),
                    downloadUrl = "https://github.com/BaiXiaoTao520/ZexBrowse/releases/latest/download/app-release.apk"
                )
            }
        }.getOrNull()
    }

    /**
     * 把 Atom 的正文还原成纯文本。
     *
     * Atom 里的 content 是 XML 转义后的 HTML（形如 `&lt;h2&gt;`），
     * 必须先解实体再削标签，否则标签会原样显示。
     */
    private fun atomTextToPlain(raw: String): String {
        if (raw.isBlank()) return ""
        val html = unescapeXml(raw)
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|li|h[1-6]|div)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /** 单遍解 XML 实体，避免 `&amp;lt;` 被二次解码成 `<` */
    private fun unescapeXml(value: String): String {
        val entities = mapOf(
            "lt" to "<", "gt" to ">", "quot" to "\"", "#39" to "'", "amp" to "&"
        )
        return Regex("&(lt|gt|quot|#39|amp);").replace(value) { match ->
            entities[match.groupValues[1]] ?: match.value
        }
    }

    suspend fun contributors(): List<Contributor> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.github.com/repos/BaiXiaoTao520/ZexBrowse/contributors").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = response.body ?: error("empty body")
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
