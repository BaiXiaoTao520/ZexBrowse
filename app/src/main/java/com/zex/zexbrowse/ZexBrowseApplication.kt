/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

class ZexBrowseApplication : Application() {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build()) }

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
}
