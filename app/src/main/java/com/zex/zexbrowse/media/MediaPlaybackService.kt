/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.media

import com.zex.zexbrowse.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * 网页媒体播放期间的前台服务。
 *
 * GeckoView 的音频在内容进程中解码播放，需要持续获得 CPU 时间片。
 * 应用退到后台或锁屏后如果没有前台服务，系统会降低进程优先级（部分 ROM 还会直接冻结/清理），
 * 音频线程来不及填充缓冲就会断续（表现为一卡一顿）。
 * 因此在网页开始播放媒体时启动一个 mediaPlayback 类型的前台服务，播放停止后关闭。
 */
class MediaPlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.media_playing))
            .setContentText(getString(R.string.media_playing_desc))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // 移除播放中的常驻通知，避免服务结束后通知残留
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.media_channel), NotificationManager.IMPORTANCE_LOW))
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "media_playback"
        private const val NOTIFICATION_ID = 0x5EA1

        /**
         * 启动播放前台服务。Android 12 起禁止后台启动前台服务，
         * 若因系统限制启动失败则静默忽略（播放本身不受影响，只是没有前台保护）。
         */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, MediaPlaybackService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, MediaPlaybackService::class.java)) }
        }
    }
}
