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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Mirror 酱（MirrorChyan）高速下载服务接入。
 *
 * 资源 ID 通过构建期注入（见 app/build.gradle.kts 的 MIRRORCHYAN_RES_ID），
 * 源码中不出现，避免在公开仓库暴露。
 */
object MirrorChyan {
    /**
     * 服务总开关。
     *
     * 平台方（Mirror 酱商务）建议：新项目先稳定运行一段时间后再接入，
     * 否则平台上会出现大量空 rid 与低完成度/断更条目，影响用户观感。
     * 因此默认关闭：关闭时应用不发起任何 Mirror 酱请求，相关入口置灰显示「暂不可用」。
     *
     * 平台确认接入后，只需把仓库变量 MIRRORCHYAN_ENABLED 设为 true 并配置 MIRRORCHYAN_RES_ID，
     * 无需修改任何代码即可启用全部逻辑（见 app/build.gradle.kts）。
     */
    private val serviceEnabled: Boolean get() = BuildConfig.MIRRORCHYAN_ENABLED

    /** 资源 ID：构建期注入；为空表示未注入 */
    val resId: String get() = BuildConfig.MIRRORCHYAN_RES_ID.trim()

    /** 是否已在构建期注入资源 ID（仅表示配置存在，不代表功能可用） */
    val isConfigured: Boolean get() = resId.isNotBlank()

    /** 功能是否真正可用：平台开关已开放 且 已注入资源 ID；任一不满足都不发起请求 */
    val isAvailable: Boolean get() = serviceEnabled && isConfigured

    /** 资源在 Mirror 酱平台的展示页（用户可在此获取 CDK / 订阅更新） */
    fun projectUrl(source: String? = null): String {
        val base = "https://mirrorchyan.com/zh/projects?rid=$resId"
        // source 用于统计「付费源」来源，便于开发者了解用户从何处打开
        return if (source.isNullOrBlank()) base else "$base&source=$source"
    }

    fun apiUrl(): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("mirrorchyan.com")
        .addPathSegments("api/resources/$resId/latest")
        .addQueryParameter("current_version", "v${BuildConfig.VERSION_NAME}")
        .addQueryParameter("os", "android")
        .addQueryParameter("arch", "arm64")
        // user_agent 反映用户从何处使用 CDK，对应平台统计面板中的「签到源」
        .addQueryParameter("user_agent", "ZexBrowse")
        .build()

    /** 接口返回信息（供界面层提示用，不含任何 CDK 明文） */
    data class MirrorResult(
        val version: String,
        val notes: String,
        /** 带时效的下载地址；无更新、未填 CDK 或 CDK 失效时为空 */
        val downloadUrl: String,
        /** 平台侧消息（如「今日下载次数已达上限」）；code=0 时为空字符串 */
        val message: String
    )

    /**
     * 查询 Mirror 酱最新版本。
     *
     * 官方建议即使用户未填 CDK 也调用本接口（CDN 连通性优于 GitHub），
     * 此时仅拿到版本信息、拿不到下载地址，下载回退到 GitHub。
     * 任何失败都不抛出，返回 null 由调用方回退。
     */
    suspend fun latest(cdk: String, client: OkHttpClient): MirrorResult? = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext null
        runCatching {
            val url = apiUrl().newBuilder()
                .apply { if (cdk.isNotBlank()) addQueryParameter("cdk", cdk) }
                .build()
            val request = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return@use null
                val payload = JSONObject(body.string())
                val code = payload.optString("code").ifBlank { payload.optInt("code", Int.MIN_VALUE).toString() }
                val message = payload.optString("msg")
                val data = payload.optJSONObject("data")
                val version = data?.optString("version_name").orEmpty().removePrefix("v").trim()
                // url 仅在「有新版本 + CDK 有效」时返回；无 url 时调用方回退到 GitHub 直链
                MirrorResult(
                    version = version,
                    notes = data?.optString("release_note").orEmpty(),
                    downloadUrl = data?.optString("url").orEmpty(),
                    message = mirrorMessage(code, message)
                )
            }
        }.getOrNull()
    }

    /** 把平台错误码翻译成用户能看懂的中文提示（对应官方 ErrorCode.md） */
    private fun mirrorMessage(code: String, fallback: String): String = when (code) {
        "0" -> ""
        "1001" -> "请求参数不正确"
        "7001" -> "CDK 已过期"
        "7002" -> "CDK 错误"
        "7003" -> "CDK 今日下载次数已达上限"
        "7004" -> "CDK 类型与待下载资源不匹配"
        "7005" -> "CDK 已被封禁"
        "8001" -> "对应架构和系统下的资源不存在"
        "8002" -> "错误的系统参数"
        "8003" -> "错误的架构参数"
        "8004" -> "错误的更新通道参数"
        else -> fallback.ifBlank { "Mirror 酱请求失败（$code）" }
    }
}

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val downloadUrl: String,
    /** Mirror 酱资源页地址；未配置或未查询到时为 null */
    val mirrorUrl: String? = null,
    /** Mirror 酱高速下载地址（带时效）；未提供时为 null */
    val mirrorDownloadUrl: String? = null,
    /** Mirror 酱返回的提示信息（如 CDK 失效原因），供界面展示 */
    val mirrorMessage: String? = null
)

data class Contributor(val name: String, val avatarUrl: String, val contributions: Int)

class UpdateChecker(
    private val client: OkHttpClient = defaultClient(),
    /** 用户在设置中填写的 Mirror 酱激活密钥；为空时仅能查询版本、无法使用高速下载 */
    private val cdk: String = ""
) {

    /**
     * 获取最新版本信息，并合并 GitHub Releases 与 Mirror 酱两个来源。
     *
     * 规则（遵循 Mirror 酱官方集成建议）：
     * 1. 两个来源都查询，取版本更高的一方作为「最新版本」；
     * 2. 版本相同时优先使用 Mirror 酱的版本日志（通常是面向国内用户的维护说明）；
     * 3. 下载地址优先用 Mirror 酱返回的 url（需 CDK 有效），拿不到则回退 GitHub 直链。
     */
    suspend fun latest(): ReleaseInfo? {
        val github = githubLatest()
        val mirror = MirrorChyan.latest(cdk, client)

        // Mirror 酱未接入 / 网络失败：直接用 GitHub 结果
        if (mirror == null) return github

        val mirrorMessage = mirror.message.takeIf { it.isNotBlank() }

        // 平台返回业务错误（如 CDK 错误/过期/次数用尽）时仍会带上版本信息，
        // 按官方流程图：错误只影响「能否拿到高速下载 url」，不应丢弃版本号。
        if (mirror.version.isBlank()) {
            return github?.copy(mirrorMessage = mirrorMessage)
        }

        // 功能未开放时不构造任何 Mirror 酱地址，避免下游误展示入口
        val mirrorUrl = if (MirrorChyan.isAvailable) MirrorChyan.projectUrl(source = "zexbrowse_app_update") else null

        if (github != null && isNewer(github.version, mirror.version)) {
            // GitHub 版本更高：以 GitHub 为准，但附带 Mirror 酱入口与提示
            return github.copy(
                mirrorUrl = mirrorUrl,
                mirrorMessage = mirrorMessage
            )
        }

        val sameVersion = github != null && !isNewer(github.version, mirror.version) && !isNewer(mirror.version, github.version)
        return ReleaseInfo(
            version = mirror.version,
            notes = mirror.notes.ifBlank { github?.notes.orEmpty() },
            // 下载优先级：Mirror 酱 url（需 code=0 且有 url）> GitHub 直链
            downloadUrl = mirror.downloadUrl.ifBlank { if (sameVersion) github?.downloadUrl.orEmpty() else "" },
            mirrorUrl = mirrorUrl,
            mirrorDownloadUrl = if (MirrorChyan.isAvailable) mirror.downloadUrl.takeIf { it.isNotBlank() } else null,
            mirrorMessage = mirrorMessage
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
                    .ifBlank { "https://github.com/BaiXiaoTao520/ZexBrowse/releases/latest/download/app-release.apk" },
                mirrorUrl = if (MirrorChyan.isAvailable) MirrorChyan.projectUrl("zexbrowse_app_update") else null
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
        // 更新检查与 Mirror 酱查询都需要等待网络，统一设置较短超时，避免长时间无响应
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
