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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zex.zexbrowse.ui.ZexBrowseApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    // 外部应用跳转过来的链接；用 StateFlow 传递，冷启动与已运行（onNewIntent）两种时序都能可靠送达
    private val incomingUrl = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 不受支持的系统：直接拦截，弹出不可取消的提示，仅能点「退出应用」后退出
        UnsupportedSystem.detect()?.let { systemName ->
            showUnsupportedDialog(systemName)
            return
        }
        consumeViewIntent(intent)
        setContent {
            val url by incomingUrl.collectAsState()
            ZexBrowseApp(incomingUrl = url, onIncomingUrlHandled = { incomingUrl.value = null })
        }
        (application as ZexBrowseApplication).ensureDarkExtension()
    }

    private fun showUnsupportedDialog(systemName: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("抱歉，暂不支持此系统")
            .setMessage("检测到你的设备运行的是 $systemName 系统。由于该系统与当前版本的 ZexBrowse 存在兼容性问题，我们已停止对其提供支持。感谢你的理解。")
            .setCancelable(false)
            .setPositiveButton("退出应用") { _, _ -> finish() }
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeViewIntent(intent)
    }

    // 仅接收 http/https 外链（用户把本应用设为默认浏览器或从其它应用「用浏览器打开」时触发）
    private fun consumeViewIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val candidate = intent.data?.toString()?.takeIf(::isHttpUrl)
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::extractHttpUrl)
            ?: return
        incomingUrl.value = candidate
        // 消费后清掉 data，避免配置变更（如旋转）重建时重复打开
        intent.data = null
    }

    private fun isHttpUrl(value: String): Boolean = value.startsWith("http://") || value.startsWith("https://")

    private fun extractHttpUrl(text: String): String? = Regex("https?://\\S+").find(text)?.value
}
