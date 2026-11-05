/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import com.zex.zexbrowse.data.SecurityScanner
import java.net.URI

/** Connection information belongs to a page load, not to the URL classifier. */
internal class ConnectionSecurityState {
    private data class Connection(
        val isSecure: Boolean,
        val mixedActive: Int,
        val mixedPassive: Int,
        val hasCertificate: Boolean,
        val contentLoaded: Int
    )

    private var url = ""
    private var connection: Connection? = null

    fun onPageStart(url: String): SecurityScanner.RiskAssessment {
        this.url = url
        connection = null
        return assess()
    }

    fun onLocationChange(url: String): SecurityScanner.RiskAssessment {
        val previousOrigin = origin(this.url)
        if (previousOrigin == null || previousOrigin != origin(url)) connection = null
        this.url = url
        return assess()
    }

    fun onSecurityChange(
        securityOrigin: String?,
        isSecure: Boolean,
        mixedActive: Int,
        mixedPassive: Int,
        hasCertificate: Boolean,
        contentLoaded: Int
    ): SecurityScanner.RiskAssessment {
        val pageOrigin = origin(url)
        connection = if (pageOrigin != null && pageOrigin == securityOrigin?.let(::origin)) {
            Connection(isSecure, mixedActive, mixedPassive, hasCertificate, contentLoaded)
        } else null
        return assess()
    }

    private fun assess(): SecurityScanner.RiskAssessment {
        val state = connection
        return SecurityScanner.assess(
            url = url,
            isSecure = state?.isSecure ?: false,
            mixedActive = state?.mixedActive ?: -1,
            mixedPassive = state?.mixedPassive ?: -1,
            hasCertificate = state?.hasCertificate ?: false,
            contentLoaded = state?.contentLoaded ?: -1
        )
    }

    // A conservative comparison: changes to authority discard the old connection state.
    private fun origin(url: String): Pair<String, String>? {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()
            val authority = uri.rawAuthority?.substringAfterLast('@')?.lowercase()
            if (scheme == null || scheme !in setOf("http", "https") || authority.isNullOrBlank()) return null
            val defaultPort = if (scheme == "https") ":443" else ":80"
            scheme to authority.removeSuffix(defaultPort)
        } catch (_: java.net.URISyntaxException) {
            null
        }
    }
}
