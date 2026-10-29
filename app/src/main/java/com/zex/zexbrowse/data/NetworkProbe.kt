/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 应用推荐页的连通性探测。
 *
 * 推荐条目全部托管在境外站点（GitHub 及其代理），国内网络未必可达。
 * 进入页面时先探测一次，通才展示内容，不通则给出提示，避免用户看到
 * 一堆点了没反应的应用卡片。
 */
object NetworkProbe {

    /** 探测地址：与应用推荐的实际取源站点一致 */
    private const val PROBE_URL = "https://www.github.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * 是否可访问 [PROBE_URL]。
     *
     * 只要求拿到任意 HTTP 响应即视为连通：目标站点对 HEAD 可能返回 403/405，
     * 那是「网络通、服务拒绝」而非「断网」，同样不该拦下推荐内容。
     */
    suspend fun isReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(PROBE_URL)
                .head()
                .build()
            client.newCall(request).execute().use { it.code > 0 }
        } catch (cancelled: CancellationException) {
            // 取消不是网络失败：若把它当成 false，协程被中止时调用方会误判成「不通」
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}
