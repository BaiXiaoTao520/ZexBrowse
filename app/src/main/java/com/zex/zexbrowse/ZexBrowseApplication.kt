/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
import android.content.ComponentCallbacks2
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

class ZexBrowseApplication : Application() {
    // 崩溃日志捕获必须尽早安装：可覆盖后续 Activity/组合期抛出的未捕获异常
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }

    /**
     * 运行时级反跟踪类别：广告 / 分析 / 社交 / 挖矿。
     * 会话是否真正启用这些拦截取决于每个会话的 useTrackingProtection（由「广告拦截」开关控制），
     * 这里先把要拦的类别定下来，避免不同机型默认值不一致。
     */
    val runtime: GeckoRuntime by lazy {
        GeckoRuntime.create(
            this,
            GeckoRuntimeSettings.Builder()
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(
                            ContentBlocking.AntiTracking.AD or
                                ContentBlocking.AntiTracking.ANALYTIC or
                                ContentBlocking.AntiTracking.SOCIAL or
                                ContentBlocking.AntiTracking.CRYPTOMINING
                        )
                        // 开启拦截数据库：只有它开启，Gecko 才会把拦截事件落盘，
                        // 「广告拦截」页里的拦截次数才能从里面真实读出来，而不是编个数字。
                        .contentBlockingDatabase(true)
                        .build()
                )
                .build()
        )
    }

    /**
     * 注册内置的强制暗色扩展。
     *
     * 注意：这里只做「注册」，不主动启用。是否启用由用户在「主题及网页个性化 → 强制适配深色模式」
     * 中决定（见 [setForceDarkExtensionEnabled]）。此前实现无条件启用扩展，导致用户关闭开关后
     * 网页仍被强制暗化。
     */
    private var darkExtension: WebExtension? = null
    // 用户最近一次请求的强制暗化状态；null 表示尚未同步过，此时不主动启用扩展
    private var darkExtensionRequested: Boolean? = null

    fun ensureDarkExtension() {
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_dark/", "zex-dark@zexbrowse")
                .accept({ extension: WebExtension? ->
                    darkExtension = extension
                    // 扩展注册完成后，按最近一次请求的状态应用启用/停用；
                    // 若用户尚未操作过开关（darkExtensionRequested 为 null），则先不启用，
                    // 由界面层首次同步设置时决定，避免默认打开强制暗化。
                    darkExtensionRequested?.let { enabled -> runCatching { setForceDarkExtensionEnabled(enabled) } }
                }, { _: Throwable? -> })
        }
    }

    /**
     * 启用/停用内置暗色扩展。停用后网页完全按自身与系统偏好渲染；
     * 强制暗化仅影响网页内容，不改变应用自身的深浅色主题。
     */
    fun setForceDarkExtensionEnabled(enabled: Boolean) {
        darkExtensionRequested = enabled
        val extension = darkExtension ?: return
        runCatching {
            val controller = runtime.webExtensionController
            if (enabled) {
                controller.enable(extension, WebExtensionController.EnableSource.APP).accept({ _: WebExtension? -> }, { _: Throwable? -> })
            } else {
                controller.disable(extension, WebExtensionController.EnableSource.APP).accept({ _: WebExtension? -> }, { _: Throwable? -> })
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 内置广告隐藏扩展（zex_adblock）
    //
    // 与「追踪保护」互补：追踪保护负责在请求层拦截广告/追踪域名，
    // 本扩负责用 CSS 隐藏同一页面内联/自托管的常见广告位（追踪保护无法拦的内联广告）。
    // 同样只做注册，是否启用由「广告拦截」开关决定（见 setAdBlockExtensionEnabled）。
    // ────────────────────────────────────────────────────────────────
    private var adBlockExtension: WebExtension? = null
    private var adBlockExtensionRequested: Boolean? = null

    fun ensureAdBlockExtension() {
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_adblock/", "zex-adblock@zexbrowse")
                .accept({ extension: WebExtension? ->
                    adBlockExtension = extension
                    adBlockExtensionRequested?.let { enabled -> runCatching { setAdBlockExtensionEnabled(enabled) } }
                }, { _: Throwable? -> })
        }
    }

    fun setAdBlockExtensionEnabled(enabled: Boolean) {
        adBlockExtensionRequested = enabled
        val extension = adBlockExtension ?: return
        runCatching {
            val controller = runtime.webExtensionController
            if (enabled) {
                controller.enable(extension, WebExtensionController.EnableSource.APP).accept({ _: WebExtension? -> }, { _: Throwable? -> })
            } else {
                controller.disable(extension, WebExtensionController.EnableSource.APP).accept({ _: WebExtension? -> }, { _: Throwable? -> })
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 假冒站点网络层拦截扩展（zex_compliance）
    //
    // 为什么需要它：GeckoView 没有 "shouldInterceptRequest" 这类原生钩子，
    // NavigationDelegate 只在导航层生效——iframe、图片、脚本、XHR 这些资源请求
    // 根本不经过它。仅靠导航拦截，页面仍可能向假冒域名发出请求（白白跑流量）。
    // 因此用 WebExtension 的 declarativeNetRequest 在网络层直接阻断，
    // 规则见 assets/web_extensions/zex_compliance/rules.json。
    //
    // 已恢复对大陆服务：受限搜索引擎不再在网络层硬拦（否则二次确认的
    // 「继续访问」会永远加载不出来），这里只保留假冒站点的阻断。
    // 与广告拦截无关，始终启用，不提供开关。
    // ────────────────────────────────────────────────────────────────
    private var complianceExtension: WebExtension? = null

    fun ensureComplianceExtension(onReady: () -> Unit, onFailure: (Throwable) -> Unit) {
        fun fail(error: Throwable?) {
            val failure = error ?: IllegalStateException("Compliance extension unavailable")
            CrashReporter.record(this, failure)
            onFailure(failure)
        }
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_compliance/", "zex-compliance@zexbrowse")
                .accept({ extension: WebExtension? ->
                    if (extension == null) {
                        fail(null)
                    } else {
                        runCatching {
                            runtime.webExtensionController
                                .enable(extension, WebExtensionController.EnableSource.APP)
                                .accept({ enabled: WebExtension? ->
                                    if (enabled?.metaData?.enabled != true) fail(null) else {
                                        complianceExtension = enabled
                                        onReady()
                                    }
                                }, { error: Throwable? -> fail(error) })
                        }.onFailure { fail(it) }
                    }
                }, { error: Throwable? -> fail(error) })
        }.onFailure { fail(it) }
    }

    // ────────────────────────────────────────────────────────────────
    // 内存机制
    //
    // Android 的「公平」体现在：进程收到什么级别的内存压力、能用多少堆，
    // 都由系统按后台优先级统一调度，应用无法也不应自己抢占。
    // 这里做的是正确响应系统信号，而不是“抢内存”：
    // 1) largeHeap：浏览器存缓存/页面/图片，默认堆容易在低端机上先于系统其它进程被 OOM 杀掉；
    // 2) onTrimMemory：系统回收内存时同步释放非必需缓存，避免被杀；
    // 3) 通知 GeckoView 释放非活跃会话的内存，网页内核占大头。
    // ────────────────────────────────────────────────────────────────

    /** 内存压力回调：由 Activity 在 onTrimMemory / onLowMemory 时转发 */
    var onMemoryPressure: ((level: Int) -> Unit)? = null

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        runCatching { onMemoryPressure?.invoke(level) }
        // 严重级别时清理可重建缓存；下载捕获目录由下载 Worker 使用后清理。
        // 不能动 filesDir 下的已下载文件——那是用户资产。
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            runCatching {
                cacheDir?.listFiles()?.filter { it.name != "download-capture" }
                    ?.forEach { it.deleteRecursively() }
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching { onMemoryPressure?.invoke(ComponentCallbacks2.TRIM_MEMORY_COMPLETE) }
    }

    /**
     * 让 GeckoView 释放非活跃会话的内存。
     *
     * 内核（GeckoView）占的内存远大于应用自身 Java 堆，低内存时优先动它才有效；
     * 但只能动非活跃会话：清掉当前正在看的页面会直接白屏。
     *
     * @param activeSessionId 当前活动标签的 id，保留它不清
     * @param allSessions 全部会话（标签 id -> GeckoSession）
     */
    fun trimGeckoInactiveSessions(
        activeSessionId: String?,
        allSessions: List<Pair<String, org.mozilla.geckoview.GeckoSession>>
    ) {
        runCatching {
            allSessions.forEach { (id, session) ->
                if (id != activeSessionId) {
                    // purgeHistory 清理后台会话的历史记录与部分缓存，属于可重建数据
                    session.purgeHistory()
                }
            }
        }
    }
}
