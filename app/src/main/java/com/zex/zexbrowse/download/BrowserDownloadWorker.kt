/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import org.mozilla.geckoview.GeckoSession
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest

// 部分镜像站/服务器会封禁 OkHttp 默认 User-Agent（返回 403/404），需伪装成浏览器 UA 才能下载。
// 版本号从 GeckoView 实际内核读取，避免升级内核后仍声称旧版本；
// 格式必须合法：Gecko/ 后固定 20100101（不是版本号），且需带 rv: 标记，否则会被反爬规则拒绝。
internal val DOWNLOAD_USER_AGENT: String by lazy {
    val ua = runCatching { GeckoSession.getDefaultUserAgent() }.getOrNull().orEmpty()
    val version = Regex("Firefox/([0-9.]+)").find(ua)?.groupValues?.getOrNull(1)
        ?: Regex("rv:([0-9.]+)").find(ua)?.groupValues?.getOrNull(1)
        ?: "158.0"
    "Mozilla/5.0 (Android 14; Mobile; rv:$version) Gecko/20100101 Firefox/$version"
}

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
            val call = client.newCall(Request.Builder().url(url).header("User-Agent", DOWNLOAD_USER_AGENT).build())
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
                            if (isStopped) {
                                call.cancel()
                                throw CancellationException("下载已取消")
                            }
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
                if (isStopped) {
                    call.cancel()
                    throw CancellationException("下载已取消")
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
            val cancelled = isStopped
            database.downloadDao().finishWithMessage(id, if (cancelled) "cancelled" else "failed", if (cancelled) "下载已取消" else (error.message ?: "未知错误"))
            if (!cancelled) notifyFinished(id, fileName, false)
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
        ensureFinishedChannel()
        val notification = NotificationCompat.Builder(applicationContext, FINISHED_CHANNEL_ID)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(fileName.ifBlank { "下载任务" })
            .setContentText(if (success) "下载完成，点击查看" else "下载失败")
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // 完成通知必须用与前台通知不同的 ID：Worker 返回后 WorkManager 会按「前台通知 ID」
        // 调用 cancelNotification，若两者共用同一 ID，刚发出的「下载完成」会被立即取消，
        // 表现为通知中心里看不到（或一闪而过）。
        applicationContext.getSystemService(NotificationManager::class.java).notify(finishedNotificationId(id), notification)
    }

    /**
     * 完成通知 ID：翻转前台通知 ID 的最高位。
     * xor 的结果恒不等于原值，因此与前台通知 ID 一定不同；不同下载之间也几乎不可能碰撞。
     */
    private fun finishedNotificationId(id: String): Int = id.hashCode() xor Int.MIN_VALUE

    /** 点击通知回到应用 */
    private fun openAppPendingIntent(): android.app.PendingIntent {
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: Intent()
        return android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 进行中的下载通知渠道（低重要性，静默更新，避免进度刷新反复打扰）。
     * 渠道创建后可重复调用（幂等）；用标记位避免每次进度变化都走一遍系统调用。
     */
    private var downloadChannelReady = false
    private fun ensureChannel() {
        if (downloadChannelReady) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "浏览器下载", NotificationManager.IMPORTANCE_LOW))
        downloadChannelReady = true
    }

    /**
     * 下载完成通知使用独立渠道（默认重要性）：
     * 进行中的静默通知与「已完成」提示分开，完成后用户能明确感知到。
     */
    private var finishedChannelReady = false
    private fun ensureFinishedChannel() {
        if (finishedChannelReady) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(FINISHED_CHANNEL_ID, "下载完成", NotificationManager.IMPORTANCE_DEFAULT))
        finishedChannelReady = true
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_URI = "uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EXPECTED_HASH = "expected_hash"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "browser_downloads"
        private const val FINISHED_CHANNEL_ID = "browser_downloads_done"
    }
}
