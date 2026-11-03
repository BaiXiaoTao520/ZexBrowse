package com.zex.zexbrowse.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MicrophoneOriginTest {
    @Test fun originUsesSchemeHostAndEffectivePort() {
        val origin = microphoneOrigin("https://EXAMPLE.com/path?query=1")
        assertEquals("https://example.com:443", origin)
        assertEquals(origin, microphoneOrigin("https://example.com:443/other"))
        assertNotEquals(origin, microphoneOrigin("https://example.com:8443/"))
        assertNotEquals(origin, microphoneOrigin("http://example.com:443/"))
        assertEquals("http://example.com:80", microphoneOrigin("http://example.com/"))
    }

    @Test fun originOmitsCredentialsAndNormalizesHosts() {
        assertEquals("https://example.com:443", microphoneOrigin("https://user:pass@example.com/"))
        assertEquals("https://xn--bcher-kva.example:443", microphoneOrigin("https://bücher.example/"))
        assertEquals("https://[::1]:8443", microphoneOrigin("https://[0:0:0:0:0:0:0:1]:8443/"))
        assertEquals("https://foo=bar.example:443", microphoneOrigin("https://foo=bar.example/"))
    }

    @Test fun unsupportedOrInvalidUrisHaveNoRememberedOrigin() {
        assertNull(microphoneOrigin("file:///tmp/index.html"))
        assertNull(microphoneOrigin("about:blank"))
        assertNull(microphoneOrigin("https://example.com:99999/"))
    }
}
