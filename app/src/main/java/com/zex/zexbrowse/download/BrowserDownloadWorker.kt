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
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.zex.zexbrowse.data.BrowserDatabase
import kotlinx.coroutines.CancellationException
import com.zex.zexbrowse.BuildConfig
import com.zex.zexbrowse.R
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
        ?: BuildConfig.GECKOVIEW_VERSION
    "Mozilla/5.0 (Android 14; Mobile; rv:$version) Gecko/20100101 Firefox/$version"
}

class BrowserDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val database = BrowserDatabase.create(appContext)
    private val client = OkHttpClient()

    /** 任务被主动停下：是暂停还是取消，由数据库里的状态标记决定 */
    private class PauseOrCancel : Exception()

    /** 界面层点「暂停」时会先把记录置为 paused， Worker 据此区分暂停与取消 */
    private suspend fun recordIsPauseRequested(id: String): Boolean =
        database.downloadDao().find(id)?.status == "paused"

    /**
     * 暂停时把真实进度写回记录，否则续传会从 0 重新开始。
     *
     * 优先采用目标文件在磁盘上的实际长度：写入缓冲区可能还没刷盘，
     * 用内存里的 downloaded 会比文件多，重启后发现前半段不够就会被迫重下。
     */
    private suspend fun savePausedProgress(id: String, downloaded: Long, total: Long, uri: Uri) {
        val onDisk = runCatching { if (uri.scheme == "file") File(uri.path.orEmpty()).length() else targetSize(uri) }
            .getOrDefault(0L)
        val committed = if (onDisk > 0L) minOf(onDisk, downloaded) else downloaded
        val progress = if (total > 0) ((committed * 100) / total).toInt().coerceIn(0, 100) else 0
        database.downloadDao().updateProgress(id, "paused", progress, committed, total)
        database.downloadDao().pause(id)
    }

    /** 取消时删掉已写入的半成品文件，避免留下残包 */
    private suspend fun deletePartialFile(uri: Uri) {
        runCatching {
            if (uri.scheme == "file") {
                File(uri.path.orEmpty()).delete()
            } else {
                android.provider.DocumentsContract.deleteDocument(applicationContext.contentResolver, uri)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_ID) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return@withContext Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME).orEmpty()
        // 下载过程中可能被响应头纠正为真实文件名，后续通知与记录都用这个变量
        var displayName = fileName
        val expectedHash = inputData.getString(KEY_EXPECTED_HASH).orEmpty()
        // 本地捕获文件：onExternalResponse 时 Gecko 已把响应体拉下来存到临时文件。
        // 存在且可用时直接读它落盘，全程不再发第二次 HTTP 请求（一次性令牌二次请求会 403）。
        val localBin = inputData.getString(KEY_LOCAL_SOURCE)?.let(::File)?.takeIf { it.exists() }
        // 先用 NotificationManager 直接发出「准备中」通知：
        // setForeground 走 WorkManager 的前台服务启动流程（Android 12+ 异步），
        // 紧接着的阻塞网络请求会让通知迟迟不出现，用户以为没反应。
        // 直发通知即时可见，随后 setForeground 升级为前台服务（同一通知 ID，不闪重复）。
        postPreparingNotification(id, fileName)
        setForeground(createForegroundInfo(id, fileName, 0, true))

        var input: java.io.InputStream? = null
        var httpResponse: okhttp3.Response? = null
        var call: okhttp3.Call? = null
        var cancellationHandle: kotlinx.coroutines.DisposableHandle? = null
        // 声明在 try 之外：暂停时要把它写回记录，否则续传会从 0 重新开始
        var downloadedBytes = 0L
        var totalBytes = -1L
        try {
            var total = -1L
            // 续传起点：上次暂停/中断时已经落盘的字节数。
            // 只有服务端明确支持 Range 并返回 206 时才会从这里开始，
            // 否则（返回 200）必须从头写，否则会拼出损坏文件。
            val record = database.downloadDao().find(id)
            var startOffset = record?.downloadedBytes?.coerceAtLeast(0L) ?: 0L
            val ifRange = record?.etag.orEmpty()
            if (localBin != null) {
                // 等待 capture 完成（done/fail 标记）；capture 是异步的，可能在 Worker 启动时还没读完。
                // fail 一出现就直接跳出，不再把剩下的超时时间白等完（否则界面会停在「正在下载」不动）。
                val base = localBin.name.removeSuffix(".bin")
                val doneFile = File(localBin.parentFile, "$base.done")
                val failFile = File(localBin.parentFile, "$base.fail")
                val deadline = System.currentTimeMillis() + LOCAL_WAIT_TIMEOUT_MS
                while (System.currentTimeMillis() < deadline && !doneFile.exists() && !failFile.exists()) {
                    if (isStopped) throw PauseOrCancel()
                    Thread.sleep(150)
                }
                if (doneFile.exists()) {
                    input = localBin.inputStream()
                    total = localBin.length()
                    totalBytes = total
                    // 本地捕获文件是完整文件，不能与历史偏移叠加
                    startOffset = 0L
                } else if (failFile.exists()) {
                    // 捕获失败（例如拿到的是验证页而不是文件）：立即回退网络下载，不再等超时
                    android.util.Log.d("ZexDownload", "capture failed, fallback to network: $url")
                }
            }

            // 无本地捕获文件（普通下载页面直接给 URL、或 capture 失败/超时）：回退到网络下载
            if (input == null) {
                val requestBuilder = Request.Builder().url(url).header("User-Agent", DOWNLOAD_USER_AGENT)
                if (startOffset > 0L) {
                    requestBuilder.header("Range", "bytes=$startOffset-")
                    if (ifRange.isNotBlank()) requestBuilder.header("If-Range", ifRange)
                }
                val c = client.newCall(requestBuilder.build())
                call = c
                cancellationHandle = kotlin.coroutines.coroutineContext.job.invokeOnCompletion {
                    if (it is CancellationException) c.cancel()
                }
                val response = c.execute()
                if (!response.isSuccessful) { response.close(); error(applicationContext.getString(R.string.dl_err_http, response.code)) }
                // 兜底：若入队时仍未拿到真实文件名，用本次下载的响应头纠正，
                // 保证「显示的文件名」与「实际落盘文件名」一致。
                if (DownloadFileName.isGeneric(displayName)) {
                    val fromHeader = DownloadFileName.fromContentDisposition(response.header("Content-Disposition"))
                    if (fromHeader != null && !DownloadFileName.isGeneric(fromHeader)) {
                        displayName = fromHeader
                        database.downloadDao().rename(id, displayName)
                        setForeground(createForegroundInfo(id, displayName, 0, true))
                    }
                }
                val body = response.body ?: run { response.close(); error(applicationContext.getString(R.string.dl_err_empty_body)) }
                httpResponse = response
                input = body.byteStream()
                // 206 = 服务端同意从 startOffset 继续；200 = 只能从头给，必须舍弃历史偏移
                val partial = response.code == 206
                if (!partial) startOffset = 0L
                val length = body.contentLength()
                total = if (length > 0) length + startOffset else -1L
                totalBytes = total
                // 存下 ETag，下次续传时用 If-Range 校验，避免服务器换了文件还硬拼
                val etag = response.header("ETag").orEmpty()
                if (etag.isNotBlank() && etag != record?.etag) {
                    database.downloadDao().updateEtag(id, etag)
                }
            }

            var downloaded = startOffset
            downloadedBytes = startOffset
            var lastProgress = -1
            var lastNotifyAt = 0L
            val digest = MessageDigest.getInstance("SHA-256")
            val targetFile = File(uri.path.orEmpty())
            val isFileTarget = uri.scheme == "file"
            val existing = if (isFileTarget) targetFile.length() else targetSize(uri)
            // 只有磁盘上确实已经有 startOffset 这么多数据，追加写才是安全的
            val appendable = startOffset > 0L && existing >= startOffset
            if (startOffset > 0L && !appendable) {
                // 服务端已按 Range 只发了剩余部分，本地却凑不齐前半段，
                // 无论从头写还是追加都会得到坏文件，只能让用户重新下载
                error(applicationContext.getString(R.string.dl_err_incomplete))
            }
            // 续传时已落盘的那部分是文件的前半段，必须先喂给摘要，
            // 否则最终 SHA-256 只算了后半段，校验必定报不匹配。
            if (appendable) {
                openTargetInput(uri).use { existingStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var remaining = startOffset
                    while (remaining > 0) {
                        val read = existingStream.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
            // 内部 file:// 用追加模式；外部 SAF 用 "wa"（write + append），
            // 两者都只在有可信偏移时追加，否则截断重写
            val output: OutputStream = if (isFileTarget) {
                FileOutputStream(targetFile, appendable)
            } else {
                val mode = if (appendable) "wa" else "wt"
                applicationContext.contentResolver.openOutputStream(uri, mode)
                    ?: error(applicationContext.getString(R.string.dl_err_cannot_write))
            }
            input!!.use { streamIn ->
                output.buffered().use { stream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        if (isStopped) {
                            call?.cancel()
                            throw PauseOrCancel()
                        }
                        val count = streamIn.read(buffer)
                        if (count < 0) break
                        stream.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        downloadedBytes = downloaded
                        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        // 百分比变化或距上次刷新超过 500ms 时刷新：
                        // 只按百分比会让小文件从 0% 直接跳到 100%、通知永远看不到中间进度
                        val now = System.currentTimeMillis()
                        if (progress != lastProgress || now - lastNotifyAt >= 500L) {
                            lastProgress = progress
                            lastNotifyAt = now
                            // 已经请求暂停时不再把状态写回 downloading，否则界面会闪回「正在下载」
                            if (!isStopped) {
                                database.downloadDao().updateProgress(id, "downloading", progress, downloaded, total)
                                setProgress(Data.Builder().putInt(KEY_PROGRESS, progress).build())
                                setForeground(createForegroundInfo(id, displayName, progress, total <= 0))
                            }
                        }
                    }
                }
            }
            if (isStopped) {
                call?.cancel()
                throw PauseOrCancel()
            }
            val hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            val status = if (expectedHash.isNotBlank() && !hash.equals(expectedHash, true)) "hash_mismatch" else "completed"
            database.downloadDao().complete(id, status, hash)
            notifyFinished(id, displayName, status == "completed")
            Result.success()
        } catch (stopped: PauseOrCancel) {
            // 暂停与取消都表现为任务被停下，区别在于记录状态：
            // 暂停保留已下载字节数以便续传，取消则连同文件一起清掉。
            withContext(NonCancellable) {
                if (recordIsPauseRequested(id)) {
                    // 先把真实进度写回，否则续传会从 0 开始
                    savePausedProgress(id, downloadedBytes, totalBytes, uri)
                } else {
                    database.downloadDao().finishWithMessage(id, "cancelled", applicationContext.getString(R.string.dl_err_cancelled))
                    deletePartialFile(uri)
                }
            }
            return@withContext Result.success()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                if (recordIsPauseRequested(id)) {
                    savePausedProgress(id, downloadedBytes, totalBytes, uri)
                } else {
                    database.downloadDao().finishWithMessage(id, "cancelled", applicationContext.getString(R.string.dl_err_cancelled))
                    deletePartialFile(uri)
                }
            }
            throw cancelled
        } catch (error: Throwable) {
            val cancelled = isStopped
            database.downloadDao().finishWithMessage(id, if (cancelled) "cancelled" else "failed", if (cancelled) applicationContext.getString(R.string.dl_err_cancelled) else (error.message ?: applicationContext.getString(R.string.dl_err_unknown)))
            if (!cancelled) notifyFinished(id, displayName, false)
            Result.failure()
        } finally {
            runCatching { input?.close() }
            runCatching { httpResponse?.close() }
            runCatching { cancellationHandle?.dispose() }
            // 清掉本地临时捕获文件（无论成功失败都不再需要）
            if (localBin != null) {
                val base = localBin.name.removeSuffix(".bin")
                runCatching { localBin.delete() }
                runCatching { File(localBin.parentFile, "$base.done").delete() }
                runCatching { File(localBin.parentFile, "$base.fail").delete() }
            }
        }
    }

    /** 目标文件当前实际大小（SAF 通过文件描述符读取），读不到按 0 处理 */
    private fun targetSize(uri: Uri): Long = runCatching {
        applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    }.getOrDefault(0L)

    /** 打开目标文件用于计算续传时的 SHA-256；SAF 走 contentResolver */
    private fun openTargetInput(uri: Uri): java.io.InputStream =
        if (uri.scheme == "file") {
            File(uri.path.orEmpty()).inputStream()
        } else {
            applicationContext.contentResolver.openInputStream(uri)
                ?: error(applicationContext.getString(R.string.dl_err_cannot_read))
        }

    /**
     * 立即发出「准备中」通知，不依赖 WorkManager 前台服务的启动时机。
     * 与 setForeground 使用同一通知 ID，升级为前台服务时原地更新，不会出现两条通知。
     */
    private fun postPreparingNotification(id: String, fileName: String) {
        ensureChannel()
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName.ifBlank { applicationContext.getString(R.string.dl_notify_preparing) })
            .setContentText(applicationContext.getString(R.string.dl_notify_connecting))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .setProgress(0, 0, true)
        // 与后续进度通知保持同一形态，避免刚升格为实时活动又退回普通通知造成闪动
        applyLiveUpdate(builder, 0, indeterminate = true)
        val notification = builder.build()
        runCatching {
            applicationContext.getSystemService(NotificationManager::class.java)
                .notify(foregroundNotificationId(id), notification)
        }
    }

    private fun createForegroundInfo(id: String, fileName: String, progress: Int, indeterminate: Boolean): ForegroundInfo {
        ensureChannel()
        val title = fileName.ifBlank { applicationContext.getString(R.string.dl_notify_preparing) }
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (indeterminate) applicationContext.getString(R.string.dl_notify_preparing) else "$progress%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .setProgress(100, progress, indeterminate)
        applyLiveUpdate(builder, progress, indeterminate)
        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(foregroundNotificationId(id), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(foregroundNotificationId(id), notification)
        }
    }

    /**
     * 把下载通知升级为 Android 16+ 的实时活动（Live Update）。
     *
     * 真实生效的前提（缺一不可，系统会自行逐条校验）：
     * - 通知必须是 ProgressStyle / BigTextStyle 等受支持样式，且 ongoing、有 contentTitle、
     *   没有自定义 RemoteViews、渠道重要度不是 IMPORTANCE_MIN（本渠道是 IMPORTANCE_LOW，满足）；
     * - 必须声明 POST_PROMOTED_NOTIFICATIONS 并在构建时请求升格；
     * - 用户没有在系统设置里关闭本应用的实时活动（canPostPromotedNotifications 会返回 false）。
     *
     * androidx.core 1.17.0 起 NotificationCompat 自身就带 ProgressStyle 与
     * setRequestPromotedOngoing，低版本系统上这两者自动降级为普通进度条，
     * 所以不需要按 Build.VERSION 分支写两套。
     */
    private fun applyLiveUpdate(builder: NotificationCompat.Builder, progress: Int, indeterminate: Boolean) {
        runCatching {
            // indeterminate 时进度相关字段会被系统忽略，只保留「正在准备」的转动状态；
            // 明确进度时再写入具体百分比，两者不能同时设置，否则进度条会停在 0
            val style = NotificationCompat.ProgressStyle().setStyledByProgress(true)
            if (indeterminate) {
                style.setProgressIndeterminate(true)
            } else {
                style.setProgress(progress.coerceIn(0, 100))
            }
            builder.setStyle(style)
            // 用户可在系统设置里关闭本应用的实时活动；此时为 false，通知退回普通样式即可
            if (NotificationManagerCompat.from(applicationContext).canPostPromotedNotifications()) {
                builder.setRequestPromotedOngoing(true)
            }
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
            .setContentTitle(fileName.ifBlank { applicationContext.getString(R.string.dl_notify_task) })
            .setContentText(applicationContext.getString(if (success) R.string.dl_notify_done_tap else R.string.dl_notify_failed))
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
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.dl_channel_name), NotificationManager.IMPORTANCE_LOW))
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
        manager.createNotificationChannel(NotificationChannel(FINISHED_CHANNEL_ID, applicationContext.getString(R.string.dl_channel_done), NotificationManager.IMPORTANCE_DEFAULT))
        finishedChannelReady = true
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_URI = "uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EXPECTED_HASH = "expected_hash"
        const val KEY_PROGRESS = "progress"
        // 本地已捕获的响应体临时文件路径（onExternalResponse 当场读下来的那份数据）
        const val KEY_LOCAL_SOURCE = "local_source"
        // 等待捕获完成的超时：capture 为异步，可能晚于 Worker 启动；超时则回退网络下载
        private const val LOCAL_WAIT_TIMEOUT_MS = 120_000L
        private const val CHANNEL_ID = "browser_downloads"
        private const val FINISHED_CHANNEL_ID = "browser_downloads_done"
    }
}
