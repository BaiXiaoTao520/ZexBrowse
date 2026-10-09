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
    // 外部应用跳转过来的链接。用带自增序号的 StateFlow 传递：
    // 即使两次收到完全相同的链接、或在极短时序内连续到达，也能被可靠消费，不会丢失。
    private val incomingUrl = MutableStateFlow<UriRequest?>(null)

    private data class UriRequest(val seq: Long, val url: String)

    private var uriSeq = 0L

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
            val request by incomingUrl.collectAsState()
            ZexBrowseApp(
                incomingUrl = request?.url,
                incomingUrlSeq = request?.seq ?: 0L,
                onIncomingUrlHandled = { incomingUrl.value = null }
            )
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

    // 尽力从各种外链 Intent 中提取可打开的网址：
    // - ACTION_VIEW 的 data（标准浏览器/分享链路）
    // - ACTION_SEND / ACTION_SEND_MULTIPLE 的 EXTRA_TEXT / EXTRA_HTML_TEXT（QQ、微信等「分享到」/「用浏览器打开」）
    // - 部分应用会把链接放在 clipData 或其它 extra 中
    private fun consumeViewIntent(intent: Intent?) {
        if (intent == null) return
        val candidate = extractUrl(intent) ?: return
        incomingUrl.value = UriRequest(++uriSeq, candidate)
        // 消费后清掉 data，避免配置变更（如旋转）重建时重复打开
        intent.data = null
    }

    private fun extractUrl(intent: Intent): String? {
        // 统一的候选来源收集：data、各类文本 extra、clipData
        // 不局限于特定 action，只要其中含 http/https 链接就可打开（适配各应用差异）
        intent.data?.toString()?.let { raw -> normalizeScheme(raw)?.let { return it } }

        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text -> extractHttpUrl(text)?.let { return it } }
        intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let { text -> extractHttpUrl(text)?.let { return it } }
        intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.forEach { text ->
            extractHttpUrl(text)?.let { return it }
        }

        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.toString()?.let { raw -> normalizeScheme(raw)?.let { return it } }
                clip.getItemAt(i).text?.toString()?.let { text -> extractHttpUrl(text)?.let { return it } }
            }
        }
        return null
    }

    // 补全缺失协议的域名（部分应用传 "www.example.com" 这类无 scheme 文本）
    private fun normalizeScheme(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.matches(Regex("^www\\..+"))) return "https://$trimmed"
        return null
    }

    private fun extractHttpUrl(text: String): String? {
        val match = Regex("https?://[^\\s]+").find(text)?.value ?: return normalizeScheme(text)
        return normalizeScheme(match)
    }
}
