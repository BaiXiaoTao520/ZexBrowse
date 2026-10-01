/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class ZexBrowseApplication : Application() {
    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(this, GeckoRuntimeSettings.Builder().build()) }
}
