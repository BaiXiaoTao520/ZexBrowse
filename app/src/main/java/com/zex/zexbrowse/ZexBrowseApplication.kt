/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
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
}
