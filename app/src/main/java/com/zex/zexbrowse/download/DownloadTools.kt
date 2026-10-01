/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.security.MessageDigest

object ApkHashVerifier { suspend fun sha256(input: InputStream): String = withContext(Dispatchers.IO) { val digest = MessageDigest.getInstance("SHA-256"); input.use { stream -> val bytes = ByteArray(8192); while (true) { val count = stream.read(bytes); if (count < 0) break; digest.update(bytes, 0, count) } }; digest.digest().joinToString("") { "%02x".format(it) } } }
class DownloadManager(private val context: Context) {
    private val client = OkHttpClient()
    suspend fun download(url: String, target: Uri): String = withContext(Dispatchers.IO) { val response = client.newCall(Request.Builder().url(url).build()).execute(); response.use { check(it.isSuccessful) { "下载失败：${it.code}" }; context.contentResolver.openOutputStream(target)?.use { output -> it.body.byteStream().use { input -> input.copyTo(output) } } ?: error("无法写入目标文件") }; context.contentResolver.openInputStream(target)?.use { ApkHashVerifier.sha256(it) }.orEmpty() }
}
