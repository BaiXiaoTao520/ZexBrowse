/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zex.zexbrowse.R
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DownloadScheduler(private val context: Context) {
    private val database = BrowserDatabase.create(context)
    private val workManager = WorkManager.getInstance(context)

    /**
     * 入队一次下载。
     *
     * 整个流程必须跑在 IO 线程：其中包含文件名网络探测、目标文件创建、
     * Room 写入与 WorkManager 入队，放在调用方（如界面层的 Main 协程）执行
     * 会卡住主线程，表现为“点下载后界面顿一下”。
     */
    suspend fun enqueue(url: String, fileName: String, expectedHash: String, useExternalDirectory: Boolean, externalTreeUri: String, localSourceBin: String? = null): String = withContext(Dispatchers.IO) {
        // 创建目标文件之前必须先确定真实文件名：
        // OpenList/Alist、SharePoint 等站点的下载入口常为 download.aspx，
        // 末段并非真实文件名。若先用它建文件，之后再改就晚了（文件已落盘）。
        // 界面层已解析过时这里不会重复请求（prefer 会保留非通用名）。
        // 【关键】若有本地捕获文件，绝不能再去 resolve(url) 发网络探测：
        // 那会对一次性令牌地址发生二次请求→ 403。直接沿用传入的已知名字。
        val finalName = if (localSourceBin != null) {
            fileName.ifBlank { "download" }
        } else if (DownloadFileName.isGeneric(fileName)) {
            DownloadFileName.prefer(fileName, DownloadFileName.resolve(url))
        } else {
            fileName
        }
        val target = createTargetUri(finalName, useExternalDirectory, externalTreeUri)
        val id = UUID.randomUUID().toString()
        database.downloadDao().save(
            DownloadEntity(
                id = id,
                fileName = finalName,
                sourceUrl = url,
                targetUri = target.toString(),
                expectedSha256 = expectedHash.trim()
            )
        )
        val input = Data.Builder()
            .putString(BrowserDownloadWorker.KEY_ID, id)
            .putString(BrowserDownloadWorker.KEY_URL, url)
            .putString(BrowserDownloadWorker.KEY_URI, target.toString())
            .putString(BrowserDownloadWorker.KEY_FILE_NAME, finalName)
            .putString(BrowserDownloadWorker.KEY_EXPECTED_HASH, expectedHash.trim())
            .putString(BrowserDownloadWorker.KEY_LOCAL_SOURCE, localSourceBin)
            .build()
        workManager.enqueue(OneTimeWorkRequestBuilder<BrowserDownloadWorker>().setInputData(input).addTag(id).build())
        return@withContext id
    }

    /**
     * 暂停：先把状态置为 paused（Worker 据此与「取消」区分），
     * 再取消任务。Worker 收到停止信号后会保留已下载字节数与文件，供后续续传。
     */
    suspend fun pause(id: String) {
        database.downloadDao().pause(id)
        workManager.cancelAllWorkByTag(id)
    }

    /**
     * 继续：带已有字节数与 ETag 重新入队，Worker 以 Range 请求从断点接着下。
     * 无法读取记录时（已被删除）直接返回 false，由界面提示用户重新下载。
     */
    suspend fun resume(id: String): Boolean = withContext(Dispatchers.IO) {
        val record = database.downloadDao().find(id) ?: return@withContext false
        val target = Uri.parse(record.targetUri)
        // SAF 无法可靠随机写入，只能从头重下；此时不能把旧字节数当作续传起点
        val offset = if (target.scheme == "file") record.downloadedBytes.coerceAtLeast(0L) else 0L
        database.downloadDao().resume(id, offset, record.totalBytes, record.etag)
        val input = Data.Builder()
            .putString(BrowserDownloadWorker.KEY_ID, id)
            .putString(BrowserDownloadWorker.KEY_URL, record.sourceUrl)
            .putString(BrowserDownloadWorker.KEY_URI, record.targetUri)
            .putString(BrowserDownloadWorker.KEY_FILE_NAME, record.fileName)
            .putString(BrowserDownloadWorker.KEY_EXPECTED_HASH, record.expectedSha256)
            .build()
        workManager.enqueue(OneTimeWorkRequestBuilder<BrowserDownloadWorker>().setInputData(input).addTag(id).build())
        true
    }

    /**
     * 取消：先标记取消，再取消任务并删除已写入的数据。
     * Worker 收到停止信号时若看到状态不是 paused，会按取消处理。
     */
    suspend fun cancel(id: String) {
        val target = runCatching { database.downloadDao().find(id)?.targetUri?.let(Uri::parse) }.getOrNull()
        database.downloadDao().finishWithMessage(id, "cancelled", context.getString(R.string.dl_err_cancelled))
        workManager.cancelAllWorkByTag(id)
        if (target != null) withContext(Dispatchers.IO) { deleteTarget(target) }
    }

    suspend fun delete(id: String, uri: String, deleteFile: Boolean) {
        if (deleteFile) withContext(Dispatchers.IO) { deleteTarget(Uri.parse(uri)) }
        database.downloadDao().delete(id)
    }

    /** 删除已写入的目标文件（内部 file:// 与外部 SAF 均支持） */
    private fun deleteTarget(target: Uri) = runCatching {
        if (target.scheme == "file") {
            File(target.path.orEmpty()).delete()
        } else {
            try {
                DocumentsContract.deleteDocument(context.contentResolver, target)
            } catch (_: Throwable) {
                context.contentResolver.delete(target, null, null)
            }
        }
    }

    private fun createTargetUri(fileName: String, useExternalDirectory: Boolean, externalTreeUri: String): Uri {
        if (useExternalDirectory) {
            val treeUri = Uri.parse(externalTreeUri)
            val directory = DocumentFile.fromTreeUri(context, treeUri) ?: error(context.getString(R.string.dl_err_dir_unavailable))
            val existing = directory.findFile(fileName)
            val target = existing ?: directory.createFile(mimeType(fileName), fileName) ?: error(context.getString(R.string.dl_err_cannot_create_ext))
            return target.uri
        }
        val directory = File(context.filesDir, "downloads")
        check(directory.exists() || directory.mkdirs()) { context.getString(R.string.dl_err_cannot_create_int) }
        return Uri.fromFile(File(directory, fileName))
    }

    private fun mimeType(fileName: String): String = when {
        fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
        fileName.endsWith(".pdf", true) -> "application/pdf"
        fileName.endsWith(".zip", true) -> "application/zip"
        else -> "application/octet-stream"
    }
}
