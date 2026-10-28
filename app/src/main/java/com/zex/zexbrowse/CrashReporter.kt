/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃捕获与落地。
 *
 * 背景：开发者拿不到崩溃日志（无 Firebase / 无 adb），出问题只能靠猜。
 * 这里在进程最早阶段安装一个「兜底未捕获异常处理器」，把完整的异常栈连同
 * 设备/系统/ABI/版本信息写入应用私有目录下的一个文本文件：
 *
 *   /Android/data/<包名>/files/crash/last_crash.txt
 *
 * 下次启动时界面会读取并弹窗展示，用户可直接复制发回。
 *
 * 注意：
 * - 只做「记录 + 转发给系统默认处理器」，不吞异常，保证系统侧日志（logcat/tombstone）行为不变；
 * - 运行在崩溃线程上，任何操作都 runCatching 包裹，绝不能在记录日志时再抛二次异常；
 * - native 崩溃（SIGSEGV 等）无法被 Java 层拦截，此时该文件可能为空，但对绝大多数
 *   Java/Kotlin 层异常（含 NoSuchMethodError、UnsatisfiedLinkError 等 Error）都有效。
 */
object CrashReporter {

    private const val DIR = "crash"
    private const val FILE_NAME = "last_crash.txt"
    private const val BOOT_MARKER = "boot_in_progress"

    private fun crashDir(context: Context): File {
        // 优先外部私有目录（用户用文件管理器更容易取到），失败回退内部目录
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, DIR).apply { runCatching { mkdirs() } }
    }

    /** 崩溃日志文件路径（供界面提示用户去哪里取） */
    fun crashFile(context: Context): File = File(crashDir(context), FILE_NAME)

    /**
     * 安装全局未捕获异常处理器。可在 Application.onCreate 与 Activity.onCreate 重复调用，
     * 内部通过标记避免重复包裹。
     */
    fun install(context: Context) {
        val app = context.applicationContext
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(app, throwable) }
            // 继续交给原处理器（通常是系统进程），保持标准崩溃/logcat/tombstone 行为
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    @Volatile
    private var installed = false

    /** 供 try/catch 同步兜底时手动记录 */
    fun record(context: Context, throwable: Throwable) {
        runCatching { write(context.applicationContext, throwable) }
    }

    private fun write(context: Context, throwable: Throwable) {
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            append(context.getString(R.string.crash_report_header)).append('\n')
            append(context.getString(R.string.crash_report_time)).append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append('\n')
            append(context.getString(R.string.crash_report_thread)).append(Thread.currentThread().name).append('\n')
            append(context.getString(R.string.crash_report_version)).append(runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                "${info.versionName} (${info.longVersionCode})"
            }.getOrNull() ?: context.getString(R.string.crash_report_unknown)).append('\n')
            append(context.getString(R.string.crash_report_device)).append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL).append('\n')
            append(context.getString(R.string.crash_report_os)).append(android.os.Build.VERSION.RELEASE)
                .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n")
            append(context.getString(R.string.crash_report_abi)).append(runCatching { android.os.Build.SUPPORTED_ABIS.joinToString(",") }.getOrNull() ?: context.getString(R.string.crash_report_unknown)).append('\n')
            append(context.getString(R.string.crash_report_stack)).append('\n')
            append(stack)
        }
        runCatching { crashFile(context).writeText(text) }
    }

    /** 读取上一次保存的崩溃日志；无则返回 null */
    fun readLast(context: Context): String? = runCatching {
        val f = crashFile(context)
        if (f.exists()) f.readText() else null
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** 清除已保存的崩溃日志 */
    fun clear(context: Context) {
        runCatching { crashFile(context).delete() }
    }

    private fun markerFile(context: Context): File = File(crashDir(context), BOOT_MARKER)

    /** 启动开始时打标记：若下次启动时该标记仍在，说明上次在启动阶段就崩了 */
    fun markBoot(context: Context) {
        runCatching { markerFile(context).writeText("1") }
    }

    /** 首次帧组合成功时调用，清除启动标记（表示本次启动已完成） */
    fun markStarted(context: Context) {
        runCatching { markerFile(context).delete() }
    }

    /** 上次是否在启动阶段就崩溃（启动标记未被 markStarted 清除） */
    fun didCrashLastBoot(context: Context): Boolean = runCatching { markerFile(context).exists() }.getOrDefault(false)
}
