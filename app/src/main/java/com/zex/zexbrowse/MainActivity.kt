/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zex.zexbrowse.ui.ZexBrowseApp

class MainActivity : ComponentActivity() {
    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUrl = extractUrl(intent)
        // 消费后清掉链接数据，避免配置变更（如旋转）重建时重复打开
        intent?.data = null
        setContent {
            ZexBrowseApp(incomingUrl = pendingUrl, onIncomingUrlHandled = { pendingUrl = null })
        }
        (application as ZexBrowseApplication).ensureDarkExtension()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractUrl(intent)?.let { pendingUrl = it }
        intent.data = null
    }

    // 仅接收 http/https 外链（用户把本应用设为默认浏览器或从其它应用「用浏览器打开」时触发）
    private fun extractUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return uri.toString().takeIf { uri.scheme == "http" || uri.scheme == "https" }
    }
}
