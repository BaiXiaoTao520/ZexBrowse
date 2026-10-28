/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import android.content.Context
import androidx.annotation.StringRes
import com.zex.zexbrowse.R

/**
 * 网页安全扫描。
 *
 * 完全在本地按规则判定，不联网、不上报，因此不泄露用户访问了哪些站点，
 * 也不会因为接口不可用而失效。判定分两层：
 *
 * 1. URL 层（[assessUrl]）：伪造、仿冒、黑名单、异常域名等不依赖网络的信号；
 * 2. TLS 层（[assess]）：由 GeckoView 提供的证书与混合内容状态。
 *
 * 两层取更严重者作为最终结论。规则故意保守——只有明确可疑才升级为
 * [RiskLevel.SUSPICIOUS]，避免把正常站点误判成风险页。
 */
object SecurityScanner {

    /** 风险等级，按严重度递增排列 */
    enum class RiskLevel { SAFE, INSECURE, SUSPICIOUS, DANGEROUS }

    /**
     * 扫描结论。
     *
     * 文案以字符串资源 + 参数的形式保存，而不是提前拼好的中文串：
     * 扫描发生在设置切换之外，若直接存文本，切到英文后这些提示仍会停在中文。
     *
     * @param level 风险等级
     * @param reasonRes 面向用户的简短原因资源，如「连接未加密」
     * @param reasonArgs 原因文案的格式参数
     * @param detailRes 展开说明资源，解释判定依据
     * @param detailArgs 展开说明的格式参数
     */
    data class RiskAssessment(
        val level: RiskLevel = RiskLevel.SAFE,
        @StringRes val reasonRes: Int = 0,
        val reasonArgs: List<String> = emptyList(),
        @StringRes val detailRes: Int = 0,
        val detailArgs: List<String> = emptyList()
    ) {
        val isRisky: Boolean get() = level == RiskLevel.SUSPICIOUS || level == RiskLevel.DANGEROUS

        /** 当前语言下的简短原因；无风险时为空串 */
        fun reason(context: Context): String =
            if (reasonRes == 0) "" else context.getString(reasonRes, *reasonArgs.toTypedArray())

        /** 当前语言下的详细说明；无风险时为空串 */
        fun detail(context: Context): String =
            if (detailRes == 0) "" else context.getString(detailRes, *detailArgs.toTypedArray())
    }

    /**
     * 综合判定：URL 规则 + TLS 状态，取更严重者。
     *
     * @param isSecure 页面是否处于有效 HTTPS 连接（证书有效且未被例外放行）
     * @param mixedActive 主动混合内容状态（密码表单、脚本等）
     * @param mixedPassive 被动混合内容状态（图片、字体等）
     * @param hasCertificate 是否拿到了证书
     * @param contentLoaded GeckoView 的「已加载」常量值，不在此处硬编码
     */
    fun assess(
        url: String,
        isSecure: Boolean,
        mixedActive: Int = -1,
        mixedPassive: Int = -1,
        hasCertificate: Boolean = false,
        contentLoaded: Int = -1
    ): RiskAssessment {
        val urlResult = assessUrl(url)
        val tlsResult = assessTls(url, isSecure, mixedActive, mixedPassive, hasCertificate, contentLoaded)
        return if (tlsResult.level.ordinal > urlResult.level.ordinal) tlsResult else urlResult
    }

    /** TLS 与连接层判定 */
    private fun assessTls(
        url: String,
        isSecure: Boolean,
        mixedActive: Int,
        mixedPassive: Int,
        hasCertificate: Boolean,
        contentLoaded: Int
    ): RiskAssessment {
        if (!url.startsWith("http", ignoreCase = true)) {
            return RiskAssessment()
        }
        val https = url.startsWith("https://", ignoreCase = true)
        val loaded = contentLoaded >= 0 && mixedActive == contentLoaded
        val passiveLoaded = contentLoaded >= 0 && mixedPassive == contentLoaded

        if (!https) {
            return RiskAssessment(
                level = RiskLevel.INSECURE,
                reasonRes = R.string.risk_reason_not_encrypted,
                detailRes = R.string.risk_detail_not_encrypted
            )
        }

        // 主动混合内容：脚本、样式、表单等被降级加载，攻击面较大
        if (loaded) {
            return RiskAssessment(
                level = RiskLevel.INSECURE,
                reasonRes = R.string.risk_reason_mixed_active,
                detailRes = R.string.risk_detail_mixed_active
            )
        }

        if (!isSecure) {
            // 明确拿到证书却仍不安全：证书校验未通过
            return RiskAssessment(
                level = RiskLevel.INSECURE,
                reasonRes = if (hasCertificate) R.string.risk_reason_bad_cert else R.string.risk_reason_tls_error,
                detailRes = if (hasCertificate) R.string.risk_detail_bad_cert else R.string.risk_detail_tls_unknown
            )
        }

        if (passiveLoaded) {
            return RiskAssessment(
                level = RiskLevel.INSECURE,
                reasonRes = R.string.risk_reason_mixed_passive,
                detailRes = R.string.risk_detail_mixed_passive
            )
        }

        return RiskAssessment()
    }

    /**
     * 仅依据 URL 的判定（不依赖网络与证书）。
     *
     * 判定顺序即优先级：越具体的伪造手法越先命中。
     */
    fun assessUrl(rawUrl: String): RiskAssessment {
        val url = rawUrl.trim()
        if (url.isEmpty()) return RiskAssessment()
        if (!url.startsWith("http", ignoreCase = true)) return RiskAssessment()

        val authority = extractAuthority(url) ?: return RiskAssessment()
        val host = authority.substringAfterLast('@').substringBefore(':').lowercase()
        if (host.isBlank()) return RiskAssessment()

        // 1. 用 @ 掩盖真实主机：http://www.bank.com@evil.cn/
        if (authority.contains('@')) {
            return RiskAssessment(
                level = RiskLevel.DANGEROUS,
                reasonRes = R.string.risk_reason_deceptive_url,
                detailRes = R.string.risk_detail_deceptive_url, detailArgs = listOf(host)
            )
        }

        // 白名单优先：已知安全的大站不做后续启发式判定，避免误伤
        if (isWhitelisted(host)) return RiskAssessment()

        // 2. 内置黑名单（域名与路径关键词）
        blockedDomain(host)?.let {
            return RiskAssessment(level = RiskLevel.DANGEROUS, reasonRes = R.string.risk_reason_known_bad, detailRes = it)
        }
        blockedPath(url)?.let {
            return RiskAssessment(level = RiskLevel.DANGEROUS, reasonRes = R.string.risk_reason_trojan, detailRes = it)
        }

        // 3. 仿冒知名站点（同形字符归一化后比较）
        impersonatedBrand(host)?.let {
            return RiskAssessment(
                level = RiskLevel.DANGEROUS,
                reasonRes = R.string.risk_reason_impersonation,
                detailRes = R.string.risk_detail_impersonation, detailArgs = listOf(it)
            )
        }

        // 4. 启用 punycode 的国际化域名：同形异义字符几乎都借此伪装
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            return RiskAssessment(
                level = RiskLevel.SUSPICIOUS,
                reasonRes = R.string.risk_reason_punycode,
                detailRes = R.string.risk_detail_punycode
            )
        }

        // 5. 滥用高发后缀（免费注册、批量投放垃圾站居多）
        val tld = host.substringAfterLast('.')
        if (tld in ABUSED_TLDS) {
            return RiskAssessment(
                level = RiskLevel.SUSPICIOUS,
                reasonRes = R.string.risk_reason_bad_tld,
                detailRes = R.string.risk_detail_bad_tld, detailArgs = listOf(tld)
            )
        }

        // 6. 直接用 IP 访问（且非安全连接）
        if (isIpAddress(host) && !url.startsWith("https://", ignoreCase = true)) {
            return RiskAssessment(
                level = RiskLevel.SUSPICIOUS,
                reasonRes = R.string.risk_reason_raw_ip,
                detailRes = R.string.risk_detail_raw_ip
            )
        }

        // 7. 域名结构异常
        abnormalHost(host)?.let {
            return RiskAssessment(level = RiskLevel.SUSPICIOUS, reasonRes = R.string.risk_reason_abnormal_host, detailRes = it)
        }

        return RiskAssessment()
    }

    /** 取出 // 之后、/ ? # 之前的部分（含可能的 user@host:port） */
    private fun extractAuthority(url: String): String? {
        val afterScheme = url.substringAfter("://", "")
        if (afterScheme.isBlank()) return null
        return afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    }

    private fun isIpAddress(host: String): Boolean =
        host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")) || host.startsWith("[") || host.contains(':')

    /** 域名结构异常：过长标签、层级过深、数字占比过高 */
    private fun abnormalHost(host: String): Int? {
        val labels = host.split('.').filter { it.isNotBlank() }
        if (labels.isEmpty()) return null
        if (labels.any { it.length > 40 }) {
            return R.string.risk_detail_host_long
        }
        if (labels.size > 5) {
            return R.string.risk_detail_host_deep
        }
        // 去掉后缀，只看主体部分；数字占比过高多见于自动生成的域名
        val body = labels.dropLast(1).joinToString("")
        if (body.length >= 8) {
            val digits = body.count { it.isDigit() }
            if (digits * 100 / body.length >= 40) {
                return R.string.risk_detail_host_digits
            }
        }
        return null
    }

    /** 已知风险域名：命中即危险 */
    private fun blockedDomain(host: String): Int? {
        for ((domain, note) in BLOCKED_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) return note
        }
        return null
    }

    /** URL 路径中的风险关键词（正常网站不会用这些词做路径） */
    private fun blockedPath(url: String): Int? {
        val lower = url.lowercase()
        for ((keyword, note) in BLOCKED_PATH_KEYWORDS) {
            if (lower.contains(keyword)) return note
        }
        return null
    }

    /**
     * 与知名品牌高度相似但不是官方域名。
     *
     * 只在「品牌名本身」上做相似度判断，不做任意子串包含：
     * 否则 `pineapple.com` 会因包含 `apple` 而被误判。
     * 具体做法是把域名主体拆成词，逐个词比较；只有某个词与品牌名
     * 完全相等、或仅差一个同形字符时，才判定为仿冒。
     */
    private fun impersonatedBrand(host: String): String? {
        if (OFFICIAL_DOMAINS.any { host == it || host.endsWith(".$it") }) return null

        val core = host.removePrefix("www.")
        val labels = core.split('.').filter { it.isNotBlank() }
        if (labels.isEmpty()) return null
        // 主体：去掉最后两段（域名 + 后缀）后剩下的部分；没有子域时用第一段
        val segments = if (labels.size >= 2) labels.dropLast(1) else labels

        for (segment in segments) {
            // 把复合词拆开：apple-login / applelogin 都可能出现
            for (candidate in splitWords(segment)) {
                if (candidate.length < 4) continue
                for (brand in PROTECTED_BRANDS) {
                    val brandCore = brand.substringBefore('.')
                    if (brandCore.length < 4) continue
                    val normalizedCandidate = normalizeLookalikes(candidate)
                    // 归一化后与品牌名相同，说明用了数字/字母替换（如 g00gle、app1e），
                    // 这是仿冒而非官方域名（官方域名已在函数开头放行）
                    if (normalizedCandidate == brandCore) return brand
                    if (isLookalike(normalizedCandidate, brandCore)) return brand
                }
            }
        }
        return null
    }

    /**
     * 把 `apple-login`、`apple--login` 这类用分隔符拼出的写法拆成候选词。
     *
     * 不做去数字处理：`g00gle`、`app1e` 的数字本身就是同形替换的关键特征，
     * 去掉了反而无法识别。
     */
    private fun splitWords(segment: String): List<String> {
        val parts = segment.split('-', '_', '.', ' ').filter { it.isNotBlank() }
        return if (parts.isEmpty()) listOf(segment) else parts
    }

    /**
     * 判断是否为品牌的同形异义仿冒：长度接近且编辑距离 ≤ 1。
     *
     * 归一化后完全相等的情况已在调用处直接返回，这里只处理
     * 归一化无法覆盖的剩余单字符差异。
     */
    private fun isLookalike(candidate: String, brand: String): Boolean {
        if (candidate == brand) return false
        if (kotlin.math.abs(candidate.length - brand.length) > 1) return false
        return editDistanceAtMostOne(candidate, brand)
    }

    /** 编辑距离是否 ≤ 1（只允许替换、插入、删除各一次） */
    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        val longer = if (a.length >= b.length) a else b
        val shorter = if (a.length >= b.length) b else a
        if (longer.length - shorter.length > 1) return false

        var i = 0
        var j = 0
        var edited = false
        while (i < longer.length && j < shorter.length) {
            if (longer[i] == shorter[j]) {
                i++
                j++
                continue
            }
            if (edited) return false
            edited = true
            // 长度相同：当作替换；否则当作删除较长串的一个字符
            if (longer.length == shorter.length) {
                i++
                j++
            } else {
                i++
            }
        }
        return true
    }

    /**
     * 把常见同形字符归一化，用于识别 `app1e.com`、`rnicrosoft.com` 这类仿冒。
     */
    private fun normalizeLookalikes(host: String): String = host
        .replace("0", "o")
        .replace("1", "l")
        .replace("3", "e")
        .replace("4", "a")
        .replace("5", "s")
        .replace("rn", "m")
        .replace("vv", "w")
        .replace("cl", "d")
        .replace("-", "")

    /** 已知安全的大站：跳过启发式判定，避免 .top 等后缀或长度规则误伤 */
    private fun isWhitelisted(host: String): Boolean =
        SAFE_DOMAINS.any { host == it || host.endsWith(".$it") }

    /** 已知风险域名（示例名单，可在后续按需扩充） */
    private val BLOCKED_DOMAINS: Map<String, Int> = mapOf(
        "silverfox-c2.com" to R.string.risk_detail_silverfox_domain,
        "yinhu-muma.top" to R.string.risk_detail_yinhu_domain
    )

    /** 路径关键词：覆盖银狐木马、常见诈骗落地页的特征 */
    private val BLOCKED_PATH_KEYWORDS: Map<String, Int> = mapOf(
        "silverfox" to R.string.risk_detail_silverfox_path,
        "yinhu" to R.string.risk_detail_yinhu_path,
        "muma" to R.string.risk_detail_trojan_path,
        "apk-update" to R.string.risk_detail_apk_update,
        "install-apk" to R.string.risk_detail_install_apk
    )

    /** 受保护品牌（仅用于识别仿冒，不长于 4 字符的会被跳过以减少误伤） */
    private val PROTECTED_BRANDS = listOf(
        "apple.com", "icloud.com", "google.com", "gmail.com", "microsoft.com",
        "paypal.com", "alipay.com", "taobao.com", "tmall.com", "jd.com",
        "weixin.qq.com", "wechat.com", "tencent.com", "qq.com",
        "icbc.com.cn", "ccb.com", "abchina.com", "boc.cn", "cmbchina.com",
        "unionpay.com", "gov.cn"
    )

    /** 这些品牌自己的官方域名，命中即放行 */
    private val OFFICIAL_DOMAINS = PROTECTED_BRANDS

    /** 滥用率高的顶级域：命中只判可疑，不直接判危险 */
    private val ABUSED_TLDS = setOf(
        "tk", "ml", "ga", "cf", "gq", "top", "buzz", "click", "work", "loan",
        "zip", "mov", "rest", "cyou", "sbs"
    )

    /** 已知安全站点：跳过启发式判定 */
    private val SAFE_DOMAINS = setOf(
        "github.com", "githubusercontent.com", "raw.githubusercontent.com",
        "google.com", "gstatic.com", "googleapis.com", "youtube.com",
        "apple.com", "microsoft.com", "mozilla.org", "wikipedia.org",
        "baidu.com", "bdstatic.com", "qq.com", "tencent.com", "weixin.qq.com",
        "taobao.com", "tmall.com", "jd.com", "bilibili.com", "zhihu.com",
        "weibo.com", "163.com", "126.com", "aliyun.com", "cloudflare.com",
        "jsdelivr.net", "npmjs.com", "stackoverflow.com", "npmjs.org"
    )
}
