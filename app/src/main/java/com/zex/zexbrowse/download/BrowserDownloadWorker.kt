/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.zex.zexbrowse.data.BrowserDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest

class BrowserDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val database = BrowserDatabase.create(appContext)
    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_ID) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME).orEmpty()
        val expectedHash = inputData.getString(KEY_EXPECTED_HASH).orEmpty()
        setForeground(createForegroundInfo(id, fileName, 0, false))

        try {
            val call = client.newCall(Request.Builder().url(url).build())
            val cancellationHandle = kotlin.coroutines.coroutineContext.job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
            val response = call.execute()
            response.use {
                check(it.isSuccessful) { "下载失败：${it.code}" }
                val body = it.body ?: error("下载响应内容为空")
                val total = body.contentLength()
                var downloaded = 0L
                var lastProgress = -1
                val digest = MessageDigest.getInstance("SHA-256")
                val output: OutputStream = if (uri.scheme == "file") {
                    FileOutputStream(File(uri.path.orEmpty()))
                } else {
                    applicationContext.contentResolver.openOutputStream(uri, "wt") ?: error("无法写入所选位置")
                }
                body.byteStream().use { input ->
                    output.buffered().use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            if (isStopped) throw CancellationException("下载已取消")
                            val count = input.read(buffer)
                            if (count < 0) break
                            stream.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloaded += count
                            val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                            if (progress != lastProgress) {
                                lastProgress = progress
                                database.downloadDao().updateProgress(id, "downloading", progress, downloaded, total)
                                setProgress(Data.Builder().putInt(KEY_PROGRESS, progress).build())
                                setForeground(createForegroundInfo(id, fileName, progress, total <= 0))
                            }
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                val status = if (expectedHash.isNotBlank() && !hash.equals(expectedHash, true)) "hash_mismatch" else "completed"
                database.downloadDao().complete(id, status, hash)
                notifyFinished(id, fileName, status == "completed")
                cancellationHandle.dispose()
                Result.success()
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { database.downloadDao().finishWithMessage(id, "cancelled", "下载已取消") }
            throw cancelled
        } catch (error: Throwable) {
            database.downloadDao().finishWithMessage(id, "failed", error.message ?: "未知错误")
            notifyFinished(id, fileName, false)
            Result.failure()
        }
    }

    private fun createForegroundInfo(id: String, fileName: String, progress: Int, indeterminate: Boolean): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName.ifBlank { "正在下载" })
            .setContentText(if (indeterminate) "正在下载" else "$progress%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    private fun notifyFinished(id: String, fileName: String, success: Boolean) {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(fileName.ifBlank { "下载任务" })
            .setContentText(if (success) "下载完成" else "下载失败")
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java).notify(id.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "浏览器下载", NotificationManager.IMPORTANCE_LOW))
        }
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_URI = "uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EXPECTED_HASH = "expected_hash"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "browser_downloads"
    }
}
