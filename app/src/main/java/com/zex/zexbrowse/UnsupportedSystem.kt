/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import java.io.File

/**
 * 识别不再受支持的系统。
 *
 * 已停止对以下设备的支持：
 * - 华为系：EMUI、HarmonyOS（含纯血鸿蒙的卓易通兼容层）、Hi 畅享、Hi nova 与 nova 系列
 *
 * vivo / iQOO（Funtouch OS、OriginOS，含国内与海外版）已恢复支持，不再做任何识别与拦截。
 *
 * 判定只认厂商专属属性，避免误伤其它品牌：
 * - 荣耀（HONOR）自 2020 年独立后不再属于华为，其设备一律放行；
 *   该判断放在华为系检测之前，连分家前跑 EMUI 的老荣耀也一并排除。
 * - 卓易通只匹配 isulad / zhuoyi / anco 这类华为容器标识，
 *   不使用通用的 lxc，避免个别定制系统误判。
 */
object UnsupportedSystem {

    /** 返回不受支持的系统名称；受支持时返回 null。 */
    fun detect(): String? = detectHuawei()

    private fun detectHuawei(): String? {
        // 荣耀已独立，其设备（含分家前跑 EMUI 的老机型）不在停止支持范围内
        if (isHonor()) return null

        // 卓易通：纯血鸿蒙上运行安卓应用的兼容容器，本质仍是华为设备
        if (isInDroiTongContainer()) return "HarmonyOS（卓易通）"

        // HarmonyOS（兼容安卓的 2/3/4 代）：华为专属属性，任一非空即为鸿蒙设备。
        // 同时给 EMUI 设备补一个「鸿蒙」描述，避免已升级的机器被简单报成 EMUI。
        val harmony = readProp("hw_sc.build.platform.version")
            ?: readProp("ro.build.version.harmony")
        val harmonyLabel = if (harmony != null) "HarmonyOS" else null

        // EMUI：`ro.build.version.emui` 形如 EmotionUI_10.0.0 / EMUI_11.0.0
        val emui = readProp("ro.build.version.emui")
        if (emui != null && (emui.contains("emui", true) || emui.contains("emotionui", true))) {
            return when {
                harmonyLabel != null -> "EMUI（HarmonyOS）"
                else -> "EMUI"
            }
        }

        if (harmonyLabel != null) return harmonyLabel

        // Hi 畅享 / Hi nova / nova 系列：市场名、型号或品牌带专属标识
        val marketName = readProp("ro.product.marketname") ?: ""
        val model = readProp("ro.product.model") ?: ""
        val brand = readProp("ro.product.brand") ?: ""
        val manufacturer = readProp("ro.product.manufacturer") ?: ""
        val name = "$marketName $model"

        if (HI_CHANGXIANG.containsMatchIn(name)) return "Hi 畅享"
        // Hi nova 是中邮通信的华为智选品牌（套壳 nova，跑安卓版 EMUI，无鸿蒙属性），
        // 品牌名与华为不同，必须单独按名称识别，且要排在 nova 之前才能给出准确标签
        if (HI_NOVA.containsMatchIn(name) ||
            HI_NOVA.containsMatchIn(brand) ||
            HI_NOVA.containsMatchIn(manufacturer)
        ) return "Hi nova"
        // nova 只在华为/鸿蒙设备上匹配，避免其它品牌同名机型被误伤
        if (isHuaweiFamily(brand, manufacturer) && NOVA.containsMatchIn(name)) return "nova"

        return null
    }

    /** 荣耀设备：分家后 manufacturer 为 HONOR，分家前 brand 仍写作 HONOR */
    private fun isHonor(): Boolean {
        val brand = readProp("ro.product.brand") ?: ""
        val manufacturer = readProp("ro.product.manufacturer") ?: ""
        return brand.equals("HONOR", true) || manufacturer.equals("HONOR", true)
    }

    private fun isHuaweiFamily(brand: String, manufacturer: String): Boolean {
        return brand.equals("HUAWEI", true) || manufacturer.equals("HUAWEI", true) ||
            brand.startsWith("Hi", true) || manufacturer.startsWith("Hi", true)
    }

    /**
     * 是否运行在卓易通容器内。
     *
     * 该容器基于华为 iSulad，`/proc/self/cgroup` 会带上容器引擎标识；
     * 另有兼容层特有的 Settings 类可作为兜底。
     */
    private fun isInDroiTongContainer(): Boolean {
        val cgroup = runCatching { File("/proc/self/cgroup").readText() }.getOrNull()
        if (cgroup != null) {
            val lower = cgroup.lowercase()
            if (CONTAINER_MARKERS.any { lower.contains(it) }) return true
        }
        return runCatching { Class.forName("com.huawei.android.compat.Settings") }.isSuccess
    }

    // 通过反射读取系统属性（android.os.SystemProperties 为隐藏 API）
    private fun readProp(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** 卓易通/iSulad 容器标识；不使用通用的 lxc，避免误判定制系统 */
    private val CONTAINER_MARKERS = listOf("isulad", "zhuoyi", "anco")

    /** Hi 畅享（含「Hi 畅享」带空格写法） */
    private val HI_CHANGXIANG = Regex("hi\\s*畅享", RegexOption.IGNORE_CASE)

    /** Hi nova（中邮通信智选品牌；写成 Hi nova / Hinova / HI NOVA 都能命中） */
    private val HI_NOVA = Regex("hi\\s*nova", RegexOption.IGNORE_CASE)

    /** nova 系列 */
    private val NOVA = Regex("\\bnova\\b", RegexOption.IGNORE_CASE)
}
