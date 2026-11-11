/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

/**
 * 国内搜索引擎的隐私提醒名单。
 *
 * 对这批搜索引擎不再做拦截，而是在进入前给出二次确认：
 * 它们会记录搜索内容与设备信息，用户知情后自行决定是否继续。
 * 这里只做「主机名精确匹配」，不做关键词模糊判断：
 * 模糊匹配会把 `baidu.com.example.com`（用户自建域名）、
 * 或搜索词里恰好含「百度」的普通网页误判成需要提醒的对象，属于不可接受的误伤。
 *
 * 必应（bing.com / cn.bing.com）不在名单内，不会触发提醒。
 */
object MainlandSearchEngines {

    /**
     * 需要隐私提醒的搜索主机（精确匹配，不包含独立产品子域）。
     *
     * 匹配规则：主机名必须等于名单项；移动入口单独列出。
     * 未列出的产品主机不会触发提醒，正常访问。
     */
    private val restrictedHosts: List<String> = listOf(
        // 百度搜索：只列搜索主机。baidu.com 下还有网盘（pan.baidu.com）、
        // 地图、文库等独立产品，不必对它们弹提示。
        // 搜索入口只有 www.baidu.com；m.baidu.com 是移动端搜索页，同属此列。
        "www.baidu.com",
        "m.baidu.com",
        // 搜狗搜索：只列搜索入口 www.sogou.com。
        // sogou.com 下还有输入法（pinyin / shurufa）、微信搜索等产品，不应打扰。
        "www.sogou.com",
        // 360 搜索
        "so.com",
        "www.so.com",
        "m.so.com",
        "360.so",
        "www.360.so",
        // 神马搜索
        "sm.cn",
        "m.sm.cn",
        "www.sm.cn",
        // 夸克搜索：只拦搜索子域。quark.cn 是夸克浏览器官网，
        // 整域拦截会把官网一起干掉，属于误伤
        "quark.sm.cn",
        // 头条搜索：只拦搜索子域，不影响 toutiao.com 官网
        "so.toutiao.com",
        // 抖音搜索：只拦搜索子域，不影响 douyin.com 官网
        "so.douyin.com",
        // 元宝搜索：属于国内搜索引擎，与其它引擎一致给隐私提醒
        "so.qq.com",
        // 必应国内站明确放行，不在此列
    )

    /**
     * 需要排除的例外主机：必应及其子域。
     *
     * 虽然必应不在受限名单里，但它是国内可用的合规替代方案，
     * 单独列出是为了把「不受影响」这件事写进代码，避免以后误加进受限名单。
     */
    private val allowedHosts: List<String> = listOf("bing.com")

    /**
     * 判断该地址是否命中受限的大陆搜索引擎。
     *
     * @param url 完整地址，只解析主机名部分；解析失败一律视为不受限（宁放开勿误伤）
     * @return 命中的主域名（用于提示文案），未命中返回 null
     */
    fun match(url: String): String? {
        val host = hostOf(url) ?: return null
        if (host.isBlank()) return null
        if (allowedHosts.any { host == it || host.endsWith(".$it") }) return null

        return restrictedHosts.firstOrNull { it == host }
    }

    /** 从地址中取出小写主机名；解析不了返回 null。公开供放行判定使用 */
    fun host(url: String): String? = hostOf(url)

    /**
     * 取该地址所属引擎的归并键；未命中返回 null。
     *
     * 同一引擎常有 www / m / 裸域多个入口（如 www.so.com、m.so.com、so.com），
     * 用户确认过其中任意一个，就不该在换到同引擎的另一个入口时再弹一次。
     * 这里把它们归并到同一个键，供「已确认」判定使用。
     */
    fun engineKey(url: String): String? {
        val host = match(url) ?: return null
        return engineRoot(host)
    }

    /** 把具体主机归并为其所属的引擎根域 */
    private fun engineRoot(host: String): String = when {
        host in BAIDU_HOSTS -> "baidu"
        host in SOGOU_HOSTS -> "sogou"
        host in SO_HOSTS -> "so360"
        host in SM_HOSTS -> "shenma"
        host in QUARK_HOSTS -> "quark"
        host in TOUTIAO_HOSTS -> "toutiao"
        host in DOUYIN_HOSTS -> "douyin"
        host in YUANBAO_HOSTS -> "yuanbao"
        // 兼底：名单新增主机但忘了归并时，用主机名自己，不影响正确性
        else -> host
    }

    private val BAIDU_HOSTS = setOf("www.baidu.com", "m.baidu.com")
    private val SOGOU_HOSTS = setOf("www.sogou.com")
    private val SO_HOSTS = setOf("so.com", "www.so.com", "m.so.com", "360.so", "www.360.so")
    private val SM_HOSTS = setOf("sm.cn", "m.sm.cn", "www.sm.cn")
    private val QUARK_HOSTS = setOf("quark.sm.cn")
    private val TOUTIAO_HOSTS = setOf("so.toutiao.com")
    private val DOUYIN_HOSTS = setOf("so.douyin.com")
    private val YUANBAO_HOSTS = setOf("so.qq.com")

    /** 从地址中取出小写主机名；解析不了返回 null */
    private fun hostOf(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        return runCatching {
            val address = trimmed.substringBefore('?').substringBefore('#')
            val withScheme = if (address.contains("://")) address else "https://$address"
            java.net.URI(withScheme).host?.lowercase()?.trimEnd('.')
        }.getOrNull()?.substringBefore(':')
    }
}
