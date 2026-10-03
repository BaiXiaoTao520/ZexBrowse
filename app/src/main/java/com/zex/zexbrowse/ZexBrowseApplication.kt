/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.WebExtensionController

class ZexBrowseApplication : Application() {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build()) }

    fun ensureDarkExtension() {
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_dark/", "zex-dark@zexbrowse")
                .accept({ extension ->
                    // 旧版曾按开关禁用扩展，这里强制启用，避免回退后暗色样式不生效
                    runCatching {
                        runtime.webExtensionController
                            .enable(extension, WebExtensionController.EnableSource.USER)
                            .accept({ _ -> }, { _ -> })
                    }
                }, { _ -> })
        }
    }
}