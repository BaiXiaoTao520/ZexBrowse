/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 搜索建议：使用国内可直连的搜索引擎联想接口。
 *
 * 选用百度 sugrec（覆盖与更新优于其它源，且无需 Key、无鉴权）。
 * 任何失败都返回空列表，由界面层决定回退到本地搜索历史，不影响正常搜索。
 */
object SearchSuggestions {

    // 联想接口应快速返回：超时设短一些，避免用户等待时卡顿
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * 拉取搜索建议。
     *
     * @param query 用户当前输入的关键词
     * @return 建议词列表；网络失败或解析失败时返回空列表
     */
    suspend fun fetch(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.baidu.com/sugrec?prod=pc&wd=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use emptyList()
                val json = JSONObject(body)
                val groups = json.optJSONArray("g") ?: return@use emptyList()
                (0 until groups.length())
                    .mapNotNull { index ->
                        groups.optJSONObject(index)?.optString("q")?.takeIf { it.isNotBlank() }
                    }
                    // 接口偶尔会把查询词本身排除，这里去重并过滤与输入完全相同的项
                    .filter { it != query }
                    .distinct()
            }
        }.getOrDefault(emptyList())
    }
}
