/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.DownloadEntity
import java.util.UUID

class DownloadScheduler(private val context: Context) {
    private val database = BrowserDatabase.create(context)
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueue(url: String, target: Uri, fileName: String, expectedHash: String): String {
        runCatching {
            context.contentResolver.takePersistableUriPermission(target, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
        val request = OneTimeWorkRequestBuilder<BrowserDownloadWorker>()
            .setInputData(input)
            .addTag(id)
            .build()
        workManager.enqueue(request)
        return id
    }

    suspend fun cancel(id: String) {
        database.downloadDao().finishWithMessage(id, "cancelled", "下载已取消")
        workManager.cancelAllWorkByTag(id)
    }
}
