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
class UpdateChecker(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) { val request = Request.Builder().url("https://api.github.com/repos/BaiXiaoTao520/ZexBrowse/releases/latest").header("Accept", "application/vnd.github+json").build(); client.newCall(request).execute().use { response -> if (response.code == 404) return@use null; if (!response.isSuccessful) error("更新检查失败：${response.code}"); val json = JSONObject(response.body.string()); val asset = json.optJSONArray("assets")?.let { assets -> (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } }; ReleaseInfo(json.optString("tag_name").removePrefix("v"), json.optString("body"), asset?.optString("browser_download_url").orEmpty()) } }
    fun isNewer(remote: String, local: String = "1.0.1"): Boolean = remote.split('.').map { it.toIntOrNull() ?: 0 }.zip(local.split('.').map { it.toIntOrNull() ?: 0 }).firstOrNull { it.first != it.second }?.let { it.first > it.second } ?: remote.split('.').size > local.split('.').size
}
