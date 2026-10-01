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

data class ReleaseInfo(val version: String, val notes: String, val downloadUrl: String)
data class Contributor(val name: String, val avatarUrl: String, val contributions: Int)

class UpdateChecker(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
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

    fun isNewer(remote: String, local: String = "1.0.5"): Boolean {
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
}
