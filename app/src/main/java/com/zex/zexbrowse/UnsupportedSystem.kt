/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.os.Build

/**
 * 识别不再受支持的系统。
 *
 * 仅针对：
 * - 魅族 Flyme（含 Flyme AIOS）：Build.DISPLAY 中含有 "FLYME"。
 * - vivo 国内版 Funtouch OS：`ro.vivo.os.name` 为 Funtouch，且 `ro.vivo.product.overseas` 不为 yes（国内版）；
 *   海外版该属性为 yes，必须排除，不能误判。OriginOS 不在拦截范围。
 */
object UnsupportedSystem {

    /** 返回不受支持的系统名称；受支持时返回 null。 */
    fun detect(): String? {
        if (isFlyme()) return "Flyme"
        if (isChinaFuntouch()) return "Funtouch OS"
        return null
    }

    // Flyme 与 Flyme AIOS 的 Display 标识均含 "FLYME"
    private fun isFlyme(): Boolean =
        Build.DISPLAY?.uppercase()?.contains("FLYME") == true

    private fun isChinaFuntouch(): Boolean {
        // 仅当系统名明确为 Funtouch 时才考虑（OriginOS 不在拦截范围）
        if (!readProp("ro.vivo.os.name").equals("Funtouch", ignoreCase = true)) return false

        // 排除海外版：ro.vivo.product.overseas 为 yes 即海外版；
        // 另外若地区 locale 已明确为非中国，也一并排除，避免误判
        if (readProp("ro.vivo.product.overseas").equals("yes", ignoreCase = true)) return false
        if (readProp("ro.product.locale.region")?.equals("CN", ignoreCase = true) == false) return false
        return true
    }

    // 通过反射读取系统属性（android.os.SystemProperties 为隐藏 API）
    private fun readProp(key: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
