/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import com.zex.zexbrowse.BuildConfig

/**
 * 柠枺镜像（Lemwood）高速下载服务接入。
 *
 * API 域名与启动器 ID 通过构建期注入（见 app/build.gradle.kts 的
 * LEMWOOD_API_BASE / LEMWOOD_LAUNCHER_ID），源码中不出现，避免公开仓库暴露接入点。
 * 未注入时 [isAvailable] 为 false，界面上不显示该入口。
 */
object LemwoodMirror {

    /** API 基础地址（形如 https://example.com/api/v2）；构建期注入，为空表示未配置 */
    private val apiBase: String get() = BuildConfig.LEMWOOD_API_BASE.trim().trimEnd('/')

    /** 本应用在该平台上的启动器 ID；构建期注入 */
    private val launcherId: String get() = BuildConfig.LEMWOOD_LAUNCHER_ID.trim()

    /**
     * 功能是否可用：配置齐备即可。
     *
     * 入口指向固定的启动器详情页，不依赖任何接口调用；
     * 站点维护或限流时入口依然有效，不应因此隐藏。
     */
    val isAvailable: Boolean get() = apiBase.isNotBlank() && launcherId.isNotBlank()

    /** 站点入口 */
    fun siteUrl(): String = apiBase.substringBefore("/api")

    /**
     * 本应用的启动器详情页：也是站点 PoW 验证流程的入口。
     * 下载必须经此页完成验证拿到一次性 token，不能直接拼下载路径，
     * 否则会绕过站点的验证机制。
     */
    fun launcherPageUrl(): String = "${siteUrl()}/launchers/$launcherId"
}
