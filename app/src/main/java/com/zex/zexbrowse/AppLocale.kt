/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * 应用语言的选择与生效。
 *
 * 语言是全局设置，但 Activity 创建时就要决定用哪套资源，而设置存在 DataStore（异步）。
 * 因此在写入时同步镜像一份到 SharedPreferences，供 [attachBaseContext] 同步读取，
 * 避免首帧显示旧语言再跳变。
 *
 * 切换语言后必须重建 Activity 才会真正生效：已经组合好的界面不会因为配置改变而
 * 自动重读资源。调用方在写完后应调用 recreate（见界面层的语言设置）。
 */
object AppLocale {

    const val SYSTEM = "system"
    const val ZH_CN = "zh-CN"
    const val ZH_TW = "zh-TW"
    const val EN = "en"

    /** 全部可选语言，顺序即设置页展示顺序 */
    val all: List<String> = listOf(SYSTEM, ZH_CN, ZH_TW, EN)

    private const val PREFS = "zexbrowse_locale"
    private const val KEY = "language"

    /** 该取值对应的 Locale；跟随系统时返回 null */
    fun localeOf(tag: String): Locale? = when (tag) {
        ZH_CN -> Locale.SIMPLIFIED_CHINESE
        ZH_TW -> Locale.TRADITIONAL_CHINESE
        EN -> Locale.ENGLISH
        else -> null
    }

    /** 同步读取当前选择的语言，供 Activity 启动时使用 */
    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM

    /** 同步镜像当前语言（设置变更时调用，保证重建后能立即读到） */
    fun remember(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tag).apply()
    }

    /**
     * 按当前语言返回包裹后的 Context。
     *
     * 跟随系统时清空默认 Locale 的自定义，交回系统；
     * 其余情况强制使用对应 Locale，使应用语言与系统语言解耦。
     *
     * 必须同时更新 resources.configuration 与 LocaleList，
     * 否则 Compose 里的 stringResource 仍会读系统语言。
     */
    fun wrap(base: Context): Context {
        val tag = current(base)
        val locale = localeOf(tag)
        val config = Configuration(base.resources.configuration)
        if (locale == null) {
            // 跟随系统：恢复系统语言（进程内此前可能被改成过其它语言）
            val systemLocale = LocaleList.getDefault().get(0) ?: Locale.getDefault()
            config.setLocales(LocaleList(systemLocale))
        } else {
            Locale.setDefault(locale)
            config.setLocales(LocaleList(locale))
        }
        return base.createConfigurationContext(config)
    }
}
