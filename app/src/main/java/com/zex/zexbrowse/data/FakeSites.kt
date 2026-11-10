/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

/**
 * 已知假冒站点名单。
 *
 * 与 [MainlandSearchEngines] 的区别：这里**不给继续访问**的选项。
 * 受限搜索引擎只是合规层面的暂时限制，用户知情后可以自行决定；
 * 而假冒站点是直接侵害用户的操作，不提供任何「继续访问」入口。
 *
 * 判定同样只用主机名精确匹配，不做模糊/关键词判断，避免误伤正常站点。
 */
object FakeSites {

    /**
     * 假冒站点主机及其子域。
     *
     * 匹配规则：主机名等于名单项，或以 `.名单项` 结尾。
     */
    private val hosts: List<String> = listOf(
        // micyou.cn：冒充站，存在诱导下载/钓鱼风险
        "micyou.cn",
    )

    /**
     * 判断该地址是否命中假冒站点。
     *
     * @param url 完整地址，只解析主机名；解析失败一律视为未命中（宁放开勿误伤）
     * @return 命中的主机名，未命中返回 null
     */
    fun match(url: String): String? {
        val host = hostOf(url) ?: return null
        if (host.isBlank()) return null
        // 取最长匹配，与受限引擎的做法保持一致
        return hosts
            .filter { base -> host == base || host.endsWith(".$base") }
            .maxByOrNull { it.length }
    }

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
