/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.MediaSession

/**
 * 网页媒体播放的会话管理：统一处理音频焦点、前台服务与会话优先级。
 *
 * 背景：GeckoView 自身不申请音频焦点（GeckoAppShell 中无 requestAudioFocus），
 * 音频解码又发生在内容进程中，需要稳定的 CPU 时间片。缺少以下三件事时，
 * 播放（尤其是退到后台或锁屏后）会被系统降优先级，表现为一卡一顿：
 * 1. 前台服务（mediaPlayback 类型）——保持进程优先级，Android 15 起也是后台播放的硬性要求；
 * 2. 音频焦点——与其他应用播放器正确互斥，避免混音与音量异常；
 * 3. 会话优先级提示——让 GeckoView 尽量保留播放会话状态。
 */
class MediaPlaybackController(private val context: Context) {

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // 当前正在播放的媒体会话（用于响应音频焦点变化）
    private var activeMediaSession: MediaSession? = null
    private var playing = false
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        val session = activeMediaSession ?: return@OnAudioFocusChangeListener
        when (change) {
            // 永久失去焦点（其他应用开始长期播放）：暂停本页播放，符合浏览器惯例
            AudioManager.AUDIOFOCUS_LOSS -> runCatching { session.pause() }
            // 短暂失去焦点（来电、导航播报等）：先静音，恢复后还原，避免与对方声音混在一起
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> runCatching { session.muteAudio(true) }
            AudioManager.AUDIOFOCUS_GAIN -> runCatching { session.muteAudio(false) }
            // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK（可降低音量继续播放）不做处理：
            // GeckoView 未暴露音量控制接口，强行静音反而会中断用户正在听的音乐
        }
    }

    /**
     * 绑定到某个网页会话：媒体开始播放时启动前台服务并申请音频焦点，
     * 停止播放时释放资源。
     */
    fun attach(session: GeckoSession) {
        session.setMediaSessionDelegate(object : MediaSession.Delegate {
            override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
                onPlaybackStarted(session, mediaSession)
            }

            override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
                onPlaybackStopped(mediaSession)
            }
        })
    }

    private fun onPlaybackStarted(session: GeckoSession, mediaSession: MediaSession) {
        playing = true
        activeMediaSession = mediaSession
        // 提示 GeckoView 这是一个重要会话，尽量保留其状态（后台播放更稳）
        runCatching { session.setPriorityHint(GeckoSession.PRIORITY_HIGH) }
        requestAudioFocus()
        MediaPlaybackService.start(context)
    }

    private fun onPlaybackStopped(mediaSession: MediaSession) {
        if (activeMediaSession !== mediaSession) return
        playing = false
        activeMediaSession = null
        abandonAudioFocus()
        MediaPlaybackService.stop(context)
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        runCatching { manager.requestAudioFocus(request) }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        focusRequest?.let { request -> runCatching { manager.abandonAudioFocusRequest(request) } }
        focusRequest = null
    }

    /** 应用退出（ViewModel 销毁）时释放资源，避免前台服务与焦点残留 */
    fun release() {
        if (!playing) return
        playing = false
        activeMediaSession = null
        abandonAudioFocus()
        MediaPlaybackService.stop(context)
    }
}
