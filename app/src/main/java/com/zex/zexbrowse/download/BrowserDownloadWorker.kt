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
        // 下载过程中可能被响应头纠正为真实文件名，后续通知与记录都用这个变量
        var displayName = fileName
        val expectedHash = inputData.getString(KEY_EXPECTED_HASH).orEmpty()
        // 先用 NotificationManager 直接发出「准备中」通知：
        // setForeground 走 WorkManager 的前台服务启动流程（Android 12+ 异步），
        // 紧接着的阻塞网络请求会让通知迟迟不出现，用户以为没反应。
        // 直发通知即时可见，随后 setForeground 升级为前台服务（同一通知 ID，不闪重复）。
        postPreparingNotification(id, fileName)
        setForeground(createForegroundInfo(id, fileName, 0, true))

        try {
            val call = client.newCall(Request.Builder().url(url).header("User-Agent", DOWNLOAD_USER_AGENT).build())
            val cancellationHandle = kotlin.coroutines.coroutineContext.job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
            val response = call.execute()
            response.use {
                check(it.isSuccessful) { "下载失败：${it.code}" }
                // 兜底：若入队时仍未拿到真实文件名，用本次下载的响应头纠正，
                // 保证「显示的文件名」与「实际落盘文件名」一致。
                if (DownloadFileName.isGeneric(displayName)) {
                    val fromHeader = DownloadFileName.fromContentDisposition(it.header("Content-Disposition"))
                    if (fromHeader != null && !DownloadFileName.isGeneric(fromHeader)) {
                        displayName = fromHeader
                        database.downloadDao().rename(id, displayName)
                        setForeground(createForegroundInfo(id, displayName, 0, true))
                    }
                }
                val body = it.body ?: error("下载响应内容为空")
                val total = body.contentLength()
                var downloaded = 0L
                var lastProgress = -1
                var lastNotifyAt = 0L
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
                            // 百分比变化或距上次刷新超过 500ms 时刷新：
                            // 只按百分比会让小文件从 0% 直接跳到 100%、通知永远看不到中间进度
                            val now = System.currentTimeMillis()
                            if (progress != lastProgress || now - lastNotifyAt >= 500L) {
                                lastProgress = progress
                                lastNotifyAt = now
                                database.downloadDao().updateProgress(id, "downloading", progress, downloaded, total)
                                setProgress(Data.Builder().putInt(KEY_PROGRESS, progress).build())
                                setForeground(createForegroundInfo(id, displayName, progress, total <= 0))
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
                notifyFinished(id, displayName, status == "completed")
                cancellationHandle.dispose()
                Result.success()
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { database.downloadDao().finishWithMessage(id, "cancelled", "下载已取消") }
            throw cancelled
        } catch (error: Throwable) {
            val cancelled = isStopped
            database.downloadDao().finishWithMessage(id, if (cancelled) "cancelled" else "failed", if (cancelled) "下载已取消" else (error.message ?: "未知错误"))
            if (!cancelled) notifyFinished(id, displayName, false)
            Result.failure()
        }
    }

    /**
     * 立即发出「准备中」通知，不依赖 WorkManager 前台服务的启动时机。
     * 与 setForeground 使用同一通知 ID，升级为前台服务时原地更新，不会出现两条通知。
     */
    private fun postPreparingNotification(id: String, fileName: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName.ifBlank { "正在下载" })
            .setContentText("正在连接服务器…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .setProgress(0, 0, true)
            .build()
        runCatching {
            applicationContext.getSystemService(NotificationManager::class.java)
                .notify(foregroundNotificationId(id), notification)
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
            .setContentIntent(openAppPendingIntent())
            .setProgress(100, progress, indeterminate)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(foregroundNotificationId(id), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(foregroundNotificationId(id), notification)
        }
    }

    /**
     * 前台通知 ID：必须为非负整数。
     * UUID 的 hashCode 为有符号整数，直接当通知 ID 可能得到负数，
     * 部分系统会直接丢弃这类通知（表现为下载完全没有通知）。
     */
    private fun foregroundNotificationId(id: String): Int = id.hashCode() and Int.MAX_VALUE

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
     * 完成通知 ID：与前台通知 ID 不同即可。
     * 前台 ID 已屏蔽符号位（and Int.MAX_VALUE），这里改用另一套映射，避免两者碰撞。
     */
    private fun finishedNotificationId(id: String): Int = (id.hashCode() xor Int.MIN_VALUE) and Int.MAX_VALUE

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
