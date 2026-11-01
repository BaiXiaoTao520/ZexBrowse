/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * 网络诊断。
 *
 * 每一项都是真实测量，不做任何模拟：
 * - 链路信息来自 ConnectivityManager（系统真实上报的接口类型与能力）；
 * - DNS 是真的解析域名并取 IP；
 * - 延迟是真的 TCP 连接耗时；
 * - HTTP 可达性是真的发出请求并读取状态码。
 *
 * 诊断项分三层，从「本机有没有网」到「目标站点通不通」，
 * 任何一层失败都能定位到具体环节，而不是只给一句「网络异常」。
 */
object NetworkDiagnostics {

    /** 单项结果 */
    data class CheckResult(
        val name: String,
        /** 一句话结论 */
        val summary: String,
        /** 细节（耗时、IP、状态码等），无则为空 */
        val detail: String = "",
        val state: CheckState
    )

    enum class CheckState { OK, WARN, FAIL, RUNNING, SKIPPED }

    /** 一次完整诊断的结果集合 */
    data class Report(val results: List<CheckResult>)

    /** 连接类型的人类可读名 */
    private fun transportName(capabilities: NetworkCapabilities?): String = when {
        capabilities == null -> "未知"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "蓝牙共享"
        else -> "其它"
    }

    /** 第一层：系统链路状态 */
    fun checkLink(context: Context): CheckResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return CheckResult("网络接口", "无法读取网络状态", state = CheckState.FAIL)

        // 必须用 activeNetwork + getNetworkCapabilities，而不是已废弃的 activeNetworkInfo
        val network = cm.activeNetwork
            ?: return CheckResult("网络接口", "设备未连接任何网络", state = CheckState.FAIL)
        val caps = cm.getNetworkCapabilities(network)
            ?: return CheckResult("网络接口", "当前网络无有效能力信息", state = CheckState.FAIL)

        val transport = transportName(caps)
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return when {
            // VALIDATED 表示系统已实际探测过外网可达，比单纯的 INTERNET 声明更可靠
            validated -> CheckResult("网络接口", transport, "已通过系统连通性验证", CheckState.OK)
            hasInternet -> CheckResult(
                "网络接口", transport,
                "已连接但系统尚未确认外网可达，可能出现「已连接却上不了网」",
                CheckState.WARN
            )
            else -> CheckResult("网络接口", transport, "该网络未声明互联网能力", CheckState.WARN)
        }
    }

    /** 第二层：DNS 解析 */
    suspend fun checkDns(host: String = "www.github.com"): CheckResult = withContext(Dispatchers.IO) {
        try {
            val start = System.nanoTime()
            val addresses = InetAddress.getAllByName(host)
            val cost = (System.nanoTime() - start) / 1_000_000
            val ips = addresses.take(2).joinToString(", ") { it.hostAddress.orEmpty() }
            CheckResult("DNS 解析", "$host 解析成功", "$ips · ${cost}ms", CheckState.OK)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            CheckResult("DNS 解析", "$host 解析失败", e.message ?: "未知错误", CheckState.FAIL)
        }
    }

    /** 第三层：TCP 连接耗时（直接反映链路延迟） */
    suspend fun checkTcpLatency(host: String = "www.github.com", port: Int = 443): CheckResult =
        withContext(Dispatchers.IO) {
            try {
                val start = System.nanoTime()
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(host, port), 6000)
                }
                val cost = (System.nanoTime() - start) / 1_000_000
                CheckResult("连接延迟", "TCP 握手成功", "$host:$port · ${cost}ms", CheckState.OK)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                CheckResult("连接延迟", "TCP 握手失败", e.message ?: "未知错误", CheckState.FAIL)
            }
        }

    /** 第四层：HTTP 可达性与状态码 */
    suspend fun checkHttp(url: String = "https://www.github.com"): CheckResult = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
        try {
            val start = System.nanoTime()
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { response ->
                val cost = (System.nanoTime() - start) / 1_000_000
                CheckResult(
                    "HTTP 请求", "状态码 ${response.code}",
                    "$url · ${cost}ms", if (response.isSuccessful) CheckState.OK else CheckState.WARN
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            CheckResult("HTTP 请求", "请求失败", e.message ?: "未知错误", CheckState.FAIL)
        }
    }

    /**
     * 并行执行全部检查。
     *
     * DNS / TCP / HTTP 三项互相独立，并行跑能把总耗时压到最慢那项，
     * 而不是串行叠加三个超时。链路状态是同步的，先算。
     */
    suspend fun runAll(context: Context): Report = coroutineScope {
        val link = checkLink(context)
        // 链路不通就不必再做后面三项，直接给出明确结论
        if (link.state == CheckState.FAIL) {
            return@coroutineScope Report(
                listOf(
                    link,
                    CheckResult("DNS 解析", "已跳过", "先修复网络连接", CheckState.SKIPPED),
                    CheckResult("连接延迟", "已跳过", "先修复网络连接", CheckState.SKIPPED),
                    CheckResult("HTTP 请求", "已跳过", "先修复网络连接", CheckState.SKIPPED)
                )
            )
        }
        val dns = async { checkDns() }
        val tcp = async { checkTcpLatency() }
        val http = async { checkHttp() }
        // await 顺序无关紧要，三者在 async 时已并行启动
        Report(listOf(link, dns.await(), tcp.await(), http.await()))
    }
}
