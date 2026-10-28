/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * 实时通知（Android 16+ 的实时活动 / 各家系统的流体云、灵动胶囊等）状态检测。
 *
 * 这类能力由系统统一管控，应用无法自行开关，只能：
 * 1. 用 [NotificationManager.canPostPromotedNotifications] 查询当前是否可用；
 * 2. 跳转到系统设置页让用户开启。
 *
 * 因此本对象只做检测与跳转，不保存任何本地开关——本地存储的状态会与系统实际设置脱节，
 * 出现「设置里显示已开、实际通知仍不升格」的假象。
 */
object PromotedNotification {

    /** 系统设置里「实时通知 / 实时活动」页面的 action。API 36 引入 */
    private const val ACTION_PROMOTION_SETTINGS = "android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"

    /**
     * 当前设备是否支持实时通知。
     *
     * Android 16（API 36）才引入该能力；低于此版本一律视为不支持。
     * 部分厂商（如 HyperOS 的流体云）即使系统版本更高，也可能未实现该接口，
     * 查询会返回 false，此时界面按「不支持」处理，不做误导性展示。
     */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    /**
     * 用户是否已允许本应用使用实时通知。
     *
     * 直接向系统查询，因此用户在系统设置里改了开关、或换了通知渠道/关闭了通知，
     * 这里都会立即反映真实状态。
     */
    fun isEnabled(context: Context): Boolean {
        if (!isSupported()) return false
        return runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return false
            manager.canPostPromotedNotifications()
        }.getOrDefault(false)
    }

    /** 系统通知总开关是否开启；关闭时实时通知必然不可用，需要提示用户先打开通知 */
    fun areNotificationsEnabled(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false
    }.getOrDefault(false)

    /**
     * 打开发起实时通知的系统设置页。
     *
     * 优先使用实时通知专属页面；部分系统没有该页面时退回应用通知设置，
     * 再不行退回应用详情页，保证点击一定有响应。
     */
    fun openSettings(context: Context) {
        val pkgUri = android.net.Uri.fromParts("package", context.packageName, null)
        val candidates = buildList {
            if (isSupported()) add(Intent(ACTION_PROMOTION_SETTINGS).setData(pkgUri))
            add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(pkgUri))
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
}
