package com.zex.zexbrowse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainlandSearchEnginesTest {
    @Test fun searchHostsRemainRestricted() {
        for (host in listOf("www.baidu.com", "m.baidu.com", "www.sogou.com", "so.com", "www.so.com", "m.so.com", "360.so", "www.360.so", "sm.cn", "www.sm.cn", "m.sm.cn", "quark.sm.cn", "so.toutiao.com", "so.douyin.com")) {
            assertEquals(host, MainlandSearchEngines.match("https://$host/s?q=test"))
        }
    }

    @Test fun trailingDotsMatchInNavigation() {
        for (host in listOf("www.baidu.com", "m.baidu.com", "www.sogou.com", "so.com", "www.so.com", "m.so.com", "360.so", "www.360.so", "sm.cn", "www.sm.cn", "m.sm.cn", "quark.sm.cn", "so.toutiao.com", "so.douyin.com")) {
            for (dot in listOf("", ".")) {
                assertEquals(host, MainlandSearchEngines.match("https://${host.uppercase()}$dot:443/s?q=a b"))
                assertEquals(host, MainlandSearchEngines.match("$host$dot/s"))
            }
        }
        for (host in listOf("pan.baidu.com.", "cn.bing.com.", "www.baidu.com.example.com.", "www.baidu.com.evil")) {
            assertNull(MainlandSearchEngines.match("https://$host/"))
        }
    }

    @Test fun independentProductsAndLookalikesRemainAccessible() {
        for (host in listOf("map.so.com", "map.360.cn", "pan.baidu.com", "pinyin.sogou.com", "quark.cn", "bing.com", "cn.bing.com", "www.baidu.com.example.com", "product.sm.cn")) {
            assertNull(MainlandSearchEngines.match("https://$host/"))
        }
    }

    @Test fun queryAndFragmentDoNotAffectHostParsing() {
        for (suffix in listOf("?q=hello world", "?q=https://example.com/a b", "?q=[搜索]", "#hello world", "?q=%broken#[]")) {
            assertEquals("www.baidu.com", MainlandSearchEngines.match("https://WWW.BAIDU.COM/s$suffix"))
        }
        assertEquals("www.so.com", MainlandSearchEngines.match("WWW.SO.COM?q=a b"))
        for (url in listOf("", "https:///missing", "https://bad host/?q=test", "about:blank")) {
            assertNull(MainlandSearchEngines.match(url))
        }
    }
}
