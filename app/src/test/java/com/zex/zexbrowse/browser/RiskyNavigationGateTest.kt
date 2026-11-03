package com.zex.zexbrowse.browser

import com.zex.zexbrowse.data.SecurityScanner
import org.junit.Assert.*
import org.junit.Test

class RiskyNavigationGateTest {
    private val dangerous = "https://silverfox-c2.com/"
    private val suspicious = "http://192.0.2.1/"

    @Test fun blockedBeforeUiIsReadyAndRequiresExplicitProceed() {
        val gate = RiskyNavigationGate()
        assertTrue(gate.shouldBlock("tab", dangerous, true, "standard"))
        val warning = gate.warnings.value.getValue("tab")
        assertEquals(SecurityScanner.RiskLevel.DANGEROUS, warning.assessment.level)
        assertTrue(gate.shouldBlock("tab", dangerous, true, "standard"))
        assertEquals(dangerous, gate.proceed("tab", gate.warnings.value.getValue("tab")))
        assertNull(gate.proceed("tab", warning))
        assertFalse(gate.shouldBlock("tab", dangerous, true, "standard"))
        gate.clearAllowances()
        assertTrue(gate.shouldBlock("tab", dangerous, true, "standard"))
    }

    @Test fun modesAndUserInitiationPreserveExistingPolicy() {
        val gate = RiskyNavigationGate()
        assertFalse(gate.shouldBlock("tab", dangerous, false, "enhanced"))
        assertFalse(gate.shouldBlock("tab", dangerous, true, "basic"))
        assertFalse(gate.shouldBlock("tab", suspicious, true, "standard"))
        assertTrue(gate.shouldBlock("tab", suspicious, true, "enhanced"))
        assertEquals(SecurityScanner.RiskLevel.SUSPICIOUS, gate.warnings.value.getValue("tab").assessment.level)
        gate.dismiss("tab")
        assertTrue(gate.warnings.value.isEmpty())
        assertTrue(gate.shouldBlock("tab", suspicious, true, "enhanced"))
    }

    @Test fun warningsBelongToTabsAndStaleConfirmationCannotProceed() {
        val gate = RiskyNavigationGate()
        assertTrue(gate.shouldBlock("one", dangerous, true, "standard"))
        val oldWarning = gate.warnings.value.getValue("one")
        assertTrue(gate.shouldBlock("two", dangerous, true, "standard"))
        assertTrue(gate.shouldBlock("one", suspicious, true, "enhanced"))
        assertNull(gate.proceed("one", oldWarning))
        gate.dismiss("one", oldWarning)
        assertEquals(suspicious, gate.warnings.value.getValue("one").uri)
        assertEquals(dangerous, gate.warnings.value.getValue("two").uri)
        assertFalse(gate.shouldBlock("one", "https://example.com/", true, "standard"))
        assertFalse(gate.warnings.value.containsKey("one"))
        gate.dismiss("two")
        assertTrue(gate.warnings.value.isEmpty())
    }
}
