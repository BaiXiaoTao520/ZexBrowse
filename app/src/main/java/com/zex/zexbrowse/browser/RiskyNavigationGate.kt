/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.zex.zexbrowse.browser

import com.zex.zexbrowse.data.SecurityScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class RiskySiteWarning(val uri: String, val assessment: SecurityScanner.RiskAssessment)

/** Keeps a blocked request until its tab can display a decision to the user. */
internal class RiskyNavigationGate {
    private val allowed = HashSet<String>()
    private val _warnings = MutableStateFlow<Map<String, RiskySiteWarning>>(emptyMap())
    val warnings = _warnings.asStateFlow()

    fun shouldBlock(tabId: String, uri: String, userInitiated: Boolean, mode: String): Boolean {
        if (!userInitiated) return false
        val assessment = SecurityScanner.assessUrl(uri)
        val block = uri !in allowed && when (mode) {
            "basic" -> false
            "enhanced" -> assessment.isRisky
            else -> assessment.level == SecurityScanner.RiskLevel.DANGEROUS
        }
        if (block) {
            _warnings.value = _warnings.value + (tabId to RiskySiteWarning(uri, assessment))
        } else {
            dismiss(tabId)
        }
        return block
    }

    fun dismiss(tabId: String) { _warnings.value = _warnings.value - tabId }

    fun dismiss(tabId: String, warning: RiskySiteWarning) {
        if (_warnings.value[tabId] === warning) dismiss(tabId)
    }

    fun proceed(tabId: String, warning: RiskySiteWarning): String? {
        if (_warnings.value[tabId] !== warning) return null
        dismiss(tabId)
        allowed.add(warning.uri)
        return warning.uri
    }

    fun clearAllowances() { allowed.clear() }
}
