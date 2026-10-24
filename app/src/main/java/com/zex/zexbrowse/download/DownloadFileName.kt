/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder

/**
 * 解析下载文件的真实名称。
 *
 * 不少站点（OpenList / Alist 系、SharePoint、OneDrive 等）把下载地址做成
 * `download.aspx?...`、`download?path=...` 这类通用入口，末段并非真实文件名。
 * 直接取 URL 末段会得到 `download.aspx` 这种无意义名称，且缺少扩展名会让系统
 * 无法按类型打开文件。
 *
 * 解析顺序（先本地、后网络，网络请求失败时静默回退）：
 * 1. OpenList/Alist 的分享页路径：直接调它的 API 拿真实文件名；
 * 2. URL 查询参数中的显式文件名（filename / name / file 等）；
 * 3. 响应头 `Content-Disposition` 的 filename / filename*（RFC 5987）。
 */
object DownloadFileName {

    /** 常见的「通用下载入口」末段，这类名字不能当作真实文件名 */
    private val genericNames = setOf(
        "download", "download.aspx", "download.php", "download.jsp", "download.do",
        "getfile", "getfile.aspx", "file", "file.aspx", "attachment", "index.php"
    )

    /** 本地推断：仅依据 URL 本身，不发网络请求 */
    fun fromUrl(url: String): String? {
        val query = url.substringAfter('?', "").substringBefore('#')
        if (query.isNotBlank()) {
            val params = query.split('&').mapNotNull { part ->
                val key = part.substringBefore('=').lowercase()
                val value = part.substringAfter('=', "")
                if (value.isBlank()) null else key to value
            }
            // 优先精确的文件名字段，再退回通用的 name / file
            val preferred = listOf("filename", "file_name", "name", "file", "download", "path")
            for (key in preferred) {
                val raw = params.firstOrNull { it.first == key }?.second ?: continue
                val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
                // path 类参数取末段
                val candidate = decoded.substringAfterLast('/').substringAfterLast('\\')
                if (candidate.isNotBlank() && !isGeneric(candidate) && candidate.contains('.')) return candidate
            }
        }
        return null
    }

    /** 是否为常见网盘/对象存储域名（这类地址的 API 探测无意义） */
    private fun isCloudStorageUrl(url: String): Boolean {
        val host = url.substringAfter("://", "").substringBefore('/').lowercase()
        return listOf(
            "microsoftpersonalcontent.com", "sharepoint.com", "onedrive", "1drv.ms",
            "aliyundrive.com", "alipan.com", "lanzou", "pan.baidu.com", "123pan.com",
            "115.com", "quark.cn", "uc.cn", "weiyun.com", "jianguoyun.com",
            "amazonaws.com", "aliyuncs.com", "myqcloud.com", "googleusercontent.com"
        ).any { host.contains(it) }
    }

    /**
     * OpenList / Alist 站点：通过其 API 拿真实文件名。
     *
     * 这类站点把目录页渲染成 SPA（`/fingerprintpay/` 返回 text/html），
     * 实际文件列表在 `/api/fs/list`，单文件信息在 `/api/fs/get`。
     * 后者的 `name` 字段就是真实文件名，而 `raw_url` 往往指向网盘
     * （如 SharePoint 的 `download.aspx`），无 token 时会 302 到登录页，
     * 根本拿不到 `Content-Disposition`。因此必须在 API 层取名字。
     *
     * 支持两种输入：
     * - 具体文件页：`/dir/sub/file.ext` → 直接调 fs/get；
     * - 目录页：`/dir/` → 调 fs/list 取目录下第一个文件（用户通常点的就是这一项）。
     */
    suspend fun fromOpenList(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        val segments = parsed.pathSegments.filter { it.isNotBlank() }
        if (segments.isEmpty()) return null

        val origin = "${parsed.scheme}://${parsed.host}" + if (parsed.port != 80 && parsed.port != 443) ":${parsed.port}" else ""
        val client = OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val jsonType = "application/json; charset=utf-8".toMediaType()

        suspend fun postJson(path: String, body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$origin/api/fs/$path")
                    .header("User-Agent", DOWNLOAD_USER_AGENT)
                    .post(body.toString().toRequestBody(jsonType))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val text = response.body?.string() ?: return@use null
                    val root = JSONObject(text)
                    if (root.optInt("code", -1) != 200) null else root.optJSONObject("data")
                }
            }.getOrNull()
        }

        val fullPath = "/" + segments.joinToString("/")
        // 先当作文件路径查
        postJson("get", JSONObject().put("path", fullPath).put("password", ""))?.let { data ->
            if (!data.optBoolean("is_dir", false)) {
                data.optString("name").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        // 不是文件（或路径不存在）时，当作目录列出内容，取第一个文件
        postJson(
            "list",
            JSONObject().put("path", fullPath).put("password", "").put("page", 1).put("per_page", 100).put("refresh", false)
        )?.let { data ->
            val content = data.optJSONArray("content") ?: return@let
            for (i in 0 until content.length()) {
                val item = content.optJSONObject(i) ?: continue
                if (item.optBoolean("is_dir", false)) continue
                val name = item.optString("name")
                if (name.isNotBlank()) return name
            }
        }
        return null
    }

    /**
     * 通过网络探测真实文件名（读 Content-Disposition）。
     *
     * 不用 HEAD：OpenList/Alist、SharePoint、OneDrive 等站点的下载入口
     * 普遍不支持 HEAD（返回 405），导致永远拿不到响应头。
     * 改用带 Range: bytes=0-0 的 GET：服务器只返回首字节，开销极小，
     * 且兼容绝大多数会带 Content-Disposition 的实现。
     */
    suspend fun resolve(url: String): String? {
        fromUrl(url)?.let { return it }
        // OpenList/Alist 站点：文件信息只能从它的 API 拿（raw_url 多为网盘且需登录）。
        // 仅在 URL 不是网盘直链时才尝试，否则会对 SharePoint 等域名发无效请求、白等超时。
        if (!isCloudStorageUrl(url)) {
            fromOpenList(url)?.let { if (!isGeneric(it)) return it }
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", DOWNLOAD_USER_AGENT)
                    .header("Range", "bytes=0-0")
                    .build()
                client.newCall(request).execute().use { response ->
                    // 206/200 都可能有 Content-Disposition；只读头部，不消费整个 body
                    val disposition = response.header("Content-Disposition")
                    val parsed = if (disposition != null) parseContentDisposition(disposition) else null
                    // 解析结果仍是通用入口时视为无效，交由调用方保留原名
                    parsed?.takeIf { !isGeneric(it) }
                }
            }.getOrNull()
        }
    }

    /**
     * 用已建立的响应头解析文件名（供下载 Worker 复用，避免重复请求）。
     */
    fun fromContentDisposition(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return parseContentDisposition(value)
    }

    /** 从 Content-Disposition 中提取文件名，支持 RFC 5987 的 filename* 形式 */
    private fun parseContentDisposition(value: String): String? {
        // filename*=UTF-8''%E4%B8%AD%E6%96%87.zip
        Regex("filename\\*\\s*=\\s*([^;]+)", RegexOption.IGNORE_CASE).find(value)?.let { match ->
            val raw = match.groupValues[1].trim().trim('"')
            val encoded = raw.substringAfter("''", raw)
            val decoded = runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
            if (decoded.isNotBlank()) return decoded.substringAfterLast('/').substringAfterLast('\\')
        }
        // filename="名字.zip"
        Regex("filename\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(value)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) return name.substringAfterLast('/').substringAfterLast('\\')
        }
        Regex("filename\\s*=\\s*([^;]+)", RegexOption.IGNORE_CASE).find(value)?.let { match ->
            val name = match.groupValues[1].trim().trim('"')
            if (name.isNotBlank()) return name.substringAfterLast('/').substringAfterLast('\\')
        }
        return null
    }

    /** 判断名称是否只是通用下载入口，而非真实文件 */
    fun isGeneric(name: String): Boolean {
        val lower = name.lowercase()
        if (lower in genericNames) return true
        // 没有扩展名的通用入口同样视为无效
        return !lower.contains('.')
    }

    /**
     * 结合当前名称与解析结果，给出最终应使用的文件名。
     * 当前名称本身已合法时优先保留（尊重服务器给出的原始名）；
     * 解析结果本身仍是通用名时视为无效，不采用。
     */
    fun prefer(current: String, resolved: String?): String {
        if (resolved.isNullOrBlank() || isGeneric(resolved)) return current
        return if (isGeneric(current)) resolved else current
    }
}
