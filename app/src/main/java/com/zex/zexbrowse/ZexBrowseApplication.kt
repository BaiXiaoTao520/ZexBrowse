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

    private var darkExtension: WebExtension? = null
    private var darkExtensionDesired = false
    private var darkExtensionReady = false

    fun ensureDarkExtension() {
        runCatching {
            runtime.webExtensionController
                .ensureBuiltIn("resource://android/assets/web_extensions/zex_dark/", "zex-dark@zexbrowse")
                .accept({ extension ->
                    darkExtension = extension
                    darkExtensionReady = true
                    applyDarkExtensionState()
                }, { _ -> })
        }
    }

    fun setDarkExtensionEnabled(enabled: Boolean, onApplied: () -> Unit = {}) {
        darkExtensionDesired = enabled
        if (!darkExtensionReady) {
            onApplied()
            return
        }
        applyDarkExtensionState(onApplied)
    }

    private fun applyDarkExtensionState(onApplied: () -> Unit = {}) {
        val extension = darkExtension ?: run { onApplied(); return }
        runCatching {
            val result = if (darkExtensionDesired) {
                runtime.webExtensionController.enable(extension, WebExtensionController.EnableSource.USER)
            } else {
                runtime.webExtensionController.disable(extension, WebExtensionController.EnableSource.USER)
            }
            result.accept({ _ -> onApplied() }, { _ -> onApplied() })
        }.onFailure { onApplied() }
    }
}
