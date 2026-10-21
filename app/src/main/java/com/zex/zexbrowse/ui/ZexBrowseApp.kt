/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zex.zexbrowse.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.ContactSupport
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zex.zexbrowse.browser.BrowserViewModel
import com.zex.zexbrowse.data.BrowserNotice
import com.zex.zexbrowse.data.BrowserSettings
import com.zex.zexbrowse.data.DownloadEntity
import com.zex.zexbrowse.data.MirrorChyan
import com.zex.zexbrowse.data.RecommendedApp
import com.zex.zexbrowse.data.SettingsStore
import com.zex.zexbrowse.data.SearchSuggestions
import com.zex.zexbrowse.data.UpdateChecker
import com.zex.zexbrowse.data.ReleaseInfo
import com.zex.zexbrowse.data.recommendedApps
import com.zex.zexbrowse.download.DownloadManager
import com.zex.zexbrowse.download.UpdateDownloadResult
import com.zex.zexbrowse.ExternalLinkDiagnostics
import com.zex.zexbrowse.BuildConfig
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Page { HOME, APP_RECOMMENDATIONS, BROWSER, TABS, SETTINGS, BOOKMARKS, DOWNLOADS, HISTORY, DOWNLOAD_DIRECTORY, GENERAL_SETTINGS, SITE_SETTINGS, COOKIE_SETTINGS, WEB_PERMISSIONS, SEARCH_ENGINE, CUSTOM_SEARCH, USER_AGENT, CUSTOM_USER_AGENT, CLIPBOARD_PERMISSION, MIC_PERMISSION, EXTERNAL_APP_PERMISSION, ABOUT }

data class QuickSite(val title: String, val url: String, val icon: String)

/** 设置搜索项：子选项标题 + 所属页面（作为副标题并决定图标）+ 可匹配关键词 + 点击跳转动作 */
private data class SettingSearchItem(
    val title: String,
    val group: String,
    val keywords: List<String>,
    val onClick: () -> Unit,
    val accent: Boolean = false,
    val subtitle: String? = null
)

/** 设置页入口卡片（搜索框为空时的默认展示） */
private data class SettingsEntryItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val accent: Boolean = false
)

/** 赞助与支持说明中引导用户查看赞赏码的地址（仓库 README 的赞助章节） */
private const val SPONSOR_PAGE_URL = "https://github.com/BaiXiaoTao520/ZexBrowse/blob/main/README.md"

/** 联系我们的跳转地址 */
private const val CONTACT_URL = "https://knocket.ai/u/zex"

/** 书签序列化：每行「标题|网址」，与 DataStore 中的存储格式一致 */
private fun encodeBookmarks(sites: List<QuickSite>): String =
    sites.joinToString("\n") { "${it.title}|${it.url}" }

/**
 * 书签反序列化。与旧版快捷站点不同，空数据返回空列表而非默认站点：
 * 书签是用户自己的数据，全部删除后应当显示空态提示，而不是又冒出一批默认项。
 */
/**
 * 网页加载失败时的错误页：显示目标网址、无法访问的结论，以及若干条可读的失败原因。
 *
 * 错误码直接使用 [org.mozilla.geckoview.WebRequestError] 中的符号常量比较，
 * 不硬编码数值，避免内核升级后数值变动导致误判。
 */
@Composable
private fun PageErrorView(
    url: String,
    errorCode: Int,
    errorCategory: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reasons = remember(errorCode, errorCategory) { describeLoadFailure(errorCode, errorCategory) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Text("无法访问此网站", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            // 同步显示请求失败的网址，方便用户核对是否输错或粘贴了错误链接
            Text(
                url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("重新加载")
            }
        }
    }
}

/**
 * 将 GeckoView 的错误码与分类翻译成给用户看的原因列表。
 *
 * 返回值至少包含两条通用建议，保证任何错误都有可读说明；
 * 命中具体错误时会把最贴切的一条放在最前面。
 *
 * 不用 when 分支直接比对错误码：这些常量来自 Java 类的静态字段，
 * 用 if/else 链更直白，也避开了“分支必须为编译期常量”的限制。
 */
private fun describeLoadFailure(errorCode: Int, errorCategory: Int): List<String> {
    val specific = when {
        errorCode == WebRequestError.ERROR_UNKNOWN_HOST -> "域名解析失败，可能是网址拼写有误或该网站已停止服务"
        errorCode == WebRequestError.ERROR_OFFLINE -> "设备当前未连接到网络"
        errorCode == WebRequestError.ERROR_NET_TIMEOUT -> "连接超时，服务器长时间没有响应"
        errorCode == WebRequestError.ERROR_CONNECTION_REFUSED -> "服务器拒绝了连接请求"
        errorCode == WebRequestError.ERROR_NET_INTERRUPT -> "网络连接中断，传输被意外切断"
        errorCode == WebRequestError.ERROR_NET_RESET -> "连接被服务器重置"
        errorCode == WebRequestError.ERROR_REDIRECT_LOOP -> "页面发生了过多的重定向，可能陷入了循环"
        errorCode == WebRequestError.ERROR_PORT_BLOCKED -> "目标端口被拦截"
        errorCode == WebRequestError.ERROR_SECURITY_SSL ||
            errorCode == WebRequestError.ERROR_SECURITY_BAD_CERT ||
            errorCode == WebRequestError.ERROR_BAD_HSTS_CERT -> "该网站的安全证书存在问题，连接不被信任"
        errorCode == WebRequestError.ERROR_HTTPS_ONLY -> "该网站要求使用 HTTPS 访问"
        errorCode == WebRequestError.ERROR_UNKNOWN_PROTOCOL -> "无法识别的链接协议"
        errorCode == WebRequestError.ERROR_MALFORMED_URI -> "网址格式不正确"
        errorCode == WebRequestError.ERROR_FILE_NOT_FOUND -> "要访问的文件不存在"
        errorCode == WebRequestError.ERROR_FILE_ACCESS_DENIED -> "没有权限访问该文件"
        errorCode == WebRequestError.ERROR_CORRUPTED_CONTENT -> "服务器返回的内容已损坏"
        errorCode == WebRequestError.ERROR_CONTENT_CRASHED -> "页面渲染进程异常退出"
        errorCode == WebRequestError.ERROR_UNSAFE_CONTENT_TYPE -> "返回的内容类型不被允许"
        errorCode == WebRequestError.ERROR_INVALID_CONTENT_ENCODING -> "返回的内容编码无法解析"
        errorCode == WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> "代理服务器拒绝了连接"
        errorCode == WebRequestError.ERROR_UNKNOWN_PROXY_HOST -> "无法解析代理服务器地址"
        errorCode == WebRequestError.ERROR_LOCAL_NETWORK_ACCESS_DENIED -> "本地网络访问权限被拒绝"
        errorCode == WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI -> "该网址被标记为恶意网站，已阻止访问"
        errorCode == WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> "该网址被标记为不受欢迎的网站，已阻止访问"
        errorCode == WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> "该网址被标记为有害网站，已阻止访问"
        errorCode == WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> "该网址被判定为钓鱼网站，已阻止访问"
        else -> null
    }

    val byCategory = when {
        errorCategory == WebRequestError.ERROR_CATEGORY_NETWORK -> "网络连接异常"
        errorCategory == WebRequestError.ERROR_CATEGORY_SECURITY -> "安全策略阻止了本次连接"
        errorCategory == WebRequestError.ERROR_CATEGORY_CONTENT -> "页面内容无法正常加载"
        errorCategory == WebRequestError.ERROR_CATEGORY_URI -> "网址无法被识别"
        errorCategory == WebRequestError.ERROR_CATEGORY_PROXY -> "代理设置导致连接失败"
        errorCategory == WebRequestError.ERROR_CATEGORY_SAFEBROWSING -> "网站安全检测阻止了本次访问"
        else -> null
    }

    return buildList {
        specific?.let { add(it) }
        byCategory?.takeIf { it != specific }?.let { add(it) }
        add("请检查网络连接后重试")
        add("如果其它网站可以正常打开，请确认该网站本身是否正常运行")
    }
}

private fun decodeBookmarks(raw: String): List<QuickSite> =
    raw.split("\n").mapNotNull { line ->
        val title = line.substringBefore('|').trim()
        val url = line.substringAfter('|', "").trim()
        if (title.isEmpty() || url.isEmpty()) null else QuickSite(title, url, title.take(1))
    }

private fun Context.findActivity(): android.app.Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

// 判断本应用是否为系统默认浏览器：系统会把网页链接的默认 handler 指向默认浏览器
private fun isAppDefaultBrowser(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        .addCategory(Intent.CATEGORY_BROWSABLE)
    @Suppress("DEPRECATION")
    val resolved = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

/**
 * 构造系统「默认浏览器选择框」的 Intent（Android 10+ 的角色请求）。
 * 返回 null 表示当前系统不支持该角色，由调用方回退到默认应用设置页。
 *
 * 注意：必须用 ActivityResultLauncher 以 startActivityForResult 形式启动（见 ZexBrowseApp），
 * 系统端 RequestRoleActivity 靠 getCallingPackage() 识别请求方，
 * 而该值仅在 startActivityForResult 启动时才非空；用 startActivity 会被系统直接忽略。
 */
private fun defaultBrowserRoleIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return runCatching {
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
        if (roleManager == null || !roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) return null
        roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER)
    }.getOrNull()
}

/** 系统不支持角色请求时的兜底：打开系统「默认应用」设置页，再不济提示手动操作 */
private fun openDefaultAppSettings(context: Context) {
    val launched = runCatching {
        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    if (!launched) Toast.makeText(context, "请前往系统设置 → 应用 → 默认应用中将 ZexBrowse 设为浏览器", Toast.LENGTH_LONG).show()
}

@Composable
fun ZexBrowseApp(incomingUrl: String? = null, incomingUrlSeq: Long = 0L, onIncomingUrlHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settings.collectAsState(initial = BrowserSettings())
    val scope = rememberCoroutineScope()
    val browserViewModel: BrowserViewModel = viewModel()
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // 默认浏览器角色请求：结果由 ON_RESUME 时的 isAppDefaultBrowser 重新检测，这里无需处理返回值
    val defaultBrowserRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    // 引导用户设置默认浏览器：优先弹系统选择框（Android 10+）；
    // 系统不支持或启动失败时回退到「默认应用」设置页
    val requestDefaultBrowser: () -> Unit = {
        val roleIntent = defaultBrowserRoleIntent(context)
        if (roleIntent == null) {
            openDefaultAppSettings(context)
        } else {
            runCatching { defaultBrowserRoleLauncher.launch(roleIntent) }.onFailure { openDefaultAppSettings(context) }
        }
    }
    // 网页请求麦克风时待回传的引擎回调与设备 id；pendingMicSite 非空表示正在弹「是否允许」站点询问框
    val pendingMicCallback = remember { mutableStateOf<GeckoSession.PermissionDelegate.MediaCallback?>(null) }
    val pendingMicId = remember { mutableStateOf<String?>(null) }
    var pendingMicSite by remember { mutableStateOf<String?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = pendingMicCallback.value
        val microphoneId = pendingMicId.value
        pendingMicCallback.value = null
        pendingMicId.value = null
        if (callback != null) { if (granted) callback.grant(null, microphoneId) else callback.reject() }
    }
    // 首启提示里「设置外部存储」直接调起系统目录选择器并保存授权
    val externalDirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val displayPath = uri.path?.substringAfterLast(":")?.let { "/storage/emulated/0/$it/" } ?: ""
            scope.launch { settingsStore.downloadDirectory("external", uri.toString(), displayPath) }
            Toast.makeText(context, "已设置为外部存储", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 17 (API 37) 起，访问局域网需 ACCESS_LOCAL_NETWORK 运行时权限（常量用字面量以避免 compileSdk 36 下不可用）
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 37 && androidx.core.content.ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_LOCAL_NETWORK") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            localNetworkPermissionLauncher.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }
    }
    var navStack by rememberSaveable { mutableStateOf(listOf(Page.HOME)) }
    // 导航方向：true=前进（入栈），false=后退（出栈）。
    // 不能靠比较栈深度推断（状态更新时序不可靠），而是在导航时显式记录。
    var navForward by remember { mutableStateOf(true) }
    val page = navStack.last()
    val navigate: (Page) -> Unit = { navForward = true; navStack = navStack + it }
    val goBack: () -> Unit = { if (navStack.size > 1) { navForward = false; navStack = navStack.dropLast(1) } }
    var tabsIncognito by remember { mutableStateOf(false) }
    var startupUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
    // 赞助与支持：从「关于」页的卡片入口打开说明弹窗
    var sponsorInfo by remember { mutableStateOf(false) }
    // 首次启动引导：提醒下载目录为内部存储（仅首次，读取已持久化的真实值，避免默认初始值误触发）
    var showFirstRunStorageHint by remember { mutableStateOf(false) }
    // 首启引导第二弹：推荐将 ZexBrowse 设为默认浏览器（在外部存储提示之后，仅首启展示一次）
    var showDefaultBrowserPrompt by remember { mutableStateOf(false) }
    // 本应用是否已被用户设为默认浏览器（用于决定是否展示设置项入口）
    var isDefaultBrowser by remember { mutableStateOf(isAppDefaultBrowser(context)) }

    // 外部链接（默认浏览器 / 其它应用打开/分享链接）：新建普通标签打开
    // 以 incomingUrlSeq 为 key，保证即使目标 URL 与上次完全相同也能重新触发，不丢链接
    LaunchedEffect(incomingUrlSeq, incomingUrl) {
        val target = incomingUrl ?: return@LaunchedEffect
        android.util.Log.d("ZexIntent", "LaunchedEffect external url=$target seq=$incomingUrlSeq")
        // 外部链接优先复用当前标签：首次启动时已有一个空白首页标签，
        // 此时若无条件 newTab 就会出现两个标签页（原问题是两个空白页）。
        // 仅当当前标签已经加载过内容（非空白）时，才新建标签，避免覆盖用户正在看的网页。
        if (browserViewModel.loadExternalUrl(target)) {
            navStack = listOf(Page.HOME, Page.BROWSER)
        } else {
            browserViewModel.newTab(initialUrl = target)
            navStack = listOf(Page.HOME, Page.BROWSER)
        }
        onIncomingUrlHandled()
    }

    // 进入应用、切回前台（含从系统默认应用设置返回）时重新检测默认浏览器状态：
    // 已是默认则不显示入口；用户改选了其它浏览器后入口重新出现
    val lifecycleOwner = LocalLifecycleOwner.current
    // 首启引导：外部存储提示处理完后，再询问是否设为默认浏览器（两步互斥，避免弹窗叠加）
    LaunchedEffect(settings.storageHintShown, settings.downloadDirectoryMode, isDefaultBrowser) {
        if (settings.defaultBrowserHintShown || isDefaultBrowser) return@LaunchedEffect
        if (showFirstRunStorageHint) return@LaunchedEffect
        // 外部存储提示尚在等待处理（目录仍为内部存储且从未提示过）时先让位，
        // 否则已有外部存储的老用户会因 storageHintShown 永远为 false 而永远不触发
        val storageHintPending = !settings.storageHintShown && settings.downloadDirectoryMode == "internal"
        if (storageHintPending) return@LaunchedEffect
        if (startupUpdate != null) return@LaunchedEffect
        if (settingsStore.settings.first().defaultBrowserHintShown) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        if (startupUpdate == null && !isAppDefaultBrowser(context)) showDefaultBrowserPrompt = true
    }
    DisposableEffect(lifecycleOwner, navStack.last()) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDefaultBrowser = isAppDefaultBrowser(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次启动提示：下载目录仍为内部存储时，建议改为外部存储（用户可选稍后）
    // 稍作延迟，让更新检查先完成，避免与更新弹窗叠加
    LaunchedEffect(Unit) {
        val stored = settingsStore.settings.first()
        if (stored.downloadDirectoryMode != "internal" || stored.storageHintShown) return@LaunchedEffect
        kotlinx.coroutines.delay(1200)
        if (startupUpdate == null) showFirstRunStorageHint = true
    }

    browserViewModel.browserSettings = settings
    // 网页请求麦克风 → 按「网页权限管理 → 麦克风权限」策略处理：
    // 始终拒绝直接回绝，始终允许直接放行，每次询问则弹窗并把选择按站点记住。
    // 放行前必须持有系统录音权限：未授权时先走系统询问，用户允许后才回传引擎。
    browserViewModel.mediaPermissionHandler = { uri, audio, callback ->
        val microphoneId = audio.firstOrNull()?.id
        val site = siteHost(uri)
        val grantOrRequestSystem = {
            if (hasRecordAudioPermission(context)) callback.grant(null, microphoneId)
            else {
                pendingMicCallback.value = callback
                pendingMicId.value = microphoneId
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        scope.launch {
            val stored = settingsStore.settings.first()
            when (stored.micPolicy) {
                "allow" -> grantOrRequestSystem()
                "deny" -> callback.reject()
                else -> when (stored.siteMicPermissions[site]) {
                    "allow" -> grantOrRequestSystem()
                    "deny" -> callback.reject()
                    else -> {
                        pendingMicCallback.value = callback
                        pendingMicId.value = microphoneId
                        pendingMicSite = site
                    }
                }
            }
        }
    }
    LaunchedEffect(settings.autoCheckUpdates) {
        if (settings.autoCheckUpdates) {
            val checker = UpdateChecker(cdk = settings.mirrorCdk)
            startupUpdate = runCatching { checker.latest() }.getOrNull()?.takeIf { checker.isNewer(it.version) }
        }
    }
    DisposableEffect(
        lifecycleOwner,
        settings.clearOnExit,
        settings.clearCookiesOnExit,
        settings.clearCacheOnExit,
        settings.clearHistoryOnExit
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // 配置变更（如旋转）不算退出，不应清掉无痕标签
                if (context.findActivity()?.isChangingConfigurations == true) return@LifecycleEventObserver
                // 退出无痕浏览：离开应用即彻底关闭全部无痕标签并清除其 Cookie/缓存，普通标签不受影响
                browserViewModel.exitIncognito()
                if (settings.clearOnExit) {
                    browserViewModel.clearBrowserData(settings.clearCookiesOnExit, settings.clearCacheOnExit, settings.clearHistoryOnExit)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var startupCleared by remember { mutableStateOf(false) }
    LaunchedEffect(settings.clearOnExit) {
        if (settings.clearOnExit && !startupCleared) {
            startupCleared = true
            browserViewModel.clearBrowserData(settings.clearCookiesOnExit, settings.clearCacheOnExit, settings.clearHistoryOnExit)
        }
    }

    // 侧滑返回（预测性返回）的跟手进度：0=未开始，1=已滑到底。
    // 仅用于手势进行中的实时位移，松手提交/取消后由回调恢复为 0。
    val backProgress = remember { Animatable(0f) }
    // 是否正在由侧滑手势完成返回：为 true 时不播放 AnimatedContent 自带的横向切换动画，
    // 否则会与整页位移叠加，出现「旧页面停留片刻再滑走」的双重动画。
    var predictivePop by remember { mutableStateOf(false) }
    // 手势进行中：true 时下层渲染目标页，使页面滑走时露出的是真实的目标页而非空白
    var backActive by remember { mutableStateOf(false) }
    // 本次手势将要返回到的页面（无页面可退时为默认值，不参与渲染）
    var backTarget by remember { mutableStateOf(Page.HOME) }
    // 返回是否可以拦截：有可退的页面栈，或浏览器可后退。
    // 侧滑手势与实体返回键共用同一套判定。
    val canHandleBack = navStack.size > 1 || (page == Page.BROWSER && browserViewModel.selected?.canGoBack == true)
    // 浏览器（搜索结果）页不启用侧滑跟手动画：退出搜索页时保持原有的页面切换动画，
    // 由 BackHandler 接管即可，其余页面不受影响。
    val predictiveEnabled = canHandleBack && page != Page.BROWSER
    val performBack: () -> Unit = {
        if (page == Page.BROWSER && browserViewModel.selected?.canGoBack == true) {
            browserViewModel.back()
        } else {
            goBack()
        }
    }
    // 计算并启用下层目标页；浏览器内先后退网页时页面不变，无需预渲染。
    // 预渲染目标为浏览器页时必须跳过：BrowserScreen 会通过 AndroidView 创建 GeckoView 并挂载同一个 session，
    // 而 GeckoView 不允许同一 session 同时挂到两个 View，否则会崩溃或黑屏。
    // 浏览器页需要预渲染时改为显示纯色背景，同样比空白自然。
    val beginBack: () -> Unit = {
        val browserCanGoBack = page == Page.BROWSER && browserViewModel.selected?.canGoBack == true
        if (!browserCanGoBack && navStack.size > 1) {
            backTarget = navStack[navStack.size - 2]
            backActive = true
        }
    }
    // 下层是否可安全预渲染真实页面
    val canPreviewTarget = backTarget != Page.BROWSER

    BackHandler(enabled = navStack.size > 1) {
        // 实体/三键返回不走整页跟手动画，确保标志处于关闭状态，避免影响常规页面切换动画
        predictivePop = false
        performBack()
    }

    // 预测性返回：系统开始侧滑时持续推送进度，据此实时位移页面，
    // 松手完成则执行返回，取消则回弹。仅 Android 13+ 支持，低版本自动回退到 BackHandler。
    //
    // 流程语义（官方约定）：
    // - collect 正常结束 = 手势完成，应提交返回；
    // - 抛 CancellationException = 手势取消，必须终止本协程，不能继续执行返回。
    PredictiveBackHandler(enabled = predictiveEnabled) { progressEvents ->
        beginBack()
        try {
            progressEvents.collect { event ->
                // 首个事件到达即开启下层目标页；初始进度可能很小，确保页面不会先露出一帧空白
                if (!backActive && event.progress > 0f) beginBack()
                backProgress.snapTo(event.progress)
            }
            // 手势完成：先把整页顺完滑出屏幕，再换页。
            backProgress.animateTo(1f, tween(100))
            // 标志在换页的整个重组周期内保持为 true，使 AnimatedContent 不插自己的滑入滑出动画；
            // 若在此处立即改回 false，重组时标志已复位，过渡动画依旧会启动。
            predictivePop = true
            performBack()
            // 换页与复位放在同一帧提交：旧页已完全滑出，下方的目标页已在原位静止渲染，
            // 切页后画面完全一致，因此看不到空白也不会跳帧。
            // 复位必须用 NonCancellable：若协程在换页后被取消，标志会永久停在 true，
            // 导致之后所有页面切换都丢失动画。
            withContext(NonCancellable) {
                backProgress.snapTo(0f)
                withFrameNanos { }
                predictivePop = false
                backActive = false
            }
        } catch (cancelled: CancellationException) {
            // 手势取消：本协程已处于取消态，回弹动画需放在 NonCancellable 中，
            // 否则 animateTo 会立即再抛 CancellationException，导致回弹不执行、页面停在半途。
            withContext(NonCancellable) {
                backProgress.animateTo(0f, tween(180))
                // 取消手势：下层目标页也随之收起
                backActive = false
            }
            throw cancelled
        }
    }

    val useDarkTheme = when (settings.darkMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    LaunchedEffect(useDarkTheme, settings.forceDarkWeb, settings.userAgentMode, settings.customUserAgent, settings.simplifiedUserAgent) {
        browserViewModel.applyForceDark(useDarkTheme, settings.forceDarkWeb)
        browserViewModel.applyCurrentUserAgentToSelected()
    }
    val colorScheme = if (settings.dynamicColor && android.os.Build.VERSION.SDK_INT >= 31) {
        if (useDarkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else if (useDarkTheme) {
        androidx.compose.material3.darkColorScheme()
    } else {
        androidx.compose.material3.lightColorScheme()
    }

    // 应用内主题可能与系统深浅色不一致，需显式设置状态栏/导航栏图标明暗，避免深色下看不清
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialExpressiveTheme(colorScheme = colorScheme) {
        Surface {
            Box(Modifier.fillMaxSize()) {
                // 下层：手势进行中预先渲染目标页，让滑走时能看到真实的目标页面而不是空白。
                // 仅在 beginBack() 之后有内容，平时完全不参与组合，不影响性能。
                if (backActive) {
                    Box(Modifier.fillMaxSize()) {
                        if (canPreviewTarget) {
                            PageContent(
                                destination = backTarget,
                                settings = settings,
                                searchHistory = settings.searchHistory,
                                settingsStore = settingsStore,
                                browserViewModel = browserViewModel,
                                scope = scope,
                                context = context,
                                isDefaultBrowser = isDefaultBrowser,
                                requestDefaultBrowser = requestDefaultBrowser,
                                micPermissionLauncher = micPermissionLauncher,
                                tabsIncognito = tabsIncognito,
                                onTabsIncognitoChange = { tabsIncognito = it },
                                navigate = navigate,
                                goBack = goBack,
                                onSponsor = { sponsorInfo = true }
                            )
                        } else {
                            // 目标为浏览器页：不能重复创建 GeckoView，用主题背景色占位，
                            // 比露出纯空白更自然，也不会出现白色闪屏。
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                        }
                    }
                }

                // 上层：当前页。手势期间随手指整体右移，并按 Miuix（HyperOS）风格
                // 缩放、收拢圆角、投下阴影，营造「页面被拎起退回上层」的层级感。
                // 不做透明度渐变：透明淡出会让下层页面「从暗渐亮」，观感上像蒙了层纱。
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val progress = backProgress.value
                        if (progress > 0f) {
                            translationX = size.width * progress
                            val scale = 1f - 0.16f * progress
                            scaleX = scale
                            scaleY = scale
                            // Miuix 风格：跟手缩放时边缘圆角收拢并投下阴影
                            shape = RoundedCornerShape((24f * progress).dp)
                            clip = true
                            shadowElevation = (14f * progress).dp.toPx()
                            ambientShadowColor = Color.Black.copy(alpha = 0.35f)
                            spotShadowColor = Color.Black.copy(alpha = 0.35f)
                        }
                    }
                ) {
                    // 页面跳转动效：前进时新页从右滑入、旧页向左滑出；后退时相反。
                    // 侧滑手势完成时跳过该动画，由整页位移接管，避免双重动画。
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            if (predictivePop) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                val direction = if (navForward) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                slideIntoContainer(direction, animationSpec = tween(260)) togetherWith slideOutOfContainer(direction, animationSpec = tween(260))
                            }
                        },
                        label = "page"
                    ) { destination ->
                        PageContent(
                            destination = destination,
                            settings = settings,
                            searchHistory = settings.searchHistory,
                            settingsStore = settingsStore,
                            browserViewModel = browserViewModel,
                            scope = scope,
                            context = context,
                            isDefaultBrowser = isDefaultBrowser,
                            requestDefaultBrowser = requestDefaultBrowser,
                            micPermissionLauncher = micPermissionLauncher,
                            tabsIncognito = tabsIncognito,
                            onTabsIncognitoChange = { tabsIncognito = it },
                            navigate = navigate,
                            goBack = goBack,
                            onSponsor = { sponsorInfo = true }
                        )
                    }
                }
            }
            startupUpdate?.let { release -> UpdateDialog(release = release, preferMirror = settings.preferMirrorDownload, onDismiss = { startupUpdate = null }) }
            if (sponsorInfo) SponsorDialog(onDismiss = { sponsorInfo = false })
            if (showFirstRunStorageHint && startupUpdate == null) {
                AlertDialog(
                    onDismissRequest = { showFirstRunStorageHint = false; scope.launch { settingsStore.storageHintShown(true) } },
                    title = { Text("建议修改下载目录") },
                    text = { Text("当前下载目录为内部存储（应用私有目录）。为保证大文件与外部存储访问，建议改为外部存储目录。") },
                        confirmButton = { TextButton(onClick = {
                            showFirstRunStorageHint = false
                            scope.launch { settingsStore.storageHintShown(true) }
                            externalDirPicker.launch(null)
                        }) { Text("设置外部存储") } },
                    dismissButton = { TextButton(onClick = { showFirstRunStorageHint = false; scope.launch { settingsStore.storageHintShown(true) } }) { Text("稍后") } }
                )
            }
            // 首启引导第二弹：外部存储提示处理完（或无需提示）后询问是否设为默认浏览器
            if (showDefaultBrowserPrompt && startupUpdate == null && !showFirstRunStorageHint) {
                // 显式标注 () -> Unit：末尾 scope.launch 返回 Job，存进变量后类型会被推成 () -> Job
                val dismissDefaultBrowserPrompt: () -> Unit = {
                    showDefaultBrowserPrompt = false
                    scope.launch { settingsStore.defaultBrowserHintShown(true) }
                }
                AlertDialog(
                    onDismissRequest = dismissDefaultBrowserPrompt,
                    title = { Text("设为默认浏览器") },
                    text = { Text("将 ZexBrowse 设为默认浏览器后，你从其它应用或分享菜单打开的网页链接会直接由它打开。是否设置完全由你自愿决定，也可以随时在系统设置中改回。") },
                    confirmButton = {
                        Button(onClick = {
                            dismissDefaultBrowserPrompt()
                            requestDefaultBrowser()
                        }) { Text("设为默认") }
                    },
                    dismissButton = { TextButton(onClick = dismissDefaultBrowserPrompt) { Text("稍后") } }
                )
            }
            // 网页请求麦克风且策略为「每次询问」时：按站点询问并记住选择。
            // 允许时若系统尚未授权，则转为系统询问（保留 pendingMicCallback，交给 launcher 回传结果）。
            val micSite = pendingMicSite
            if (micSite != null) {
                val finishMic: (Boolean, Boolean) -> Unit = { allow, remember ->
                    val callback = pendingMicCallback.value
                    val microphoneId = pendingMicId.value
                    if (remember) scope.launch { settingsStore.setSiteMicPermission(micSite, if (allow) "allow" else "deny") }
                    pendingMicSite = null
                    if (allow && !hasRecordAudioPermission(context)) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        pendingMicCallback.value = null
                        pendingMicId.value = null
                        if (allow) callback?.grant(null, microphoneId) else callback?.reject()
                    }
                }
                AlertDialog(
                    onDismissRequest = { finishMic(false, false) },
                    title = { Text("允许使用麦克风？") },
                    text = { Text("$micSite 请求使用你的麦克风。") },
                    confirmButton = { TextButton(onClick = { finishMic(true, true) }) { Text("始终允许") } },
                    dismissButton = { TextButton(onClick = { finishMic(false, true) }) { Text("拒绝") } }
                )
            }
        }
    }
}

/**
 * 单个页面的内容渲染。
 *
 * 抽成独立函数是为了让「当前页」与「侧滑返回时下层预渲染的目标页」共用同一份实现：
 * 手势滑动时上方页面被移走，下方显示的必须是真实的目标页（而不是空白），
 * 否则会先看到空白、松手后又瞬间切页，观感断裂。
 */
@Composable
private fun PageContent(
    destination: Page,
    settings: BrowserSettings,
    searchHistory: List<String>,
    settingsStore: SettingsStore,
    browserViewModel: BrowserViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    isDefaultBrowser: Boolean,
    requestDefaultBrowser: () -> Unit,
    micPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    tabsIncognito: Boolean,
    onTabsIncognitoChange: (Boolean) -> Unit,
    navigate: (Page) -> Unit,
    goBack: () -> Unit,
    onSponsor: () -> Unit
) {
    when (destination) {
        Page.HOME -> HomeScreen(
            settings = settings,
            searchHistory = settings.searchHistory,
            onRemoveSearchHistory = { scope.launch { settingsStore.removeSearchHistory(it) } },
            setQuickSites = { scope.launch { settingsStore.quickSites(it) } },
            onOpen = { address ->
                // 记录搜索历史（网址也一并记下，便于重复访问）
                scope.launch { settingsStore.addSearchHistory(address) }
                browserViewModel.load(address); navigate(Page.BROWSER)
            },
            onHistory = { navigate(Page.HISTORY) },
            onSettings = { navigate(Page.SETTINGS) },
            onAppRecommendations = { navigate(Page.APP_RECOMMENDATIONS) }
        )
        Page.APP_RECOMMENDATIONS -> AppRecommendationsScreen(
            browserViewModel = browserViewModel,
            onBack = goBack
        )
        Page.BROWSER -> BrowserScreen(
            viewModel = browserViewModel,
            settings = settings,
            settingsStore = settingsStore,
            onTabs = { incognito -> onTabsIncognitoChange(incognito); navigate(Page.TABS) },
            onDownloads = { navigate(Page.DOWNLOADS) },
            onSettings = { navigate(Page.SETTINGS) }
        )
        Page.TABS -> TabOverview(viewModel = browserViewModel, initialIncognito = tabsIncognito, onBack = goBack)
        Page.SETTINGS -> SettingsScreen(
            settings = settings,
            isDefaultBrowser = isDefaultBrowser,
            onSetDefaultBrowser = {
                // 直接弹系统询问框由用户选择（不替用户决定）；成功后 OnResume 会自动刷新入口可见性
                requestDefaultBrowser()
            },
            onHistory = { navigate(Page.HISTORY) },
            onBookmarks = { navigate(Page.BOOKMARKS) },
            onGeneralSettings = { navigate(Page.GENERAL_SETTINGS) },
            onSiteSettings = { navigate(Page.SITE_SETTINGS) },
            onCookieSettings = { navigate(Page.COOKIE_SETTINGS) },
            onWebPermissions = { navigate(Page.WEB_PERMISSIONS) },
            onAbout = { navigate(Page.ABOUT) },
            onContactUs = { openInBrowser(context, CONTACT_URL) },
            onBack = goBack
        )
        Page.GENERAL_SETTINGS -> GeneralSettingsScreen(
            settings = settings,
            setMode = { scope.launch { settingsStore.mode(it) } },
            setDynamic = { scope.launch { settingsStore.dynamic(it) } },
            setApkHash = { scope.launch { settingsStore.apkHash(it) } },
            setHardwareAcceleration = { scope.launch { settingsStore.hardwareAcceleration(it) } },
            onDownloads = { navigate(Page.DOWNLOADS) },
            onDownloadDirectory = { navigate(Page.DOWNLOAD_DIRECTORY) },
            onBack = goBack
        )
        Page.SITE_SETTINGS -> WebSiteSettingsScreen(
            settings = settings,
            setForceDarkWeb = { scope.launch { settingsStore.forceDarkWeb(it) } },
            onSearchEngine = { navigate(Page.SEARCH_ENGINE) },
            onUserAgent = { navigate(Page.USER_AGENT) },
            onBack = goBack
        )
        Page.COOKIE_SETTINGS -> CookieSettingsScreen(
            settings = settings,
            setCookie = { scope.launch { settingsStore.cookies(it) } },
            setClearOnExit = { enabled, cookies, cache, history -> scope.launch { settingsStore.clearOnExit(enabled, cookies, cache, history) } },
            clearBrowserData = {
                browserViewModel.clearBrowserData()
                Toast.makeText(context, "已完成", Toast.LENGTH_SHORT).show()
            },
            onBack = goBack
        )
        Page.WEB_PERMISSIONS -> WebPermissionsScreen(
            settings = settings,
            onClipboard = { navigate(Page.CLIPBOARD_PERMISSION) },
            onExternalApp = { navigate(Page.EXTERNAL_APP_PERMISSION) },
            onMic = { navigate(Page.MIC_PERMISSION) },
            onBack = goBack
        )
        Page.DOWNLOADS -> DownloadsScreen(browserViewModel, onBack = goBack)
        Page.BOOKMARKS -> BookmarksScreen(
            settings = settings,
            setQuickSites = { scope.launch { settingsStore.quickSites(it) } },
            onBack = goBack
        )
        Page.HISTORY -> HistoryScreen(viewModel = browserViewModel, onOpen = { url -> browserViewModel.load(url); navigate(Page.BROWSER) }, onBack = goBack)
        Page.DOWNLOAD_DIRECTORY -> DownloadDirectoryScreen(
            settings = settings,
            onSave = { mode, uri, path ->
                scope.launch { settingsStore.downloadDirectory(mode, uri, path) }
                Toast.makeText(context, "已修改", Toast.LENGTH_SHORT).show()
                goBack()
            },
            onBack = goBack
        )
        Page.SEARCH_ENGINE -> SearchEngineScreen(
            settings = settings,
            onSelect = { scope.launch { settingsStore.searchEngine(it) } },
            onCustom = { navigate(Page.CUSTOM_SEARCH) },
            onBack = goBack
        )
        Page.CUSTOM_SEARCH -> CustomSearchScreen(
            settings = settings,
            onSave = { title, url ->
                scope.launch { settingsStore.customSearch(title, url); settingsStore.searchEngine("custom") }
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                goBack()
            },
            onBack = goBack
        )
        Page.USER_AGENT -> UserAgentScreen(
            settings = settings,
            onSelect = { scope.launch { settingsStore.userAgentMode(it) } },
            onSimplified = { scope.launch { settingsStore.simplifiedUserAgent(it) } },
            onCustom = { navigate(Page.CUSTOM_USER_AGENT) },
            onBack = goBack
        )
        Page.CUSTOM_USER_AGENT -> CustomUserAgentScreen(
            settings = settings,
            onSave = { title, value ->
                scope.launch { settingsStore.customUserAgent(title, value); settingsStore.userAgentMode("custom") }
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                goBack()
            },
            onBack = goBack
        )
        Page.CLIPBOARD_PERMISSION -> ClipboardPermissionScreen(
            settings = settings,
            onSelect = { scope.launch { settingsStore.clipboardReadPolicy(it) } },
            onClear = {
                scope.launch { settingsStore.clearSitePermissions() }
                Toast.makeText(context, "已清除已记住的网站权限", Toast.LENGTH_SHORT).show()
            },
            onBack = goBack
        )
        Page.MIC_PERMISSION -> MicPermissionScreen(
            settings = settings,
            onSelect = { policy ->
                scope.launch { settingsStore.micPolicy(policy) }
                // 同步系统录音权限：始终允许时申请一次，始终拒绝时撤销已授予的授权
                when (policy) {
                    "allow" -> if (!hasRecordAudioPermission(context)) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    "deny" -> revokeRecordAudioPermission(context)
                }
            },
            onClear = {
                scope.launch { settingsStore.clearSiteMicPermissions() }
                Toast.makeText(context, "已清除已记住的网站权限", Toast.LENGTH_SHORT).show()
            },
            onBack = goBack
        )
        Page.EXTERNAL_APP_PERMISSION -> SimplePolicyScreen(
            title = "跳转至外部 APP 权限",
            description = "网页尝试打开非 http(s) 链接（如 intent:、mailto: 等）时的处理方式。始终允许/拒绝将直接放行或拦截；每次询问则会弹出确认提示。",
            options = listOf("ask" to "每次询问", "allow" to "始终允许", "deny" to "始终拒绝"),
            current = settings.externalAppPolicy,
            onSelect = { scope.launch { settingsStore.externalAppPolicy(it) } },
            onBack = goBack
        )
        Page.ABOUT -> AboutScreen(
            settings = settings,
            setAutoCheckUpdates = { scope.launch { settingsStore.autoCheckUpdates(it) } },
            setMirrorCdk = { scope.launch { settingsStore.mirrorCdk(it) } },
            setPreferMirrorDownload = { scope.launch { settingsStore.preferMirrorDownload(it) } },
            onSponsor = onSponsor,
            onBack = goBack
        )
    }
}

@Composable
private fun GlassSurface(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, content = content)
    }
}

/** 首页顶栏时钟：独立状态，每秒只重组自身，避免拖拽整个 HomeScreen 重组导致掉帧 */
@Composable
private fun ClockText() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); kotlinx.coroutines.delay(1_000) } }
    Text(SimpleDateFormat("yyyy年MM月dd日  HH:mm:ss", Locale.getDefault()).format(now), style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun HomeScreen(settings: BrowserSettings, searchHistory: List<String>, onRemoveSearchHistory: (String) -> Unit, setQuickSites: (String) -> Unit, onOpen: (String) -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onAppRecommendations: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val bookmarks = remember(settings.quickSites) { decodeBookmarks(settings.quickSites) }
    var showQuickSiteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("ZexBrowse", fontWeight = FontWeight.Bold); ClockText() } },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Default.History, "浏览历史") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "设置") }
                }
            )
        }
    ) { padding ->
        Column(
            // 快捷访问条目多、或系统字体放大时会超出一屏，配上下滚动避免底部不可达
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            AddressInput(value = query, onValueChange = { query = it }, onSubmit = { onOpen(query) }, searchHistory = searchHistory, onRemoveHistory = onRemoveSearchHistory)
            Spacer(Modifier.height(32.dp))
            // 应用推荐入口：点击进入推荐列表（位于快捷访问上方）
            AppRecommendationEntry(onClick = onAppRecommendations)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("书签", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showQuickSiteDialog = true }) { Text("添加") }
            }
            BookmarkGrid(
                sites = bookmarks,
                onOpen = onOpen,
                onDelete = { site -> setQuickSites(encodeBookmarks(bookmarks.filter { it.url != site.url })) }
            )
        }
    }

    if (showQuickSiteDialog) {
        QuickSiteDialog(
            onAdd = { site ->
                setQuickSites(encodeBookmarks(bookmarks + site))
                showQuickSiteDialog = false
            },
            onDismiss = { showQuickSiteDialog = false }
        )
    }
}

/**
 * 应用推荐列表页。
 *
 * 下载行为沿用本应用已有的下载体系：
 * - APK 直链 → 走应用内下载（下载目录遵循「下载设置」的配置）；
 * - 网页/网盘类链接 → 交给系统处理，由系统默认浏览器或用户选择的应用打开。
 */
@Composable
private fun AppRecommendationsScreen(browserViewModel: BrowserViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<RecommendedApp?>(null) }
    // 已触发下载的条目 → 其在下载记录中的 id，用于完成后直接安装
    var downloadingApp by remember { mutableStateOf<RecommendedApp?>(null) }
    var downloadId by remember { mutableStateOf<String?>(null) }
    var downloadDone by remember { mutableStateOf(false) }
    // 网页/网盘类条目：确认二次说明弹窗后再跳转浏览器
    var browserNoticeApp by remember { mutableStateOf<RecommendedApp?>(null) }
    val downloads by browserViewModel.downloads.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用推荐") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Zex 开发者为您精选以下应用", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text("发现适合你的工具与创作体验", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(recommendedApps) { app ->
                RecommendationCard(app = app) { detail = app }
            }
        }
    }

    detail?.let { app ->
        RecommendationInfoDialog(
            app = app,
            onDownload = {
                detail = null
                when {
                    app.isApk -> {
                        downloadingApp = app
                        downloadDone = false
                        browserViewModel.enqueueDownload(app.downloadUrl, app.fileName, "") { id -> downloadId = id }
                    }
                    // 网页/网盘类链接：先弹二次说明（下载步骤、网盘提取码等），确认后再交给系统打开
                    app.browserNotice != null -> browserNoticeApp = app
                    else -> openInBrowser(context, app.downloadUrl)
                }
            },
            onDismiss = { detail = null }
        )
    }

    downloadingApp?.let { app ->
        val record = downloads.firstOrNull { it.id == downloadId }
        // 后台下载完成后自动切换到「安装」状态
        if ((record?.status == "completed" || record?.status == "hash_mismatch") && !downloadDone) downloadDone = true
        RecommendationDownloadDialog(
            app = app,
            record = record,
            finished = downloadDone,
            onInstall = { record?.let { openDownload(context, it) } },
            onDismiss = { downloadingApp = null; downloadId = null; downloadDone = false }
        )
    }

    val noticeApp = browserNoticeApp
    val notice = noticeApp?.browserNotice
    if (noticeApp != null && notice != null) {
        BrowserNoticeDialog(
            notice = notice,
            onOpen = { browserNoticeApp = null; openInBrowser(context, noticeApp.downloadUrl) },
            onDismiss = { browserNoticeApp = null }
        )
    }
}

/** 交给系统打开网页类链接（系统默认浏览器或用户选择的应用） */
private fun openInBrowser(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(context, "没有可以打开该链接的应用", Toast.LENGTH_SHORT).show() }
}

/**
 * 推荐条目卡片。
 *
 * 水波纹说明：[Card] 的 onClick 重载内部用卡片自身的 [shape] 作为水波纹边界，
 * 因此水波纹与卡片圆角完全一致；同时 [Card] 默认 clip 内容，图标方块也不会溢出。
 */
@Composable
private fun RecommendationCard(app: RecommendedApp, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标小正方形背景
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(app.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(app.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 推荐详情弹窗：展示完整说明，统一一个「下载」按钮：
 * APK 直链走应用内下载，网页/网盘链接交给系统打开。
 */
@Composable
private fun RecommendationInfoDialog(
    app: RecommendedApp,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(app.icon, null)
                Spacer(Modifier.width(10.dp))
                Text(app.displayName)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            ) {
                Text(app.description)
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Icon(if (app.isApk) Icons.Default.Download else Icons.Default.Language, null)
                Spacer(Modifier.width(6.dp))
                Text(if (app.isApk) "下载" else "浏览器打开")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 网页/网盘下载的二次说明弹窗：打开浏览器前先说明下载步骤或网盘信息（如蓝奏云提取码），
 * 确认后才跳转。对应原项目 ZexNote 中 AppShare / CoolMonitor 的下载说明弹窗。
 */
@Composable
private fun BrowserNoticeDialog(
    notice: BrowserNotice,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(notice.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            ) {
                Text(notice.content)
            }
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Icon(Icons.Default.Language, null)
                Spacer(Modifier.width(6.dp))
                Text(notice.confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/**
 * 应用内下载进度弹窗：进度直接取自下载记录（与「下载设置 → 下载记录」同一数据源），
 * 下载完成后原地切换为「安装」。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecommendationDownloadDialog(
    app: RecommendedApp,
    record: DownloadEntity?,
    finished: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val failed = record?.status == "failed" || record?.status == "cancelled"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载 ${app.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (finished) {
                    Text("下载完成，可直接安装；文件也已保存到「下载设置」指定的目录。")
                    record?.sha256?.takeIf { it.isNotBlank() }?.let { hash ->
                        Text("SHA-256：$hash", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (failed) {
                    Text(record?.errorMessage?.ifBlank { "下载失败" } ?: "下载失败")
                } else {
                    val progress = record?.progress ?: 0
                    if (progress > 0) {
                        // 确定态：沿用原来的平滑波浪进度条
                        WavyProgressBar(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
                        Text("正在下载：$progress%")
                    } else {
                        // 连接中：不确定态波浪进度条，无限滚动
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), amplitude = 0.5f, wavelength = 24.dp)
                        Text("正在连接服务器…")
                    }
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = onInstall) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("立即安装") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (finished) "关闭" else "后台下载") } }
    )
}

/**
 * 首页「应用推荐」入口卡片。
 *
 * 水波纹说明：使用 [Card] 的 onClick 重载（内部自带与卡片形状一致的 ripple），
 * 不再用 Modifier.clickable（矩形水波纹会超出圆角卡片边界造成错位）。
 */
@Composable
private fun AppRecommendationEntry(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标小方块背景
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("应用推荐", style = MaterialTheme.typography.titleMedium)
                Text("精选工具与创作应用，一点即览", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddressInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onRemoveHistory: (String) -> Unit = {},
    // 是否启用联想与历史下拉：首页需要，浏览器地址栏不需要（会遮挡网页）
    showSuggestionsPanel: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    // 远程联想结果；每次输入变化后由防抖效应刷新
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    // 本地过滤后的搜索历史（仅在无远程建议或未输入时展示）
    val matchedHistory = remember(searchHistory, value) {
        val keyword = value.trim()
        if (keyword.isEmpty()) searchHistory.take(8)
        else searchHistory.filter { it.contains(keyword, ignoreCase = true) }.take(8)
    }

    // 防抖拉取联想词：每次输入变更后延时 250ms 再请求，取消上一次任务，避免逐字刷接口
    LaunchedEffect(value) {
        val keyword = value.trim()
        if (!showSuggestionsPanel || keyword.isEmpty()) { suggestions = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(250)
        suggestions = SearchSuggestions.fetch(keyword)
    }

    // 无输入时优先展示历史；有输入时优先远程建议，无建议时回退历史
    val showHistory = value.isBlank() && matchedHistory.isNotEmpty()
    val showSuggestions = value.isNotBlank() && suggestions.isNotEmpty()
    val fallbackHistory = value.isNotBlank() && suggestions.isEmpty() && matchedHistory.isNotEmpty()
    val expanded = showSuggestionsPanel && focused && (showHistory || showSuggestions || fallbackHistory)

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) },
            placeholder = { Text("搜索或输入网址") },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotBlank()) {
                        IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, "清空") }
                    }
                    IconButton(onClick = { if (value.isNotBlank()) onSubmit() }) { Icon(Icons.Default.Search, "打开") }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { if (value.isNotBlank()) onSubmit() }),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // 展开/收起动效：高度伸展 + 淡入淡出同步进行，展开与收起都保持顺滑
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200), expandFrom = Alignment.Top) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(160), shrinkTowards = Alignment.Top) + fadeOut(animationSpec = tween(120))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val items = when {
                        showSuggestions -> suggestions
                        showHistory || fallbackHistory -> matchedHistory
                        else -> emptyList()
                    }
                    items.forEach { keyword ->
                        // 历史项可长按式的删除由末尾图标提供，联想项直接点击即搜索
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onValueChange(keyword); onSubmit() }
                                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (showSuggestions) Icons.Default.Search else Icons.Default.History,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(keyword, maxLines = 1, modifier = Modifier.weight(1f))
                            if (!showSuggestions) {
                                IconButton(onClick = { onRemoveHistory(keyword) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Close, "从搜索历史中移除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkGrid(sites: List<QuickSite>, onOpen: (String) -> Unit, onDelete: (QuickSite) -> Unit) {
    // 用普通 Column 而非 LazyColumn：本组件始终嵌在带 verticalScroll 的首页里，
    // 纵向可滚动组件不能在无限高度约束下测量（会直接抛异常导致启动闪退）；
    // 书签数量本就很少，无需懒加载。
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sites.isEmpty()) {
            Text(
                "暂无书签，您可以点击手动添加按钮",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            sites.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { site ->
                        BookmarkCard(site = site, onOpen = onOpen, onDelete = onDelete, modifier = Modifier.weight(1f))
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 书签卡片：长按删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkCard(site: QuickSite, onOpen: (String) -> Unit, onDelete: (QuickSite) -> Unit, modifier: Modifier = Modifier) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().height(92.dp)
        ) {
            // 长按弹删除菜单；点击打开书签
            Row(
                Modifier.fillMaxSize()
                    .combinedClickable(onClick = { onOpen(site.url) }, onLongClick = { menuExpanded = true })
                    .padding(14.dp)
            ) {
                Column {
                    Text(site.icon, style = MaterialTheme.typography.headlineSmall)
                    Text(site.title, maxLines = 1)
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("删除书签") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuExpanded = false; onDelete(site) }
            )
        }
    }
}

/**
 * 书签管理页：与首页书签卡片共用同一份数据（[BrowserSettings.quickSites]），
 * 任一侧新增/删除都会立即反映到另一侧。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarksScreen(settings: BrowserSettings, setQuickSites: (String) -> Unit, onBack: () -> Unit) {
    val bookmarks = remember(settings.quickSites) { decodeBookmarks(settings.quickSites) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("书签管理") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = {
                IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "添加书签") }
            }
        )
    }) { padding ->
        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("暂无书签", style = MaterialTheme.typography.titleMedium)
                    Text("长按列表中的书签即可删除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddDialog = true }) { Text("添加书签") }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.url }) { site ->
                    BookmarkManageRow(
                        site = site,
                        onDelete = { setQuickSites(encodeBookmarks(bookmarks.filter { it.url != site.url })) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        QuickSiteDialog(
            onAdd = { site ->
                // 同一网址不重复添加，避免首页出现重复卡片
                if (bookmarks.none { it.url == site.url }) setQuickSites(encodeBookmarks(bookmarks + site))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

/** 书签管理列表行：长按删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkManageRow(site: QuickSite, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(site.title, maxLines = 1) },
            supportingContent = { Text(site.url, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(site.icon, style = MaterialTheme.typography.titleMedium) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { menuExpanded = true })
                .padding(vertical = 2.dp)
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("删除书签") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun QuickSiteDialog(onAdd: (QuickSite) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加书签") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("名称") })
                OutlinedTextField(url, { url = it }, label = { Text("网址") })
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank() && url.isNotBlank()) onAdd(QuickSite(title, url, title.take(1))) }) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun BrowserScreen(viewModel: BrowserViewModel, settings: BrowserSettings, settingsStore: SettingsStore, onTabs: (Boolean) -> Unit, onDownloads: () -> Unit, onSettings: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val selectedTab = tabs.firstOrNull { it.id == selectedId }
    // 只展示与当前标签同类型的标签，避免无痕与普通标签在同一列混合
    val visibleTabs = tabs.filter { it.incognito == (selectedTab?.incognito ?: false) }
    var input by remember(selectedTab?.id) { mutableStateOf(selectedTab?.url.orEmpty()) }
    var inputFocused by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var expectedHash by remember { mutableStateOf("") }
    var hashResult by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var showNewTabDialog by remember { mutableStateOf(false) }
    var showBrowserMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // 用户拒绝了通知权限：前台服务仍会运行，但通知栏里看不到任何下载进度
        if (!granted) {
            Toast.makeText(context, "未授予通知权限，下载进度将只在应用内的下载记录中显示", Toast.LENGTH_LONG).show()
        }
    }
    // 网页文件选择：单/多选两个启动器，选中后回传给网页
    var filePickerCallback by remember { mutableStateOf<((Array<Uri>?) -> Unit)?>(null) }
    val filePickerSingle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = filePickerCallback; filePickerCallback = null
        callback?.invoke(if (uri != null) arrayOf(uri) else null)
    }
    val filePickerMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val callback = filePickerCallback; filePickerCallback = null
        callback?.invoke(uris.toTypedArray())
    }
    // 网页打开外部应用：待确认的链接及其跳转动作
    var pendingExternalAppUri by remember { mutableStateOf<String?>(null) }
    var pendingExternalApp by remember { mutableStateOf<(() -> Unit)?>(null) }
    // 网站读取剪贴板：待确认的站点与结果回调
    var clipboardReadSite by remember { mutableStateOf<String?>(null) }
    var clipboardReadCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    LaunchedEffect(selectedTab?.url) {
        if (!inputFocused) input = selectedTab?.url.orEmpty()
    }

    // 当前网址是否已是书签：与首页书签卡片、设置页书签管理共用同一份数据，
    // 用户在任一处增删后，这里的菜单文案会自动跟着变
    val currentBookmarks = remember(settings.quickSites) { decodeBookmarks(settings.quickSites) }
    val currentUrl = selectedTab?.url.orEmpty()
    val bookmarked = currentBookmarks.any { it.url == currentUrl }
    val toggleBookmark: () -> Unit = {
        if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
            if (bookmarked) {
                scope.launch { settingsStore.quickSites(encodeBookmarks(currentBookmarks.filter { it.url != currentUrl })) }
            } else {
                // 标题为空或为默认值时回退到域名，避免出现一批「新标签页」书签
                val title = selectedTab?.title?.takeIf { it.isNotBlank() && it != "新标签页" }
                    ?: Uri.parse(currentUrl).host?.removePrefix("www.")
                    ?: currentUrl
                scope.launch {
                    settingsStore.quickSites(encodeBookmarks(currentBookmarks + QuickSite(title, currentUrl, title.take(1))))
                }
            }
        }
        showBrowserMenu = false
    }

    viewModel.onExternalDownload = { url -> scope.launch { downloadUrl = url } }
    // 网页请求选择文件 → 拉起系统文件选择器（原生文件应用），选完回传网页
    viewModel.filePickerHandler = { mimeTypes, multiple, onResult ->
        val mimes = (mimeTypes?.toList()?.takeIf { it.isNotEmpty() } ?: listOf("*/*")).toTypedArray()
        filePickerCallback = onResult
        if (multiple) filePickerMultiple.launch(mimes) else filePickerSingle.launch(mimes)
    }
    // 网页请求打开外部应用 → 按「跳转至外部 APP 权限」策略处理：始终拒绝直接拦截，始终允许直接跳转，否则弹窗询问
    viewModel.externalAppHandler = { uri ->
        when (settings.externalAppPolicy) {
            "deny" -> Unit
            "allow" -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { Toast.makeText(context, "没有可以打开该链接的应用", Toast.LENGTH_SHORT).show() }
            else -> {
                pendingExternalApp = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { Toast.makeText(context, "没有可以打开该链接的应用", Toast.LENGTH_SHORT).show() }
                }
                pendingExternalAppUri = uri
            }
        }
    }
    // 网站请求读取剪贴板 → 按全局策略处理：始终允许/拒绝直接放行，询问则弹窗并将选择按站点永久记住
    viewModel.clipboardReadHandler = { site, callback ->
        scope.launch {
            val stored = settingsStore.settings.first()
            when (stored.clipboardReadPolicy) {
                "allow" -> callback(true)
                "deny" -> callback(false)
                else -> when (stored.sitePermissions[site]) {
                    "allow" -> callback(true)
                    "deny" -> callback(false)
                    else -> { clipboardReadCallback = callback; clipboardReadSite = site }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 2.dp)) {
                AddressInput(value = input, onValueChange = { input = it }, onSubmit = { viewModel.load(input) }, onFocusChange = { inputFocused = it }, showSuggestionsPanel = false)
            }
        },
        bottomBar = {
            GlassSurface(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)) {
                IconButton(onClick = viewModel::back, enabled = selectedTab?.canGoBack == true) { Icon(Icons.Default.ArrowBack, "后退") }
                IconButton(onClick = viewModel::forward, enabled = selectedTab?.canGoForward == true) { Icon(Icons.Default.ArrowForward, "前进") }
                // 加载中时变为「停止」：避免用户在卡住的页面无法中断加载；加载完成后恢复为「刷新」
                if (selectedTab?.loading == true) {
                    IconButton(onClick = viewModel::stopLoading) { Icon(Icons.Default.Close, "停止加载") }
                } else {
                    IconButton(onClick = viewModel::reload) { Icon(Icons.Default.Refresh, "刷新") }
                }
                IconButton(onClick = { showNewTabDialog = true }) { Icon(Icons.Default.Add, "新建标签") }
                IconButton(onClick = { onTabs(selectedTab?.incognito == true) }) {
                    BadgedBox(badge = { Badge { Text(visibleTabs.size.toString()) } }) { Icon(Icons.Default.Tab, "标签") }
                }
                Box {
                    IconButton(onClick = { showBrowserMenu = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = showBrowserMenu, onDismissRequest = { showBrowserMenu = false }, offset = androidx.compose.ui.unit.DpOffset(0.dp, 48.dp)) {
                        DropdownMenuItem(text = { Text("电脑 UA 模式") }, leadingIcon = { if (selectedTab?.session?.settings?.userAgentOverride?.contains("X11") == true) Icon(Icons.Default.Check, null) else Icon(Icons.Default.Computer, null) }, onClick = { viewModel.toggleDesktopUserAgent(); showBrowserMenu = false })
                        DropdownMenuItem(
                            text = { Text(if (bookmarked) "删除书签" else "添加书签") },
                            leadingIcon = { Icon(if (bookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd, null) },
                            onClick = toggleBookmark
                        )
                        DropdownMenuItem(text = { Text("下载记录") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = { showBrowserMenu = false; onDownloads() })
                        DropdownMenuItem(text = { Text("浏览器设置") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { showBrowserMenu = false; onSettings() })
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                items(visibleTabs, key = { it.id }) { tab ->
                    AssistChip(
                        onClick = { viewModel.select(tab.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.title, maxLines = 1)
                                Icon(Icons.Default.Close, "关闭标签", Modifier.size(16.dp).clickable { viewModel.close(tab.id) })
                            }
                        },
                        leadingIcon = { if (tab.id == selectedId) Icon(Icons.Default.Check, null) }
                    )
                }
            }
            if (selectedTab != null) {
                if (selectedTab.incognito) {
                    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.small) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("无痕浏览：退出后将关闭全部无痕标签并清除 Cookie 与缓存，普通标签不受影响。", Modifier.weight(1f), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { viewModel.exitIncognito() }) { Text("退出无痕", color = MaterialTheme.colorScheme.inversePrimary) }
                        }
                    }
                }
                if (selectedTab.loading) Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    WavyProgressBar(progress = selectedTab.progress / 100f, modifier = Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    key(settings.hardwareAcceleration) {
                        AndroidView(
                            factory = { ctx ->
                                GeckoView(ctx).apply {
                                    // 开启硬件加速时保持 GeckoView 默认的 SurfaceView，
                                    // 不要显式调用 setViewBackend(SURFACE_VIEW)：
                                    // 该调用会 removeView/addView 重建渲染层，在 Compose 的
                                    // AndroidView 中会破坏渲染时序，导致网页黑屏。
                                    // 仅关闭时才切换 TextureView（必须在 setSession 之前）。
                                    if (!settings.hardwareAcceleration) {
                                        runCatching { setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW) }
                                    }
                                }
                            },
                            update = { view ->
                                view.setSession(selectedTab.session)
                                // 会话挂载后再触发待加载地址（外部链接），否则 loadUri 会被丢弃
                                view.post { viewModel.onSessionAttached(selectedTab.id) }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // 错误页盖在 GeckoView 之上：失败时页面本身已无内容，
                    // 但下层仍需保留渲染层，用户重试后无需重建视图。
                    // 必须写在 Box 内部才能使用 matchParentSize 铺满整个网页区域。
                    if (selectedTab.failed) {
                        PageErrorView(
                            url = selectedTab.failedUrl.ifBlank { selectedTab.url },
                            errorCode = selectedTab.errorCode,
                            errorCategory = selectedTab.errorCategory,
                            onRetry = { viewModel.reload() },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }
        }
    }

    if (showNewTabDialog) {
        AlertDialog(
            onDismissRequest = { showNewTabDialog = false },
            title = { Text("新建标签") },
            text = { Text("无痕标签不会保存浏览历史、Cookie 或缓存。") },
            confirmButton = { TextButton(onClick = { viewModel.newTab(); showNewTabDialog = false; onTabs(false) }) { Text("普通标签") } },
            dismissButton = { TextButton(onClick = { viewModel.newTab(incognito = true); showNewTabDialog = false; onTabs(true) }) { Text("无痕标签") } }
        )
    }
    if (downloadUrl != null) {
        DownloadDialog(
            url = downloadUrl!!,
            onDownload = { fileName, expected ->
                expectedHash = expected
                selectedFileName = fileName
                viewModel.enqueueDownload(downloadUrl!!, selectedFileName, expectedHash)
                Toast.makeText(context, "已开始下载，可前往下载记录或通知栏查看", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                downloadUrl = null
            },
            onDismiss = { downloadUrl = null }
        )
    }
    if (hashResult != null) {
        AlertDialog(
            onDismissRequest = { hashResult = null },
            title = { Text("APK 哈希校验") },
            text = { Text(hashResult!!) },
            confirmButton = { TextButton(onClick = { hashResult = null }) { Text("确定") } }
        )
    }
    if (pendingExternalAppUri != null) {
        AlertDialog(
            onDismissRequest = { pendingExternalAppUri = null; pendingExternalApp = null },
            title = { Text("打开外部应用？") },
            text = { Text("此链接将交给设备上的其它应用打开：\n${pendingExternalAppUri}") },
            confirmButton = { TextButton(onClick = { pendingExternalApp?.invoke(); pendingExternalAppUri = null; pendingExternalApp = null }) { Text("打开") } },
            dismissButton = { TextButton(onClick = { pendingExternalAppUri = null; pendingExternalApp = null }) { Text("取消") } }
        )
    }
    if (clipboardReadSite != null) {
        val site = clipboardReadSite!!
        val finishClipboard: (Boolean, Boolean) -> Unit = { allow, remember ->
            clipboardReadCallback?.invoke(allow)
            if (remember) scope.launch { settingsStore.setSitePermission(site, if (allow) "allow" else "deny") }
            clipboardReadSite = null
            clipboardReadCallback = null
        }
        AlertDialog(
            onDismissRequest = { finishClipboard(false, false) },
            title = { Text("允许读取剪贴板？") },
            text = { Text("${siteHost(site)} 请求读取你的剪贴板内容。") },
            confirmButton = { TextButton(onClick = { finishClipboard(true, true) }) { Text("始终允许") } },
            dismissButton = { TextButton(onClick = { finishClipboard(false, true) }) { Text("拒绝") } }
        )
    }
}

@Composable
private fun TabOverview(viewModel: BrowserViewModel, initialIncognito: Boolean = false, onBack: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    var incognitoOnly by remember { mutableStateOf(initialIncognito) }
    val displayedTabs = tabs.filter { it.incognito == incognitoOnly }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部标签") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { TextButton(onClick = { if (incognitoOnly) viewModel.closeAllIncognito() else viewModel.closeAllNormal() }) { Text(if (incognitoOnly) "关闭无痕" else "关闭普通") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row { AssistChip(onClick = { incognitoOnly = false }, label = { Text("普通标签") }, leadingIcon = { if (!incognitoOnly) Icon(Icons.Default.Check, null) }); Spacer(Modifier.width(8.dp)); AssistChip(onClick = { incognitoOnly = true }, label = { Text("无痕标签") }, leadingIcon = { if (incognitoOnly) Icon(Icons.Default.Check, null) }) } }
            if (displayedTabs.isEmpty()) item { Text(if (incognitoOnly) "暂无无痕标签" else "暂无普通标签") }
            items(displayedTabs, key = { it.id }) { tab ->
                ElevatedCard(onClick = { viewModel.select(tab.id); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Language, null) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.fillMaxWidth(0.72f)) {
                            Text(tab.title, maxLines = 1)
                            Text(tab.url.ifEmpty { "新标签页" }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.close(tab.id) }) { Icon(Icons.Default.Close, "关闭") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(viewModel: BrowserViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteFiles by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text(if (selectedIds.isEmpty()) "下载记录" else "已选 ${selectedIds.size} 项") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { if (selectedIds.isNotEmpty()) IconButton(onClick = { showDeleteConfirmation = true }) { Icon(Icons.Default.Delete, "删除选中项") } }) }) { padding ->
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("暂无下载记录") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextButton(onClick = { selectedIds = if (selectedIds.size == downloads.size) emptySet() else downloads.map { it.id }.toSet() }) { Text(if (selectedIds.size == downloads.size) "取消全选" else "全选") } }
                items(downloads, key = { it.id }) { item -> DownloadRecordCard(item, item.id in selectedIds, { selectedIds = selectedIds.toggle(item.id) }, { viewModel.cancelDownload(item.id) }, { openDownload(context, item) }, { showDeleteConfirmation = true; selectedIds = setOf(item.id) }) }
            }
        }
    }
    if (showDeleteConfirmation) AlertDialog(onDismissRequest = { showDeleteConfirmation = false }, title = { Text("删除下载记录？") }, text = { Column { Text("将删除 ${selectedIds.size} 条下载记录。") ; CheckRow("同时删除本地下载文件", deleteFiles) { deleteFiles = it } } }, confirmButton = { TextButton(onClick = { downloads.filter { it.id in selectedIds }.forEach { viewModel.deleteDownload(it.id, it.targetUri, deleteFiles) }; Toast.makeText(context, "已完成", Toast.LENGTH_SHORT).show(); selectedIds = emptySet(); showDeleteConfirmation = false }) { Text("删除") } }, dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") } })
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

@Composable
private fun HistoryScreen(viewModel: BrowserViewModel, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState(initial = emptyList())
    var showClearConfirmation by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("浏览历史") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { if (history.isNotEmpty()) TextButton(onClick = { showClearConfirmation = true }) { Text("清除") } }) }) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("暂无浏览历史") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.id }) { item ->
                    ElevatedCard(onClick = { onOpen(item.url) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title.ifBlank { item.url }, maxLines = 1)
                            Text(item.url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.visitedAt)), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    if (showClearConfirmation) {
        ConfirmDialog("清除浏览历史？", "此操作无法撤销。", {
            viewModel.clearHistory()
            Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
            showClearConfirmation = false
        }, { showClearConfirmation = false })
    }
}

private fun openUpdateApk(context: android.content.Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "未找到系统安装器", Toast.LENGTH_SHORT).show()
    }
}

private fun openDownload(context: android.content.Context, item: DownloadEntity) {
    val source = Uri.parse(item.targetUri)
    val uri = if (source.scheme == "file") FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(source.path.orEmpty())) else source
    val isApk = item.fileName.endsWith(".apk", true)
    val type = if (isApk) "application/vnd.android.package-archive" else when {
        item.fileName.endsWith(".pdf", true) -> "application/pdf"
        item.fileName.endsWith(".zip", true) -> "application/zip"
        else -> "application/octet-stream"
    }
    val action = Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
    action.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        if (isApk) {
            action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(action)
        } else {
            context.startActivity(Intent.createChooser(action, "打开文件"))
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "未找到可打开此文件的应用", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "请在系统设置中允许安装未知应用", Toast.LENGTH_SHORT).show()
    }
}

private fun copyDownloadLink(context: android.content.Context, url: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("下载链接", url))
}

@Composable
private fun DownloadRecordCard(item: DownloadEntity, selected: Boolean, onSelect: () -> Unit, onCancel: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val running = item.status == "queued" || item.status == "downloading"
    var menuExpanded by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = selected, onCheckedChange = { onSelect() })
            Column(Modifier.fillMaxWidth(0.78f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.fileName, style = MaterialTheme.typography.titleMedium)
                Text(downloadStatus(item), style = MaterialTheme.typography.bodySmall)
                if (running) {
                    WavyProgressBar(progress = item.progress / 100f, modifier = Modifier.fillMaxWidth())
                    Text("${item.progress}% · ${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}")
                    TextButton(onClick = onCancel) { Text("取消下载") }
                } else if (item.status == "completed" || item.status == "hash_mismatch") {
                    IconButton(onClick = onOpen) { Icon(Icons.Default.OpenInNew, "打开文件") }
                }
                if (item.errorMessage.isNotBlank()) Text(item.errorMessage, color = MaterialTheme.colorScheme.error)
                if (item.sha256.isNotBlank()) Text("SHA-256：${item.sha256}", style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "更多") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; onDelete() }) }
            }
        }
    }
}

private fun downloadStatus(item: DownloadEntity): String = when (item.status) {
    "queued" -> "等待下载"
    "downloading" -> "正在下载"
    "completed" -> "下载完成"
    "hash_mismatch" -> "哈希不匹配（文件可能损坏或被篡改）"
    "cancelled" -> "已取消"
    else -> "下载失败"
}

private fun formatBytes(value: Long): String = when {
    value < 0 -> "未知"
    value >= 1024 * 1024 -> "%.1f MB".format(value / 1024.0 / 1024.0)
    value >= 1024 -> "%.1f KB".format(value / 1024.0)
    else -> "$value B"
}

@Composable
private fun SettingsScreen(
    settings: BrowserSettings,
    isDefaultBrowser: Boolean,
    onSetDefaultBrowser: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onGeneralSettings: () -> Unit,
    onSiteSettings: () -> Unit,
    onCookieSettings: () -> Unit,
    onWebPermissions: () -> Unit,
    onAbout: () -> Unit,
    onContactUs: () -> Unit,
    onBack: () -> Unit
) {
    var showLinkDiagnostics by remember { mutableStateOf(false) }
    // 设置搜索：按关键词过滤上方入口卡片
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // 搜索索引：子选项 -> 所属页面。搜索命中后直接展示子选项，点击即跳转到对应页面。
    val searchIndex = remember {
        listOf(
            SettingSearchItem("主题", "通用设置", listOf("主题", "浅色", "深色", "夜间", "跟随系统", "亮色", "暗色"), onGeneralSettings),
            SettingSearchItem("动态莫奈取色", "通用设置", listOf("取色", "莫奈", "Monet", "动态颜色", "壁纸", "主题色"), onGeneralSettings),
            SettingSearchItem("硬件加速", "通用设置", listOf("硬件加速", "渲染", "性能", "GPU", "卡顿", "流畅", "掉帧", "加速"), onGeneralSettings),
            SettingSearchItem("下载记录", "通用设置", listOf("下载", "记录", "下载记录", "文件", "进度"), onGeneralSettings),
            SettingSearchItem("下载文件路径", "通用设置", listOf("路径", "保存位置", "存储", "目录", "下载位置", "下载路径"), onGeneralSettings),
            SettingSearchItem("APK SHA-256 校验", "通用设置", listOf("校验", "哈希", "SHA", "APK", "安全", "完整性", "sha256", "SHA256"), onGeneralSettings),
            SettingSearchItem("强制适配深色模式", "网站设置", listOf("深色", "黑暗", "夜间", "网页", "暗色", "适配", "网页渲染", "网页深色"), onSiteSettings),
            SettingSearchItem("搜索引擎", "网站设置", listOf("搜索", "引擎", "百度", "必应", "DuckDuckGo", "谷歌", "搜索引擎"), onSiteSettings),
            SettingSearchItem("浏览器标识", "网站设置", listOf("浏览器标识", "UA", "User-Agent", "桌面", "标识", "网页标识", "桌面模式"), onSiteSettings),
            SettingSearchItem("Cookie 追踪", "隐私", listOf("Cookie", "追踪", "隐私", "数据", "Cookies"), onCookieSettings),
            SettingSearchItem("清除缓存与 Cookie", "隐私", listOf("缓存", "清除", "Cookie", "数据", "清理", "站点数据"), onCookieSettings),
            SettingSearchItem("退出时清除浏览器数据", "隐私", listOf("退出", "清除", "数据", "无痕", "清理", "退出时清除"), onCookieSettings),
            SettingSearchItem("剪贴板读取权限", "网页权限管理", listOf("剪贴板", "复制", "粘贴", "权限", "读取"), onWebPermissions),
            SettingSearchItem("跳转至外部 APP 权限", "网页权限管理", listOf("外部", "跳转", "APP", "应用", "Intent", "权限"), onWebPermissions),
            SettingSearchItem("麦克风权限", "网页权限管理", listOf("麦克风", "录音", "权限", "语音", "音频"), onWebPermissions),
            SettingSearchItem("浏览历史", "浏览历史", listOf("历史", "访问", "记录", "足迹", "历史记录"), onHistory),
            SettingSearchItem("书签管理", "书签管理", listOf("书签", "收藏", "收藏夹", "书签管理", "首页"), onBookmarks),
            SettingSearchItem("设为默认浏览器", "设为默认浏览器", listOf("默认", "浏览器", "角色", "默认应用", "默认浏览器"), onSetDefaultBrowser, accent = true, subtitle = "选择 ZexBrowse 为系统默认浏览器，带来更好使用体验"),
            SettingSearchItem("外链诊断", "外链诊断", listOf("外链", "诊断", "Intent", "跳转", "链接"), { showLinkDiagnostics = true }),
            SettingSearchItem("关于 ZexBrowse", "关于 ZexBrowse", listOf("关于", "版本", "许可", "贡献者", "赞助", "Mirror", "CDK", "更新"), onAbout),
            SettingSearchItem("联系我们", "联系我们", listOf("联系", "反馈", "建议", "问题"), onContactUs)
        )
    }
    // 默认入口卡片（搜索框为空时展示，保持原有视觉）
    val entries = remember(isDefaultBrowser, settings.cookiesEnabled, settings.clearOnExit) {
        listOf(
            SettingsEntryItem("通用设置", "主题、动态取色、硬件加速、下载与保存路径", Icons.Rounded.Tune, onGeneralSettings),
            SettingsEntryItem("网站设置", "强制适配深色模式、搜索引擎、浏览器标识", Icons.Rounded.Language, onSiteSettings),
            SettingsEntryItem("隐私", cookieSettingsSubtitle(settings), Icons.Rounded.PrivacyTip, onCookieSettings),
            SettingsEntryItem("网页权限管理", "剪贴板读取、外部 APP 跳转、麦克风", Icons.Rounded.Security, onWebPermissions),
            SettingsEntryItem("浏览历史", "查看最近访问的网页", Icons.Default.History, onHistory),
            SettingsEntryItem("书签管理", "管理首页书签，长按可删除", Icons.Rounded.Bookmarks, onBookmarks),
            SettingsEntryItem("设为默认浏览器", "选择 ZexBrowse 为系统默认浏览器，带来更好使用体验", Icons.Rounded.Public, onSetDefaultBrowser, accent = true),
            SettingsEntryItem("外链诊断", "查看最近一次外部跳转传入的原始信息", Icons.Default.Language, { showLinkDiagnostics = true }),
            SettingsEntryItem("关于 ZexBrowse", "版本、许可、贡献者与赞助支持", Icons.Default.Info, onAbout),
            SettingsEntryItem("联系我们", "反馈问题与交流建议", Icons.Rounded.ContactSupport, onContactUs)
        ).filter { !it.accent || !isDefaultBrowser }
    }
    // 搜索命中结果（仅在搜索框有输入时使用）
    val filtered = remember(searchQuery, isDefaultBrowser) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else searchIndex.filter { item ->
            (!item.accent || !isDefaultBrowser) &&
                (item.title.contains(q, ignoreCase = true) || item.keywords.any { it.contains(q, ignoreCase = true) })
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // 设置搜索框
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("搜索设置项") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, "清空") }
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            if (searchQuery.isBlank()) {
                // 默认展示入口卡片
                entries.forEach { entry ->
                    item {
                        SettingsEntry(
                            title = entry.title,
                            subtitle = entry.subtitle,
                            icon = entry.icon,
                            onClick = entry.onClick,
                            accent = entry.accent
                        )
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "未找到匹配的设置项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)
                    )
                }
            } else {
                filtered.forEach { entry ->
                    item {
                        SettingsEntry(
                            title = entry.title,
                            subtitle = entry.subtitle ?: entry.group,
                            icon = settingsEntryIcon(entry.group),
                            onClick = entry.onClick,
                            accent = entry.accent
                        )
                    }
                }
            }
        }
    }
    if (showLinkDiagnostics) {
        val info by ExternalLinkDiagnostics.last.collectAsState()
        AlertDialog(
            onDismissRequest = { showLinkDiagnostics = false },
            title = { Text("外链诊断") },
            text = { Text(if (info.isBlank()) "尚未收到任何外部跳转。请从其它应用打开一个链接后再回来查看。" else info) },
            confirmButton = { TextButton(onClick = { showLinkDiagnostics = false }) { Text("关闭") } }
        )
    }
}

/** 隐私入口副标题：概括当前各项开关状态 */
private fun cookieSettingsSubtitle(settings: BrowserSettings): String {
    val cookieState = if (settings.cookiesEnabled) "Cookie 追踪已开启" else "Cookie 追踪已关闭"
    val exitState = if (settings.clearOnExit) "退出时自动清除" else "退出时保留数据"
    return "$cookieState · $exitState"
}

/** 设置入口卡片的图标（按所属页面回填，保持与原有视觉一致） */
private fun settingsEntryIcon(title: String): androidx.compose.ui.graphics.vector.ImageVector = when (title) {
    "通用设置" -> Icons.Rounded.Tune
    "网站设置" -> Icons.Rounded.Language
    "隐私" -> Icons.Rounded.PrivacyTip
    "网页权限管理" -> Icons.Rounded.Security
    "浏览历史" -> Icons.Default.History
    "书签管理" -> Icons.Rounded.Bookmarks
    "设为默认浏览器" -> Icons.Rounded.Public
    "外链诊断" -> Icons.Default.Language
    "关于 ZexBrowse" -> Icons.Default.Info
    "联系我们" -> Icons.Rounded.ContactSupport
    else -> Icons.Default.Settings
}

/**
 * 设置页合并入口项：带图标小方块背景的卡片。
 *
 * 水波纹说明：使用 [Card] 的 onClick 重载，水波纹由卡片自身的 [shape] 裁剪，
 * 与实际卡片形状完全一致；若改用 Modifier.clickable，水波纹会溢出圆角边界。
 */@Composable
private fun SettingsEntry(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp, accent: Boolean = false) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        // accent 仅用于需要突出的单项入口（如「设为默认浏览器」）；其余入口保持默认配色
        colors = if (accent) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = if (accent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (accent) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (accent) 0.75f else 1f), maxLines = 2)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (accent) 0.75f else 1f))
        }
    }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { changed(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = changed)
        Text(title)
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = confirm) { Text("确定") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

private fun searchEngineLabel(settings: BrowserSettings) = when (settings.searchEngine) {
    "baidu" -> "百度"
    "bing" -> "必应"
    "duckduckgo" -> "DuckDuckGo"
    "custom" -> settings.customSearchTitle
    else -> "Google"
}

private fun userAgentLabel(settings: BrowserSettings) = when (settings.userAgentMode) {
    "android_chrome" -> "Android Chrome"
    "desktop_chrome" -> "桌面 Chrome"
    "desktop_firefox" -> "桌面 Firefox"
    "custom" -> settings.customUserAgentTitle
    else -> "默认 GeckoView"
}

@Composable
private fun DownloadDirectoryScreen(settings: BrowserSettings, onSave: (String, String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var externalUri by remember(settings.externalDownloadTreeUri) { mutableStateOf(settings.externalDownloadTreeUri) }
    var externalPath by remember(settings.externalDownloadDisplayPath) { mutableStateOf(settings.externalDownloadDisplayPath) }
    var useExternal by remember(settings.downloadDirectoryMode) { mutableStateOf(settings.downloadDirectoryMode == "external") }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            externalUri = uri.toString()
            externalPath = uri.path?.substringAfterLast(":")?.let { "/storage/emulated/0/$it/" } ?: ""
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("下载文件路径") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = {
        if (!useExternal || externalUri.isNotBlank()) {
            TextButton(onClick = { onSave(if (useExternal) "external" else "internal", externalUri, externalPath) }) { Text("确定") }
        }
    }) }) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChoiceItem("内部应用目录", !useExternal) { useExternal = false }
            Text("内部目录不可修改，卸载应用后会一并清理。", style = MaterialTheme.typography.bodySmall)
            ChoiceItem("外部目录", useExternal) { useExternal = true }
            Text(externalPath.ifEmpty { "未设置（请点击下方按钮选择目录）" }, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = useExternal, onClick = { treeLauncher.launch(null) }) { Text("修改外部目录") }
            Text("选择目录时将由系统授予访问权限，支持任意可授权的外部目录。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SearchEngineScreen(settings: BrowserSettings, onSelect: (String) -> Unit, onCustom: () -> Unit, onBack: () -> Unit) {
    val engines = listOf("google" to "Google", "baidu" to "百度", "bing" to "必应", "duckduckgo" to "DuckDuckGo")
    SelectionScreen("搜索引擎", onBack) {
        engines.forEach { (value, title) -> item { ChoiceItem(title, settings.searchEngine == value) { onSelect(value) } } }
        item { ListItem(headlineContent = { Text(settings.customSearchTitle) }, supportingContent = { Text("自定义搜索 URL") }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onCustom)) }
    }
}

@Composable
private fun CustomSearchScreen(settings: BrowserSettings, onSave: (String, String) -> Unit, onBack: () -> Unit) {
    EditorScreen("自定义搜索引擎", settings.customSearchTitle, settings.customSearchUrl, "搜索 URL（使用 {query} 作为关键词）", onSave, onBack)
}

@Composable
private fun UserAgentScreen(settings: BrowserSettings, onSelect: (String) -> Unit, onSimplified: (Boolean) -> Unit, onCustom: () -> Unit, onBack: () -> Unit) {
    val agents = listOf("geckoview" to "默认 GeckoView", "android_chrome" to "Android Chrome", "desktop_chrome" to "桌面 Chrome", "desktop_firefox" to "桌面 Firefox")
    SelectionScreen("浏览器标识", onBack) {
        agents.forEach { (value, title) -> item { ChoiceItem(title, settings.userAgentMode == value) { onSelect(value) } } }
        item { SwitchRow("简化浏览器标识", "减少 User-Agent 中的设备信息", settings.simplifiedUserAgent, onSimplified, leading = Icons.Default.Computer) }
        item { ListItem(headlineContent = { Text(settings.customUserAgentTitle) }, supportingContent = { Text("自定义浏览器标识") }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onCustom)) }
    }
}

@Composable
private fun CustomUserAgentScreen(settings: BrowserSettings, onSave: (String, String) -> Unit, onBack: () -> Unit) {
    EditorScreen("自定义浏览器标识", settings.customUserAgentTitle, settings.customUserAgent, "浏览器标识内容", onSave, onBack)
}

@Composable
private fun ClipboardPermissionScreen(settings: BrowserSettings, onSelect: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    SelectionScreen("剪贴板读取权限", onBack) {
        item { ChoiceItem("每次询问（弹窗并记住选择）", settings.clipboardReadPolicy == "ask") { onSelect("ask") } }
        item { ChoiceItem("始终允许", settings.clipboardReadPolicy == "allow") { onSelect("allow") } }
        item { ChoiceItem("始终拒绝", settings.clipboardReadPolicy == "deny") { onSelect("deny") } }
        // 说明文字：上下留白，避免贴到上方选项与下方按钮的边缘
        item { Text("网站请求读取剪贴板时：始终允许/拒绝将直接放行或拦截；每次询问则会弹出提示，并将你的选择按网站永久记住。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
        item { Spacer(Modifier.height(24.dp)) }
        item { OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("清除已记住的网站权限") } }
    }
}

/**
 * 通用权限策略选择页（麦克风 / 外部 APP 跳转）。
 *
 * 这些策略会实时作用于对应权限回调（见 BrowserViewModel 中的 policy 判断），
 * 不是仅保存设置的「摆设」。
 */
@Composable
private fun SimplePolicyScreen(
    title: String,
    description: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    SelectionScreen(title, onBack) {
        options.forEach { (value, label) ->
            item { ChoiceItem(label, current == value) { onSelect(value) } }
        }
        item { Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
    }
}

/** 通用设置：主题设置（主题 / 动态取色）与下载设置的合并页 */
@Composable
private fun GeneralSettingsScreen(
    settings: BrowserSettings,
    setMode: (String) -> Unit,
    setDynamic: (Boolean) -> Unit,
    setApkHash: (Boolean) -> Unit,
    setHardwareAcceleration: (Boolean) -> Unit,
    onDownloads: () -> Unit,
    onDownloadDirectory: () -> Unit,
    onBack: () -> Unit
) {
    SelectionScreen("通用设置", onBack) {
        item { SectionHeader("主题设置") }
        item { ThemeSelector(settings.darkMode, setMode) }
        item { SwitchRow("动态莫奈取色", "使用系统动态颜色", settings.dynamicColor, setDynamic, leading = Icons.Rounded.Palette) }
        item { SectionHeader("性能") }
        item { SwitchRow("硬件加速", "使用 GPU 合成图层（SurfaceView）提升滚动与动画流畅度；部分机型开启后耗电或发热较高，可关闭改用兼容渲染", settings.hardwareAcceleration, setHardwareAcceleration, leading = Icons.Rounded.Speed) }
        item { SectionHeader("下载设置") }
        item { ListItem(headlineContent = { Text("下载记录") }, supportingContent = { Text("查看下载进度与历史") }, leadingContent = { Icon(Icons.Default.Download, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onDownloads)) }
        item { ListItem(headlineContent = { Text("下载文件路径") }, supportingContent = { Text(downloadDirectoryLabel(settings)) }, leadingContent = { Icon(Icons.Rounded.Folder, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onDownloadDirectory)) }
        item { SwitchRow("APK SHA-256 校验", "下载 APK 时计算哈希值", settings.apkHashEnabled, setApkHash, leading = Icons.Rounded.VerifiedUser) }
    }
}

/** 设置页内的分组标题 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun downloadDirectoryLabel(settings: BrowserSettings): String = when {
    settings.downloadDirectoryMode == "external" && settings.externalDownloadDisplayPath.isBlank() -> "外部目录（未设置）"
    settings.downloadDirectoryMode == "external" -> settings.externalDownloadDisplayPath
    else -> "内部应用目录"
}

/**
 * 网站设置：把原「强制适配深色模式」（属于网页渲染）与
 * 「搜索引擎」「浏览器标识」合并到同一页。
 */
@Composable
private fun WebSiteSettingsScreen(
    settings: BrowserSettings,
    setForceDarkWeb: (Boolean) -> Unit,
    onSearchEngine: () -> Unit,
    onUserAgent: () -> Unit,
    onBack: () -> Unit
) {
    SelectionScreen("网站设置", onBack) {
        item { SectionHeader("网页渲染") }
        item { SwitchRow("强制适配深色模式", "开启后强制暗化未适配网页去除白底；关闭后网页按自身与系统偏好渲染", settings.forceDarkWeb, setForceDarkWeb, leading = Icons.Rounded.DarkMode) }
        item { SectionHeader("搜索与浏览") }
        item { ListItem(headlineContent = { Text("搜索引擎") }, supportingContent = { Text(searchEngineLabel(settings)) }, leadingContent = { Icon(Icons.Rounded.ManageSearch, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onSearchEngine)) }
        item { ListItem(headlineContent = { Text("浏览器标识") }, supportingContent = { Text(userAgentLabel(settings)) }, leadingContent = { Icon(Icons.Default.Computer, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onUserAgent)) }
    }
}

/** 隐私（原 Cookie 设置）：Cookie 追踪 / 清除缓存与 Cookie / 退出时清除浏览器数据 */
@Composable
private fun CookieSettingsScreen(
    settings: BrowserSettings,
    setCookie: (Boolean) -> Unit,
    setClearOnExit: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    clearBrowserData: () -> Unit,
    onBack: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showExitOptions by remember { mutableStateOf(false) }
    var exitCookies by remember(settings.clearCookiesOnExit) { mutableStateOf(settings.clearCookiesOnExit) }
    var exitCache by remember(settings.clearCacheOnExit) { mutableStateOf(settings.clearCacheOnExit) }
    var exitHistory by remember(settings.clearHistoryOnExit) { mutableStateOf(settings.clearHistoryOnExit) }
    SelectionScreen("隐私", onBack) {
        item { SwitchRow("Cookie 追踪", "关闭后新会话不保存 Cookie", settings.cookiesEnabled, setCookie, leading = Icons.Rounded.Cookie) }
        item { ListItem(headlineContent = { Text("清除缓存与 Cookie") }, supportingContent = { Text("立即清除缓存、Cookie 和浏览历史") }, leadingContent = { Icon(Icons.Default.Delete, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable { showClearConfirmation = true }) }
        item { SwitchRow("退出时清除浏览器数据", "离开应用后清除缓存、Cookie 与历史记录", settings.clearOnExit, { showExitOptions = true }, leading = Icons.Default.Delete) }
    }
    if (showClearConfirmation) ConfirmDialog("清除浏览器数据？", "将清除缓存、Cookie 和浏览历史，此操作无法撤销。", {
        clearBrowserData(); showClearConfirmation = false
    }, { showClearConfirmation = false })
    if (showExitOptions) {
        AlertDialog(
            onDismissRequest = { showExitOptions = false },
            title = { Text("退出时清除浏览器数据") },
            text = {
                Column {
                    CheckRow("Cookie 与站点数据", exitCookies) { exitCookies = it }
                    CheckRow("网页缓存", exitCache) { exitCache = it }
                    CheckRow("浏览历史", exitHistory) { exitHistory = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    setClearOnExit(exitCookies || exitCache || exitHistory, exitCookies, exitCache, exitHistory)
                    showExitOptions = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExitOptions = false }) { Text("取消") } }
        )
    }
}

/** 网页权限管理：剪贴板 / 跳转至外部 APP / 麦克风 */
@Composable
private fun WebPermissionsScreen(
    settings: BrowserSettings,
    onClipboard: () -> Unit,
    onExternalApp: () -> Unit,
    onMic: () -> Unit,
    onBack: () -> Unit
) {
    SelectionScreen("网页权限管理", onBack) {
        item { ListItem(headlineContent = { Text("剪贴板读取权限") }, supportingContent = { Text(clipboardPolicyLabel(settings.clipboardReadPolicy)) }, leadingContent = { Icon(Icons.Rounded.ContentPaste, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onClipboard)) }
        item { ListItem(headlineContent = { Text("跳转至外部 APP 权限") }, supportingContent = { Text(policyLabel(settings.externalAppPolicy)) }, leadingContent = { Icon(Icons.Default.OpenInNew, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onExternalApp)) }
        item { ListItem(headlineContent = { Text("麦克风权限") }, supportingContent = { Text(policyLabel(settings.micPolicy)) }, leadingContent = { Icon(Icons.Rounded.Mic, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onMic)) }
        item { Text("这些权限会实时作用于网页请求：始终允许/拒绝直接放行或拦截，每次询问则先征求你的意见。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
    }
}

private fun policyLabel(policy: String) = when (policy) {
    "allow" -> "始终允许"
    "deny" -> "始终拒绝"
    else -> "每次询问"
}

private fun clipboardPolicyLabel(policy: String) = when (policy) {
    "allow" -> "始终允许"
    "deny" -> "始终拒绝"
    else -> "每次询问"
}

private fun siteHost(uri: String): String = runCatching { java.net.URI(uri).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri

/** 在 URL 上追加查询参数：已带查询串时用 & 拼接，解析失败时原样返回 */
private fun String.suffixQuery(pair: Pair<String, String>): String = runCatching {
    val uri = java.net.URI(this)
    val separator = if (uri.rawQuery.isNullOrEmpty()) "?" else "&"
    "$this$separator${pair.first}=${pair.second}"
}.getOrDefault(this)

private fun hasRecordAudioPermission(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

/**
 * 撤销系统录音授权，使 Android 设置同步为「不允许」。
 * 仅 Android 13 及以上支持；系统会在合适时机异步执行撤销，届时应用进程会被结束。
 */
private fun revokeRecordAudioPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= 33 && hasRecordAudioPermission(context)) {
        runCatching { context.revokeSelfPermissionOnKill(Manifest.permission.RECORD_AUDIO) }
    }
}

/**
 * 麦克风权限设置页。
 *
 * 三档策略直接作用于网页请求；同时把授权状态同步到系统：选「始终允许」时申请一次系统录音权限，
 * 选「始终拒绝」时撤销已授予的系统录音权限（Android 13 及以上）。
 */
@Composable
private fun MicPermissionScreen(
    settings: BrowserSettings,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 用户可能去系统设置里改动授权，回到本页时重新读取
    var osGranted by remember { mutableStateOf(hasRecordAudioPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) osGranted = hasRecordAudioPermission(context) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SelectionScreen("麦克风权限", onBack) {
        item { ChoiceItem("每次询问（弹窗并记住选择）", settings.micPolicy == "ask") { onSelect("ask") } }
        item { ChoiceItem("始终允许", settings.micPolicy == "allow") { onSelect("allow") } }
        item { ChoiceItem("始终拒绝", settings.micPolicy == "deny") { onSelect("deny") } }
        item { Text("网页请求使用麦克风（如语音搜索、语音输入）时：始终允许/拒绝直接放行或拦截；每次询问则先弹窗并将你的选择按网站记住。选择「始终拒绝」会同时撤销系统的录音授权（Android 13 及以上，系统在合适时机执行，届时应用可能重启）。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            ListItem(
                headlineContent = { Text("系统录音权限") },
                supportingContent = { Text(if (osGranted) "已允许" else "未允许") },
                leadingContent = { Icon(Icons.Rounded.Mic, null) },
                trailingContent = {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show() }
                    }) { Text("系统设置") }
                }
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text("清除已记住的网站权限") } }
    }
}

@Composable
private fun SelectionScreen(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), content = content) }
}

@Composable
private fun ChoiceItem(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, leadingContent = { if (selected) Icon(Icons.Default.Check, null) }, modifier = Modifier.clickable(onClick = onClick))
}

@Composable
private fun EditorScreen(title: String, initialTitle: String, initialValue: String, valueLabel: String, onSave: (String, String) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable(initialTitle) { mutableStateOf(initialTitle) }
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { TextButton(enabled = name.isNotBlank() && value.isNotBlank(), onClick = { onSave(name.trim(), value.trim()) }) { Text("保存") } }) }) { padding ->
        // 多行输入框在小屏下会顶到底部，给整页滚动能力
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("标题") }, singleLine = true)
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text(valueLabel) }, minLines = 4)
        }
    }
}

@Composable
private fun ThemeSelector(mode: String, setMode: (String) -> Unit) {
    val labels = linkedMapOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色")
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("主题") },
        supportingContent = { Text("跟随系统、浅色或深色") },
        leadingContent = { Icon(Icons.Rounded.Palette, null) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(labels[mode] ?: "跟随系统") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    labels.forEach { (value, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { setMode(value); expanded = false }) }
                }
            }
        }
    )
}

@Composable
private fun UserAgentSelector(mode: String, labels: Map<String, String>, setMode: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("浏览器标识") },
        supportingContent = { Text("更改后仅新建标签页生效") },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(labels[mode] ?: "默认 GeckoView") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    labels.forEach { (value, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { setMode(value); expanded = false }) }
                }
            }
        }
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, leading: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = leading?.let { icon -> { Icon(icon, null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun DownloadDialog(url: String, onDownload: (String, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isApk = url.substringBefore('?').endsWith(".apk", ignoreCase = true)
    var expectedHash by rememberSaveable { mutableStateOf("") }
    val fileName = url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载文件") },
        text = {
            Column {
                Text(fileName)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { copyDownloadLink(context, url); Toast.makeText(context, "已复制下载链接", Toast.LENGTH_SHORT).show() }) { Text("复制下载链接") }
                if (isApk) {
                    Spacer(Modifier.height(12.dp))
                    Text("APK 下载完成后将自动计算 SHA-256。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(expectedHash, { expectedHash = it }, label = { Text("官方 SHA-256（可选）") }, supportingText = { Text("下载后会显示匹配或不匹配结果") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDownload(fileName, expectedHash) }) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("下载") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// 全局统一的波浪进度条：波长 24dp，振幅沿用官方规则（进度 10% 以下、95% 以上收平为直线，中段才起波），
// 并对进度变化做平滑过渡，避免每次进度跳变时动效中断产生停顿感（LinearWavyProgressIndicator 默认不做进度过渡动画）
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WavyProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "wavyProgress"
    )
    LinearWavyProgressIndicator(progress = { animated }, modifier = modifier, wavelength = 24.dp)
}

/**
 * 「Mirror 酱高速下载」卡片。
 *
 * 平台方（Mirror 酱商务）建议新项目先稳定运行一段时间后再接入，
 * 因此在功能开放前展示为「熄灭」状态：整条卡片暗淡、不可点击、无水波纹，
 * 副标题标注「暂不可用」，也不展示无法使用的 CDK 输入。
 * 待平台确认接入后，把构建开关打开即自动恢复为正常可点击状态（无需改代码）。
 *
 * 水波纹说明：使用 [Card] 的 onClick 重载，水波纹与卡片圆角形状一致；
 * 熄灭态下 enabled = false，不响应点击也不显示水波纹。
 */
@Composable
private fun MirrorChyanCard(cdk: String, setCdk: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available = MirrorChyan.isAvailable
    var showInfo by remember { mutableStateOf(false) }
    var draft by remember(cdk) { mutableStateOf(cdk) }
    var verifying by remember { mutableStateOf(false) }
    var verdict by remember { mutableStateOf<String?>(null) }
    val shape = MaterialTheme.shapes.large
    // 熄灭态统一使用 38% 不透明度（Material disabled 标准值），
    // 包括卡片内容区里用显式颜色的图标方块、副标题与箭头，
    // 否则会出现「卡片变暗但文字/图标仍然高亮」的漏光感。
    val disabledAlpha = 0.38f
    val iconContainer = if (available) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = disabledAlpha)
    val iconTint = if (available) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)
    val subtitleColor = if (available) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)
    val arrowColor = if (available) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)
    // 卡片本身也显式给出禁用配色（不依赖 CardDefaults 的默认值），
    // 与上面各子项使用同一套 38% 不透明度，整体看起来才是均匀的「熄灭」而不是斑驳的灰。
    val cardColors = if (available) CardDefaults.cardColors() else CardDefaults.cardColors(
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = disabledAlpha),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
    )
    Card(
        onClick = { showInfo = true },
        modifier = Modifier.fillMaxWidth(),
        enabled = available,
        colors = cardColors,
        shape = shape
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = iconContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Speed, null, tint = iconTint) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Mirror 酱高速下载", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !available -> "暂不可用"
                        cdk.isBlank() -> "未填写激活密钥（CDK）"
                        else -> "已填写激活密钥（CDK）"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
            Icon(Icons.Default.ArrowForward, null, tint = arrowColor)
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Mirror 酱高速下载")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Mirror 酱是国内第三方应用分发平台，为开源项目提供高速 CDN 镜像，可免梯下载最新安装包。", style = MaterialTheme.typography.bodyMedium)
                    Text("在 Mirror 酱项目页获取激活密钥后填入下方，更新与下载即可使用高速通道；不填也可正常检查更新，下载会自动回退到 GitHub。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.trim(); verdict = null },
                        label = { Text("激活密钥（CDK）") },
                        placeholder = { Text("选填") },
                        singleLine = true,
                        enabled = available && !verifying,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (available) {
                        OutlinedButton(
                            enabled = !verifying,
                            onClick = {
                                if (draft.isBlank()) {
                                    verdict = "请先填写激活密钥"
                                } else {
                                    verifying = true
                                    verdict = null
                                    scope.launch {
                                        val result = runCatching { UpdateChecker().verifyCdk(draft) }.getOrNull()
                                        verdict = when {
                                            result == null -> "验证失败：网络异常或服务未响应，请稍后重试"
                                            result.isBlank() -> "密钥有效，可使用高速下载"
                                            else -> result
                                        }
                                        verifying = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (verifying) "正在验证…" else "验证密钥")
                        }
                    }
                    verdict?.let { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (text.startsWith("密钥有效")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Text("密钥仅保存在本机，不会随代码或日志上传。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Mirror 酱为付费第三方服务，与本项目相互独立；未获取 CDK 时仍可在项目页查看版本信息。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    setCdk(draft)
                    showInfo = false
                    val url = MirrorChyan.projectUrl(source = "zexbrowse_app_settings")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show() }
                }) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text("保存并前往 Mirror 酱") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { setCdk(draft); showInfo = false }) { Text("仅保存") }
                    TextButton(onClick = { showInfo = false }) { Text("取消") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutScreen(
    settings: BrowserSettings,
    setAutoCheckUpdates: (Boolean) -> Unit,
    setMirrorCdk: (String) -> Unit,
    setPreferMirrorDownload: (Boolean) -> Unit,
    onSponsor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<ReleaseInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var contributors by remember { mutableStateOf<List<com.zex.zexbrowse.data.Contributor>?>(null) }
    var contributorsLoading by remember { mutableStateOf(false) }
    // 加载请求令牌：用于丢弃被用户提前关闭弹窗后的过期结果，避免弹窗被加载结果重新弹回
    var contributorsRequest by remember { mutableStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("关于") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding ->
        // 条目较多，在窄屏/横屏/大字体下会超出一屏，必须可滚动，否则底部内容点不到
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("ZexBrowse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
            Text("本项目采用 Mozilla Public License 2.0 (MPL-2.0) 开源。GeckoView 及其相关组件遵循 Mozilla 的相应开源许可。Jetpack Compose、Material 3 和 AndroidX 库遵循各自许可证。")
            FilledTonalButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BaiXiaoTao520/ZexBrowse"))) }) {
                Icon(Icons.Default.OpenInNew, null)
                Spacer(Modifier.width(8.dp))
                Text("GitHub 仓库")
            }
            OutlinedButton(onClick = {
                val request = ++contributorsRequest
                contributors = null
                contributorsLoading = true
                scope.launch {
                    val result = runCatching { UpdateChecker().contributors() }.getOrElse { emptyList() }
                    if (contributorsRequest == request) {
                        contributors = result
                        contributorsLoading = false
                    }
                }
            }) { Text("贡献者") }
            SwitchRow("启动时自动检查更新", "默认开启", settings.autoCheckUpdates, setAutoCheckUpdates, leading = Icons.Default.SystemUpdate)
            SwitchRow("优先使用高速下载通道", "默认关闭；开启且高速通道可用时优先走它，否则始终走 GitHub 直链", settings.preferMirrorDownload, setPreferMirrorDownload, leading = Icons.Rounded.Speed)
            // Mirror 酱高速下载：卡片式入口，内含 CDK 输入与说明
            MirrorChyanCard(cdk = settings.mirrorCdk, setCdk = setMirrorCdk)
            // 赞助与支持：与其它入口保持相同的卡片样式
            SettingsEntry(
                title = "赞助与支持",
                subtitle = "支持作者与后续开发维护",
                icon = Icons.Rounded.VolunteerActivism,
                onClick = onSponsor,
                // 本页其余控件都基于 24dp 内边距，卡片不再额外内缩，保持左边缘对齐
                horizontalPadding = 0.dp
            )
            OutlinedButton(
                onClick = {
                    checking = true
                    scope.launch {
                        val checker = UpdateChecker(cdk = settings.mirrorCdk)
                        runCatching { checker.latest() }
                            .onSuccess { release ->
                                if (release != null && checker.isNewer(release.version)) {
                                    update = release
                                } else {
                                    Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .onFailure { error -> message = "更新检查失败：${error.message ?: "请稍后重试"}" }
                        checking = false
                    }
                },
                enabled = !checking
            ) { Text(if (checking) "正在检查…" else "检查更新") }
        }
    }

    val dismissContributors = { contributorsRequest++; contributors = null; contributorsLoading = false }
    if (contributors != null || contributorsLoading) {
        AlertDialog(
            onDismissRequest = dismissContributors,
            title = { Text("贡献者") },
            text = {
                val list = contributors
                if (list == null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(modifier = Modifier.size(48.dp), polygons = listOf(MaterialShapes.Clover4Leaf, MaterialShapes.Flower, MaterialShapes.Burst, MaterialShapes.Diamond))
                    }
                } else if (list.isEmpty()) {
                    Text("暂无贡献者信息")
                } else {
                    LazyColumn { items(list) { contributor ->
                        val homepage = "https://github.com/${contributor.name}"
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { openInBrowser(context, homepage) }.padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(model = contributor.avatarUrl, contentDescription = contributor.name, modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(contributor.name)
                                Text("${contributor.contributions} 次提交", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } }
                }
            },
            confirmButton = { TextButton(onClick = dismissContributors) { Text("确定") } }
        )
    }

    update?.let { release -> UpdateDialog(release = release, preferMirror = settings.preferMirrorDownload, onDismiss = { update = null }) }
    message?.let { content ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("更新") },
            text = { Text(content) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }
        )
    }
}

/** 赞助与支持说明弹窗：自愿原则，引导前往仓库 README 查看赞赏码 */
@Composable
private fun SponsorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolunteerActivism, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("赞助与支持")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("如果 ZexBrowse 用起来顺手，欢迎请作者喝杯咖啡。")
                Text("ZexBrowse 由个人在业余时间开发维护，持续免费、无广告，也不上架任何应用商店。你的支持会直接用于服务器与带宽开销、测试设备，以及后续功能的开发与维护。")
                Text("赞助完全出于自愿，金额不限；不赞助也完全不影响任何功能的使用。")
                Text("如何支持", style = MaterialTheme.typography.titleSmall)
                Text("1. 点击下方「前往赞赏」，跳转到项目 README。")
                Text("2. 在页面中向下滑动，找到「赞助与支持」中的赞赏码图片。")
                Text("3. 长按保存该图片（或截图保存），再打开微信「扫一扫」，从相册选择它即可完成。")
                Text("无论是否赞助，感谢你使用 ZexBrowse；遇到问题欢迎在 GitHub 仓库反馈，帮助它变得更好。")
            }
        },
        confirmButton = {
            Button(onClick = { openInBrowser(context, SPONSOR_PAGE_URL); onDismiss() }) {
                Icon(Icons.Rounded.VolunteerActivism, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("前往赞赏")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateDialog(release: ReleaseInfo, preferMirror: Boolean, onDismiss: () -> Unit) {
    // 用户点了「下载」后切换到独立下载窗口；为空表示仍在简介窗口
    var stage by remember { mutableStateOf<ReleaseInfo?>(null) }

    val current = stage
    if (current == null) {
        UpdateIntroDialog(release = release, preferMirror = preferMirror, onStartDownload = { stage = release }, onDismiss = onDismiss)
    } else {
        UpdateDownloadDialog(release = current, preferMirror = preferMirror, onBack = onDismiss)
    }
}

/** 发现新版本的简介窗口：只负责展示更新说明并提供下载入口 */
@Composable
private fun UpdateIntroDialog(release: ReleaseInfo, preferMirror: Boolean, onStartDownload: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // 应用内下载：仅在 Mirror 酱可用且拿到直链时优先用之，否则回退 GitHub
    val mirrorUrl = if (MirrorChyan.isAvailable && preferMirror) release.mirrorDownloadUrl?.takeIf { it.isNotBlank() } else null
    val effectiveUrl = mirrorUrl ?: release.downloadUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, "升级更新", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("发现新版本 ${release.version}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(release.notes.ifBlank { "暂无更新说明" })
                }
                // 下载通道标注：让用户知道即将走哪条通道，以及为什么
                Text(
                    if (mirrorUrl != null) "下载通道：Mirror 酱高速通道" else "下载通道：GitHub 直链",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Mirror 酱返回的提示（如 CDK 失效、次数用完），让用户知道回退原因
                release.mirrorMessage?.takeIf { it.isNotBlank() }?.let { hint ->
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (effectiveUrl.isBlank()) {
                    Text("未获取到可用的下载地址，可稍后重试或前往项目页手动下载。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = effectiveUrl.isNotBlank(),
                onClick = onStartDownload
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(6.dp))
                Text("下载")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mirror酱高速下载：跳转浏览器打开。
                // 功能未开放时保持熄灭状态；可用时优先用返回的带时效直链；未填 CDK 时打开项目页。
                val mirrorReady = MirrorChyan.isAvailable
                val browserUrl = if (mirrorReady) {
                    release.mirrorDownloadUrl?.takeIf { it.isNotBlank() } ?: release.mirrorUrl
                } else null
                TextButton(
                    enabled = mirrorReady && browserUrl != null,
                    // 权重 + fill=false：宽度不够时标签可换行，不会把「取消」挤出弹窗
                    modifier = Modifier.weight(1f, fill = false),
                    onClick = { browserUrl?.let { target -> openInBrowser(context, target) } }
                ) {
                    Icon(Icons.Rounded.Speed, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (mirrorReady) "Mirror酱高速下载" else "Mirror酱高速下载（暂不可用）",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

/**
 * 独立下载窗口：下载中只提供取消；失败提供取消与重试；成功提供取消与立即安装。
 *
 * 取消时会停止下载并同步删除已写入的临时文件与同名 APK，避免残留。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateDownloadDialog(release: ReleaseInfo, preferMirror: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileName = "ZexBrowse-${release.version}.apk"
    val mirrorUrl = if (MirrorChyan.isAvailable && preferMirror) release.mirrorDownloadUrl?.takeIf { it.isNotBlank() } else null
    val effectiveUrl = mirrorUrl ?: release.downloadUrl
    val channel = if (mirrorUrl != null) "Mirror 酱高速通道" else "GitHub 直链"

    var downloadJob by remember { mutableStateOf<Job?>(null) }
    // attempt 变化会重启 LaunchedEffect，用于「重试」
    var attempt by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0) }
    var connecting by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<UpdateDownloadResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val downloading = downloadJob?.isActive == true

    // 重试时更换文件名，绕开 HTTP 缓存（OkHttp 默认会缓存重定向与响应）
    val requestUrl = if (attempt == 0) effectiveUrl else effectiveUrl.suffixQuery("retry" to attempt.toString())

    LaunchedEffect(effectiveUrl, attempt) {
        if (effectiveUrl.isBlank()) { connecting = false; error = "未获取到可用的下载地址"; return@LaunchedEffect }
        downloadJob?.cancel()
        downloadJob = null
        progress = 0
        connecting = true
        error = null
        result = null
        downloadJob = scope.launch {
            try {
                val downloaded = DownloadManager(context).downloadUpdateApk(
                    url = requestUrl,
                    fileName = fileName,
                    onProgress = { value ->
                        connecting = false
                        progress = value
                    }
                )
                result = downloaded
            } catch (_: CancellationException) {
                // 取消由界面处理，这里不弹错误
            } catch (throwable: Throwable) {
                error = throwable.message ?: "未知错误"
            }
            // 不在此处清空 downloadJob：重试时旧任务可能晚于新任务结束，
            // 清空会把新任务误标为已结束；downloading 由 isActive 实时判断。
        }
    }

    // 关闭窗口（系统返回或点关闭）：先停下载，再同步清掉未完成的文件。
    // 删除必须同步完成：本函数的 composition scope 会在 onBack() 后立即销毁，
    // 若删在协程里，标签页被销毁时会连同删除任务一起取消，文件就会残留。
    val cancelAndClean = {
        downloadJob?.cancel()
        downloadJob = null
        DownloadManager(context).deleteUpdateApk(fileName)
    }

    AlertDialog(
        // 下载进行中不允许点外部关闭，避免文件残留；需用「取消」按钮
        onDismissRequest = { if (!downloading) { cancelAndClean(); onBack() } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, "下载更新", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (result != null) "下载完成" else "正在下载 ${release.version}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("下载通道：$channel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    result != null -> {
                        Text("文件名：${result!!.file.name}")
                        Text("SHA-256：${result!!.sha256}", style = MaterialTheme.typography.bodySmall)
                        Text(result!!.file.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    error != null -> Text("下载失败：$error", color = MaterialTheme.colorScheme.error)
                    connecting -> {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), amplitude = 0.5f, wavelength = 24.dp)
                        Text("正在连接服务器…")
                    }
                    else -> {
                        WavyProgressBar(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
                        Text("正在下载：$progress%")
                    }
                }
            }
        },
        confirmButton = {
            if (result != null) {
                Button(onClick = { openUpdateApk(context, result!!.file); onBack() }) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text("立即安装")
                }
            } else if (error != null) {
                Button(onClick = { attempt += 1 }) { Text("重试") }
            }
        },
        dismissButton = {
            // 三种状态下的「取消」都停下载并删除文件（含已完成的安装包），不留残留
            TextButton(onClick = { cancelAndClean(); onBack() }) { Text("取消") }
        }
    )
}
