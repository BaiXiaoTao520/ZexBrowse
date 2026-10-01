/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.DownloadEntity
import java.io.File
import java.util.UUID

class DownloadScheduler(private val context: Context) {
    private val database = BrowserDatabase.create(context)
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueue(url: String, fileName: String, expectedHash: String, useExternalDirectory: Boolean, externalTreeUri: String): String {
        val target = createTargetUri(fileName, useExternalDirectory, externalTreeUri)
        val id = UUID.randomUUID().toString()
        database.downloadDao().save(
            DownloadEntity(
                id = id,
                fileName = fileName,
                sourceUrl = url,
                targetUri = target.toString(),
                expectedSha256 = expectedHash.trim()
            )
        )
        val input = Data.Builder()
            .putString(BrowserDownloadWorker.KEY_ID, id)
            .putString(BrowserDownloadWorker.KEY_URL, url)
            .putString(BrowserDownloadWorker.KEY_URI, target.toString())
            .putString(BrowserDownloadWorker.KEY_FILE_NAME, fileName)
            .putString(BrowserDownloadWorker.KEY_EXPECTED_HASH, expectedHash.trim())
            .build()
        workManager.enqueue(OneTimeWorkRequestBuilder<BrowserDownloadWorker>().setInputData(input).addTag(id).build())
        return id
    }

    suspend fun cancel(id: String) {
        database.downloadDao().finishWithMessage(id, "cancelled", "下载已取消")
        workManager.cancelAllWorkByTag(id)
    }

    suspend fun delete(id: String, uri: String, deleteFile: Boolean) {
        if (deleteFile) runCatching {
            val target = Uri.parse(uri)
            if (target.scheme == "file") File(target.path.orEmpty()).delete() else context.contentResolver.delete(target, null, null)
        }
        database.downloadDao().delete(id)
    }

    private fun createTargetUri(fileName: String, useExternalDirectory: Boolean, externalTreeUri: String): Uri {
        if (useExternalDirectory) {
            val treeUri = Uri.parse(externalTreeUri)
            val directory = DocumentFile.fromTreeUri(context, treeUri) ?: error("外部下载目录不可用")
            val existing = directory.findFile(fileName)
            val target = existing ?: directory.createFile(mimeType(fileName), fileName) ?: error("无法在外部目录创建文件")
            return target.uri
        }
        val directory = File(context.filesDir, "downloads")
        check(directory.exists() || directory.mkdirs()) { "无法创建内部下载目录" }
        return Uri.fromFile(File(directory, fileName))
    }

    private fun mimeType(fileName: String): String = when {
        fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
        fileName.endsWith(".pdf", true) -> "application/pdf"
        fileName.endsWith(".zip", true) -> "application/zip"
        else -> "application/octet-stream"
    }
}
