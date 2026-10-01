/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class UpdateDownloadResult(val file: File, val sha256: String)

object ApkHashVerifier {
    suspend fun sha256(input: InputStream): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}

class DownloadManager(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun download(url: String, target: Uri): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "下载失败：${response.code}" }
            val responseBody = response.body ?: error("下载响应内容为空")
            val output = context.contentResolver.openOutputStream(target) ?: error("无法写入目标文件")
            output.use { stream -> responseBody.byteStream().use { input -> input.copyTo(stream) } }
        }
        val downloadedFile = context.contentResolver.openInputStream(target) ?: error("无法读取已下载文件")
        ApkHashVerifier.sha256(downloadedFile)
    }

    suspend fun downloadUpdateApk(
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): UpdateDownloadResult = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "updates")
        check(directory.exists() || directory.mkdirs()) { "无法创建更新下载目录" }
        val destination = File(directory, fileName.ifBlank { "ZexBrowse-update.apk" })
        val temporary = File(directory, "${destination.name}.download")
        temporary.delete()

        try {
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            val cancellationHandle = coroutineContext.job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            try {
                call.execute().use { response ->
                    check(response.isSuccessful) { "下载失败：${response.code}" }
                    val responseBody = response.body ?: error("下载响应内容为空")
                    val totalBytes = responseBody.contentLength()
                    var downloadedBytes = 0L
                    val digest = MessageDigest.getInstance("SHA-256")

                    responseBody.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                downloadedBytes += count
                                if (totalBytes > 0L) withContext(Dispatchers.Main.immediate) { onProgress(((downloadedBytes * 100) / totalBytes).toInt()) }
                            }
                        }
                    }
                    withContext(Dispatchers.Main.immediate) { onProgress(100) }
                    if (destination.exists()) destination.delete()
                    check(temporary.renameTo(destination)) { "无法完成更新文件保存" }
                    UpdateDownloadResult(destination, digest.digest().joinToString("") { byte -> "%02x".format(byte) })
                }
            } finally {
                cancellationHandle.dispose()
            }
        } catch (throwable: Throwable) {
            temporary.delete()
            throw throwable
        }
    }
}
