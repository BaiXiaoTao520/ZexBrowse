package com.zex.zexbrowse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeSitesTest {
    @Test fun fakeHostsMatchWithTrailingDotsAndQueryOrFragmentText() {
        for (host in listOf("MICYOU.CN", "download.micyou.cn")) {
            for (dot in listOf("", ".")) {
                for (suffix in listOf("", "?q=hello world", "?q=https://example.com/a b", "?q=[搜索]", "#hello world", "?q=%broken#[]")) {
                    assertEquals("micyou.cn", FakeSites.match("https://$host$dot:443/s$suffix"))
                    assertEquals("micyou.cn", FakeSites.match("$host$dot/s$suffix"))
                }
            }
        }
        assertEquals("micyou.cn", FakeSites.match("http://micyou.cn./"))
        assertEquals("micyou.cn", FakeSites.match("MICYOU.CN.?q=https://example.com/a b"))
    }

    @Test fun unrelatedHostsAndMalformedAddressesRemainUnmatched() {
        for (url in listOf("", "https:///missing", "https://bad host/?q=test", "about:blank", "https://micyou.cn.example.com./", "https://notmicyou.cn/", "https://example.com/?q=micyou.cn", "https://micyou.cn/bad path?q=test")) {
            assertNull(FakeSites.match(url))
        }
    }
}
