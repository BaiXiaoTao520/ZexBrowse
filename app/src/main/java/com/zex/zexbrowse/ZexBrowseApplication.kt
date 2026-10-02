/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class ZexBrowseApplication : Application() {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build()) }

    fun ensureDarkExtension() {
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_dark/", "zex-dark@zexbrowse")
                .accept({ _ -> }, { _ -> })
        }
    }
}
