/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zex.zexbrowse.ui.ZexBrowseApp
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URLDecoder

// 外链诊断：保存最近一次外部跳转 Intent 的关键信息，供设置页排查「外链打不开」类问题
object ExternalLinkDiagnostics {
    val last = MutableStateFlow("")
}

class MainActivity : ComponentActivity() {
    // 外部应用跳转过来的链接。用带自增序号的 StateFlow 传递：
    // 即使两次收到完全相同的链接、或在极短时序内连续到达，也能被可靠消费，不会丢失。
    private val incomingUrl = MutableStateFlow<UriRequest?>(null)

    private data class UriRequest(val seq: Long, val url: String)

    private var uriSeq = 0L

    /** 应用语言在 Activity 创建前生效，避免首帧用错语言再跳变 */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 最早安装崩溃捕获（Application.onCreate 通常已装，这里幂等再装一次，确保万无一失）
        CrashReporter.install(this)
        // 上次启动如果在「启动阶段」就崩了（标记没有被成功启动清除），
        // 这次不再盲目再次启动（否则就是无限闪退），而是直接把上次的崩溃日志展示出来。
        if (CrashReporter.didCrashLastBoot(this)) {
            CrashReporter.markStarted(this) // 清除标记，下次重试正常启动
            showBootCrashScreen()
            return
        }
        // 整个启动流程加同步兜底：Compose 首帧组合是在 setContent 内同步执行的，
        // 一旦依赖不匹配/主题/资源等在组合期抛异常，会被这里捕获，
        // 转而显示可读的错误卡片（而不是直接退回桌面），同时把栈写入崩溃日志文件。
        try {
            enableEdgeToEdge()
            // 不受支持的系统：直接拦截，弹出不可取消的提示，仅能点「退出应用」后退出
            UnsupportedSystem.detect()?.let {
                showUnsupportedDialog(it)
                return
            }
            consumeViewIntent(intent)
            // 帧率优化：向系统请求设备支持的最高刷新率，让滚动/动画/页面合成以更高帧率运行。
            requestMaxRefreshRate()
            // 真正进入组合前才打启动标记：未受支持系统提前 return 时不会留下标记
            CrashReporter.markBoot(this)
            setContent {
                val request by incomingUrl.collectAsState()
                ZexBrowseApp(
                    incomingUrl = request?.url,
                    incomingUrlSeq = request?.seq ?: 0L,
                    onIncomingUrlHandled = { incomingUrl.value = null }
                )
            }
            (application as ZexBrowseApplication).ensureDarkExtension()
            (application as ZexBrowseApplication).ensureAdBlockExtension()
        } catch (t: Throwable) {
            // 兜底：记录完整栈并展示错误卡片，避免「打开即闪退」且看不到任何原因
            CrashReporter.record(this, t)
            CrashReporter.markStarted(this)
            showStartupCrash(t)
        }
    }

    /**
     * 「上次启动崩溃」界面：用最原始的 Android View 展示已保存的崩溃日志，
     * 提供一个「重试启动」按钮。这样即使首帧组合崩了，也能稳定把崩溃信息展示给用户，
     * 避免陷入「打开→闪退→打开→闪退」的死循环且什么也看不到。
     */
    private fun showBootCrashScreen() {
        val log = CrashReporter.readLast(this) ?: "（未捕获到详细栈信息，可能是 native 层崩溃）"
        val msg = buildString {
            append("ZexBrowse 上次启动时崩溃了\n\n")
            append("以下为上次的崩溃信息（可长按选中复制后反馈）：\n\n")
            append(log)
        }
        val textView = android.widget.TextView(this).apply {
            text = msg
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(48, 96, 48, 48)
            setTextColor(android.graphics.Color.DKGRAY)
        }
        val button = android.widget.Button(this).apply {
            text = "重试启动"
            setOnClickListener {
                CrashReporter.clear(this@MainActivity)
                recreate()
            }
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(
                android.widget.ScrollView(this@MainActivity).apply { addView(textView) },
                android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )
            addView(button)
        }
        setContentView(container)
    }

    /**
     * 启动阶段发生致命异常时的兜底界面。
     *
     * 刻意用最原始的 Android View（ScrollView + TextView）绘制，不依赖 Compose / Material，
     * 因为它本身可能就是崩溃来源。用户可长按选中复制完整堆栈发回，便于精准修复。
     */
    private fun showStartupCrash(t: Throwable) {
        val stack = java.io.StringWriter().also { t.printStackTrace(java.io.PrintWriter(it)) }.toString()
        val logPath = CrashReporter.crashFile(this).absolutePath
        val msg = buildString {
            append("ZexBrowse 启动失败\n\n")
            append("错误信息：\n").append(t.toString()).append("\n\n")
            append("已自动保存崩溃日志到：\n").append(logPath).append("\n\n")
            append("请长按选中以下内容复制后反馈：\n\n")
            append(stack)
        }
        val textView = android.widget.TextView(this).apply {
            text = msg
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(48, 96, 48, 96)
            setTextColor(android.graphics.Color.DKGRAY)
        }
        val scroll = android.widget.ScrollView(this).apply { addView(textView) }
        setContentView(scroll)
    }

    /**
     * 帧率优化：向系统请求设备支持的最高刷新率。
     *
     * GeckoView 使用 SurfaceView 后端，渲染跟随窗口刷新率；Compose 动画同理。
     * 不同厂商机型支持的最高刷新率不同（60/90/120/144/165Hz 等），
     * 这里遍历 Display 支持的所有模式，选刷新率最高的那个并应用给窗口，
     * 避免系统为了省电把应用锁在 60Hz。失败时静默回退，不影响启动。
     */
    private fun requestMaxRefreshRate() {
        runCatching {
            val wm = getSystemService(WindowManager::class.java) ?: return@runCatching
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay ?: return@runCatching
            val modes = display.supportedModes
            val best = modes.maxByOrNull { it.refreshRate } ?: return@runCatching
            val lp = window.attributes
            // 优先按 Display 模式设置，精确到具体分辨率/刷新率组合；不支持时再改刷新率字段
            lp.preferredDisplayModeId = best.modeId
            @Suppress("DEPRECATION")
            lp.preferredRefreshRate = best.refreshRate
            window.attributes = lp
        }
    }

    private fun showUnsupportedDialog(system: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("抱歉")
            .setMessage("很遗憾，ZexBrowse 已停止对 $system 设备的支持，无法继续为你提供服务。感谢你的理解。")
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
        val candidate = extractUrl(intent)
        Log.d(TAG, "consumeViewIntent action=${intent.action} data=${intent.data} extras=${intent.extras?.keySet()} extracted=$candidate")
        recordDiagnostics(intent, candidate)
        if (candidate == null) return
        incomingUrl.value = UriRequest(++uriSeq, candidate)
        // 消费后清掉 data，避免配置变更（如旋转）重建时重复打开
        intent.data = null
    }

    // 记录本次跳转的原始信息（需在清空 intent.data 之前调用），便于在设置中查看
    private fun recordDiagnostics(intent: Intent, extracted: String?) {
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val builder = StringBuilder()
        builder.append("时间：").append(time).append('\n')
        builder.append("action：").append(intent.action).append('\n')
        builder.append("data：").append(intent.data).append('\n')
        builder.append("extras：\n")
        intent.extras?.let { extras ->
            for (key in extras.keySet()) builder.append("  ").append(key).append(" = ").append(extras.get(key)).append('\n')
        }
        builder.append("提取结果：").append(extracted ?: "（未提取到网址）")
        ExternalLinkDiagnostics.last.value = builder.toString()
    }

    private fun extractUrl(intent: Intent): String? {
        // 统一的候选来源收集：data、各类文本 extra、clipData
        // 不局限于特定 action，只要其中含 http/https 链接就可打开（适配各应用差异）

        // 1) data：可能是直接的网址，也可能是携带网址的自定义 scheme（含 URL 编码）
        intent.data?.toString()?.let { raw ->
            normalizeScheme(raw)?.let { return it }
            extractHttpUrl(raw)?.let { return it }
            runCatching { extractHttpUrl(URLDecoder.decode(raw, "UTF-8")) }.getOrNull()?.let { return it }
        }

        // 2) 标准文本 extra（优先，避免被无关 extra 抢先）
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text -> extractHttpUrl(text)?.let { return it } }
        intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.firstNotNullOfOrNull { extractHttpUrl(it) }?.let { return it }
        intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let { text -> extractHttpUrl(text)?.let { return it } }

        // 3) 遍历其余全部 extra：部分应用把链接放在自定义 key 或 String 列表里
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                if (key == Intent.EXTRA_TEXT || key == Intent.EXTRA_HTML_TEXT) continue
                extractFromValue(extras.get(key))?.let { return it }
            }
        }

        // 4) clipData
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.toString()?.let { raw -> normalizeScheme(raw)?.let { return it } }
                clip.getItemAt(i).text?.toString()?.let { text -> extractHttpUrl(text)?.let { return it } }
            }
        }
        return null
    }

    private fun extractFromValue(value: Any?): String? = when (value) {
        is String -> extractHttpUrl(value)
        is CharSequence -> extractHttpUrl(value.toString())
        is ArrayList<*> -> value.firstNotNullOfOrNull { extractFromValue(it) }
        else -> null
    }

    // 补全缺失协议的域名（部分应用传 "www.example.com" 这类无 scheme 文本）
    private fun normalizeScheme(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.matches(Regex("^www\\..+"))) return "https://$trimmed"
        return null
    }

    private fun extractHttpUrl(text: String): String? {
        Regex("https?://[^\\s]+").find(text)?.value?.let { return normalizeScheme(it) }
        return normalizeScheme(text)
    }

    private companion object {
        const val TAG = "ZexIntent"
    }
}
