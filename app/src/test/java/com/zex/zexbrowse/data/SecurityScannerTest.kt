package com.zex.zexbrowse.data

import com.zex.zexbrowse.R
import com.zex.zexbrowse.data.SecurityScanner.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityScannerTest {
    @Test fun backslashAndFilteredCharactersDoNotImpersonateAnAllowlistedHost() {
        for (url in listOf(
            "https://silverfox-c2.com\\foo.github.com/",
            "https://silverfox-c2.com:443\\foo.github.com/",
            "https://silverfox-c2.com\\foo.github.com@evil.com/",
            "https://silverfox-c2.com\\foo.git\thub.com/"
        )) {
            val result = SecurityScanner.assessUrl(url)
            assertEquals(url, RiskLevel.DANGEROUS, result.level)
            assertEquals(url, R.string.risk_reason_known_bad, result.reasonRes)
        }
    }

    @Test fun ambiguousAuthoritiesRequireConfirmation() {
        for (url in listOf(
            "https://silverfox-c2%2ecom/",
            "https://evil%2ftest.github.com/",
            "https://bücher.example/",
            "https://github.com:99999/",
            "https:///"
        )) {
            val result = SecurityScanner.assessUrl(url)
            assertEquals(url, RiskLevel.DANGEROUS, result.level)
            assertEquals(url, R.string.risk_reason_ambiguous_url, result.reasonRes)
        }
    }

    @Test fun establishedRulesStillApplyToTheActualHost() {
        assertEquals(RiskLevel.SAFE, SecurityScanner.assessUrl("https://sub.github.com\\page").level)
        assertEquals(RiskLevel.SAFE, SecurityScanner.assessUrl("https://github.com:443/silverfox").level)
        assertEquals(RiskLevel.DANGEROUS, SecurityScanner.assessUrl("https://example.com/silverfox").level)
        assertEquals(RiskLevel.DANGEROUS, SecurityScanner.assessUrl("https://github.com@evil.com/").level)
        assertEquals(RiskLevel.SUSPICIOUS, SecurityScanner.assessUrl("https://example.top/").level)
        assertEquals(RiskLevel.SUSPICIOUS, SecurityScanner.assessUrl("https://xn--bcher-kva.example/").level)
        assertEquals(RiskLevel.SUSPICIOUS, SecurityScanner.assessUrl("http://[::1]/").level)
        assertEquals(RiskLevel.SAFE, SecurityScanner.assessUrl("about:blank").level)
    }
}
