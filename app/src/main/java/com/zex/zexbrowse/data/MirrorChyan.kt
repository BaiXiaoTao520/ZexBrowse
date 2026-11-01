/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import com.zex.zexbrowse.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * MirrorChyan（镜像酱）API 客户端。
 *
 * 严格按官方文档实现：
 * `GET https://mirrorchyan.com/api/resources/{res_id}/latest`
 * 必填 res_id（路径），可选 current_version / cdk / os / arch。
 *
 * 官方约定：即使没有 CDK 也建议用它检查更新——它在国内与境外都有大量 CDN 节点，
 * 连通性远好于直连 GitHub。因此这里只把它当作「版本号与更新日志的来源」，
 * 不使用它返回的 url：高速下载由网页侧完成，应用只提供项目页跳转。
 */
object MirrorChyan {

    /**
     * 资源标识符（rid），由 Mirror酱 侧分配。
     *
     * 与 CDK 一样通过构建期注入，不写死在源码里：
     * 公开仓库不应暴露接入点，这也是本项目接入柠枺时的既有约定。
     * 未注入时 [isAvailable] 为 false，界面上不展示相关入口。
     */
    private val resId: String get() = BuildConfig.MIRRORCHYAN_RID.trim()

    /** 配置齐备才可用；CDK 缺失不算不可用（仍能查版本号） */
    val isAvailable: Boolean get() = resId.isNotBlank()

    private const val BASE = "https://mirrorchyan.com/api/resources"

    /**
     * 项目页地址。
     *
     * 用户点「Mirror酱高速下载」时跳到这里，由网页侧完成下载分发。
     */
    fun projectUrl(): String = "https://mirrorchyan.com/zh/projects?rid=$resId"

    /** 一次查询的结果 */
    data class Result(
        /** 最新版本号（始终返回，取不到时为 null） */
        val version: String?,
        /** 版本日志 */
        val releaseNote: String,
        /** 业务错误码，0 表示成功 */
        val code: Int,
        /** 错误说明 */
        val message: String
    ) {
        val succeeded: Boolean get() = code == 0
    }

    /**
     * 查询最新版本。
     *
     * CDK 来自 BuildConfig（构建期由 Secrets 注入），不硬编码进源码。
     * 未配置 CDK 时也能查：服务端仍返回 version_name 与 release_note。
     *
     * @param currentVersion 当前本地版本号，用于服务端判断是否有更新
     */
    suspend fun latest(currentVersion: String): Result = withContext(Dispatchers.IO) {
        // rid 未注入时不发请求：否则会打到一个无效路径，白白等一轮超时
        if (!isAvailable) return@withContext Result(null, "", -1, "未配置资源 ID")

        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        // os / arch 按官方参数传入，避免服务端按默认值返回不匹配架构的资源
        val url = buildString {
            append(BASE).append('/').append(resId).append("/latest")
            append("?current_version=").append(currentVersion)
            append("&os=android")
            append("&arch=").append(androidArch())
            // CDK 仅在构建期注入且非空时附加；绝不打印、不写日志
            val cdk = BuildConfig.MIRRORCHYAN_CDK
            if (cdk.isNotBlank()) append("&cdk=").append(cdk)
        }

        try {
            val request = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@use Result(null, "", response.code, "HTTP ${response.code}")
                }
                val json = runCatching { JSONObject(body) }.getOrNull()
                    ?: return@use Result(null, "", response.code, "响应解析失败")
                val code = json.optInt("code", response.code)
                val message = json.optString("msg").ifBlank { "HTTP ${response.code}" }
                val data = json.optJSONObject("data")
                Result(
                    version = data?.optString("version_name")?.takeIf { it.isNotBlank() },
                    releaseNote = data?.optString("release_note").orEmpty(),
                    code = code,
                    message = message
                )
            }
        } catch (cancelled: CancellationException) {
            // 取消不是网络失败，不能被当成「查询失败」写进状态
            throw cancelled
        } catch (e: Exception) {
            Result(null, "", -1, e.message ?: "请求失败")
        }
    }

    /**
     * 官方参数要求 arch 取具体架构名。
     *
     * 用 Build.SUPPORTED_ABIS 的首项，与 APK 实际打包的架构一致；
     * 取不到时退回 arm64-v8a（本项目 abiFilters 只保留 arm64-v8a）。
     */
    private fun androidArch(): String =
        android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
}
