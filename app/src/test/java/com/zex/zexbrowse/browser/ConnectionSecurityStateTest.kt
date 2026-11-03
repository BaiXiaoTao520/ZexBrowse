package com.zex.zexbrowse.browser

import com.zex.zexbrowse.R
import com.zex.zexbrowse.data.SecurityScanner.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionSecurityStateTest {
    private val state = ConnectionSecurityState()

    private fun security(secure: Boolean = true, active: Int = 0, passive: Int = 0, certificate: Boolean = true, origin: String? = "https://example.com") =
        state.onSecurityChange(origin, secure, active, passive, certificate, contentLoaded = 2)

    @Test fun httpAndUnverifiedHttpsNeverBecomeSafeOnLocationChange() {
        assertEquals(RiskLevel.INSECURE, state.onPageStart("http://example.com/").level)
        assertEquals(RiskLevel.INSECURE, security(secure = false, certificate = false).level)
        val httpFragment = state.onLocationChange("http://example.com/#fragment")
        assertEquals(RiskLevel.INSECURE, httpFragment.level)
        assertEquals(R.string.risk_reason_not_encrypted, httpFragment.reasonRes)

        assertEquals(RiskLevel.INSECURE, state.onPageStart("https://example.com/").level)
        assertEquals(RiskLevel.INSECURE, state.onLocationChange("https://example.com/#fragment").level)
        assertEquals(RiskLevel.SAFE, security().level)
        assertEquals(RiskLevel.SAFE, state.onLocationChange("https://example.com/other?x=1#fragment").level)
    }

    @Test fun locationChangesRetainConnectionProblemsAndRecheckUrlRisk() {
        state.onPageStart("https://example.com/")
        assertEquals(R.string.risk_reason_bad_cert, security(secure = false).reasonRes)
        assertEquals(R.string.risk_reason_bad_cert, state.onLocationChange("https://example.com/#one").reasonRes)
        assertEquals(R.string.risk_reason_mixed_active, security(active = 2).reasonRes)
        assertEquals(R.string.risk_reason_mixed_active, state.onLocationChange("https://example.com/#two").reasonRes)
        assertEquals(R.string.risk_reason_mixed_passive, security(passive = 2).reasonRes)
        assertEquals(RiskLevel.DANGEROUS, state.onLocationChange("https://example.com/yinhu").level)
        assertEquals(R.string.risk_reason_mixed_passive, state.onLocationChange("https://example.com/clean").reasonRes)
    }

    @Test fun newLoadsAndOriginChangesDiscardPreviousConnection() {
        state.onPageStart("https://example.com/")
        assertEquals(RiskLevel.SAFE, security().level)
        assertEquals(RiskLevel.INSECURE, state.onPageStart("https://example.com/").level)
        assertEquals(RiskLevel.SAFE, security().level)
        assertEquals(RiskLevel.INSECURE, state.onLocationChange("https://other.example/").level)
        assertEquals(RiskLevel.INSECURE, state.onLocationChange("http://other.example/").level)
    }

    @Test fun sessionsHaveIndependentConnectionState() {
        state.onPageStart("https://example.com/")
        assertEquals(RiskLevel.SAFE, security().level)
        val other = ConnectionSecurityState()
        assertEquals(RiskLevel.INSECURE, other.onLocationChange("https://example.com/#fragment").level)
    }

    @Test fun securityFromAnotherOrUnknownOriginCannotMarkPageSafe() {
        state.onPageStart("https://example.com:443/")
        assertEquals(RiskLevel.INSECURE, security(origin = "https://other.example").level)
        assertEquals(RiskLevel.INSECURE, security(origin = null).level)
        assertEquals(RiskLevel.SAFE, security().level)
    }
}
