/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

/**
 * 识别不再受支持的系统。
 *
 * 已停止对 vivo / iQOO 设备的支持（无论国内还是海外版），覆盖其两套系统：
 * - Funtouch OS（`ro.vivo.os.name` 为 Funtouch）
 * - OriginOS（`ro.vivo.os.name` 为 OriginOS）
 *
 * 不再区分国内/海外，也不再依赖 locale 判断，只要系统名命中上述任一值即拦截。
 */
object UnsupportedSystem {

    /** 返回不受支持的系统名称；受支持时返回 null。 */
    fun detect(): String? {
        val os = readProp("ro.vivo.os.name") ?: return null
        return when {
            os.equals("Funtouch", ignoreCase = true) -> "Funtouch OS"
            os.equals("OriginOS", ignoreCase = true) -> "OriginOS"
            else -> null
        }
    }

    // 通过反射读取系统属性（android.os.SystemProperties 为隐藏 API）
    private fun readProp(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
