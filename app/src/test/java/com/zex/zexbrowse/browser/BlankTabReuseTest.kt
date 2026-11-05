/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlankTabReuseTest {
    private data class Tab(val url: String = "", val incognito: Boolean = true, val pending: Boolean = false)

    private fun reusableTab(tabs: List<Tab>, selected: Tab?): Tab? =
        selectedOrSingleReusableTab(tabs, selected) {
            canReuseBlankTab(it.url, it.incognito, requestedIncognito = true, hasPendingLoad = it.pending)
        }

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

    @Test fun selectedBlankPrivateTabTakesPrecedenceOverOtherBlanks() {
        val selected = Tab(url = "about:blank")
        val other = Tab()
        assertEquals(selected, reusableTab(listOf(other, selected), selected))
    }

    @Test fun ineligibleSelectionFallsBackOnlyToUniqueBlankPrivateTab() {
        val blank = Tab(url = "about:blank")
        val pending = Tab(pending = true)
        val normal = Tab(incognito = false)
        val loaded = Tab(url = "https://example.com")
        val tabs = listOf(blank, pending, normal, loaded)
        for (selected in listOf(pending, normal, loaded, null)) {
            assertEquals(blank, reusableTab(tabs, selected))
        }
        assertNull(reusableTab(listOf(pending, normal, loaded), pending))
        assertNull(reusableTab(listOf(blank, Tab(), pending), pending))
    }
}
