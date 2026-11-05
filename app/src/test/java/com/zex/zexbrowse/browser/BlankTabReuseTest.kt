/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankTabReuseTest {
    @Test fun onlyBlankTabsInTheRequestedModeAreReusable() {
        for (privateMode in listOf(false, true)) {
            assertTrue(canReuseBlankTab("", privateMode, privateMode, false))
            assertTrue(canReuseBlankTab("about:blank", privateMode, privateMode, false))
            assertFalse(canReuseBlankTab("https://example.com", privateMode, privateMode, false))
            assertFalse(canReuseBlankTab("", !privateMode, privateMode, false))
            assertFalse(canReuseBlankTab("", privateMode, privateMode, true))
            assertFalse(canReuseBlankTab("about:blank", privateMode, privateMode, true))
        }
    }
}
