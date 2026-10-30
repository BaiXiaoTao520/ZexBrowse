/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 调试功能的解锁状态。
 *
 * 只存在于进程内存，不落盘、不进 savedInstanceState：
 * 进程结束（用户划掉应用、系统回收、重启）或重新安装后自动回到未解锁，
 * 必须重新手动连点才能再次打开，与开发者选项「每次都要重新解锁」的预期一致。
 *
 * 用 Compose 的 mutableStateOf 持有：设置页依赖它决定是否显示入口卡片，
 * 解锁瞬间列表需要即时刷新。
 */
object DebugUnlock {

    /** 连点次数阈值，与 Android 开发者选项的传统一致 */
    private const val REQUIRED_TAPS = 7

    /**
     * 连点计数的失效间隔。
     *
     * 没有这个窗口，用户几个月里零星点几次也会凑满 7 次而意外解锁；
     * 超过这个间隔即视为重新开始计数。
     */
    private const val TAP_WINDOW_MILLIS = 1_500L

    var unlocked by mutableStateOf(false)
        private set

    private var taps = 0
    private var lastTapAt = 0L

    /**
     * 记录一次点击。
     *
     * @return 还差几次；已解锁或刚好本次解锁时返回 0
     */
    fun tap(now: Long = System.currentTimeMillis()): Int {
        if (unlocked) return 0
        if (now - lastTapAt > TAP_WINDOW_MILLIS) taps = 0
        lastTapAt = now
        taps++
        if (taps >= REQUIRED_TAPS) {
            taps = 0
            unlocked = true
            return 0
        }
        return REQUIRED_TAPS - taps
    }

    /** 退出调试模式：立即失效，需重新连点 */
    fun lock() {
        taps = 0
        lastTapAt = 0L
        unlocked = false
    }
}
