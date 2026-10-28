/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.zex.zexbrowse.R
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

    /** 更新包存放目录：不进用户下载目录，仅用于应用内更新流程 */
    private fun updateDirectory(): File =
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "updates")

    /** 更新包与同名临时文件；统一定位，避免下载与清场指向不同路径 */
    private fun updateFiles(fileName: String): Pair<File, File> {
        val destination = File(updateDirectory(), fileName.ifBlank { "ZexBrowse-update.apk" })
        return destination to File(destination.parentFile, "${destination.name}.download")
    }

    /** 删除更新包及其未完成的临时文件，供取消下载 / 放弃安装时清场，避免残留 */
    fun deleteUpdateApk(fileName: String) {
        val (destination, temporary) = updateFiles(fileName)
        temporary.delete()
        destination.delete()
    }

    suspend fun download(url: String, target: Uri): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", DOWNLOAD_USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { context.getString(R.string.dl_err_http, response.code.toString()) }
            val responseBody = response.body ?: error(context.getString(R.string.dl_err_empty_body))
            val output = context.contentResolver.openOutputStream(target) ?: error(context.getString(R.string.dl_err_cannot_write))
            output.use { stream -> responseBody.byteStream().use { input -> input.copyTo(stream) } }
        }
        val downloadedFile = context.contentResolver.openInputStream(target) ?: error(context.getString(R.string.dl_err_cannot_read))
        ApkHashVerifier.sha256(downloadedFile)
    }

    suspend fun downloadUpdateApk(
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): UpdateDownloadResult = withContext(Dispatchers.IO) {
        val directory = updateDirectory()
        check(directory.exists() || directory.mkdirs()) { context.getString(R.string.dl_err_cannot_create_dir) }
        val (destination, temporary) = updateFiles(fileName)
        temporary.delete()

        try {
            val request = Request.Builder().url(url).header("User-Agent", DOWNLOAD_USER_AGENT).build()
            val call = client.newCall(request)
            val cancellationHandle = coroutineContext.job.invokeOnCompletion { cause ->
                if (cause is CancellationException) call.cancel()
            }
            try {
                call.execute().use { response ->
                    check(response.isSuccessful) { context.getString(R.string.dl_err_http, response.code.toString()) }
                    val responseBody = response.body ?: error(context.getString(R.string.dl_err_empty_body))
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
                    check(temporary.renameTo(destination)) { context.getString(R.string.dl_err_cannot_save_update) }
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
