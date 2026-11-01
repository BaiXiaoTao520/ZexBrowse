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
import android.provider.OpenableColumns
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
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.ContactSupport
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Phishing
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.zex.zexbrowse.AppLocale
import com.zex.zexbrowse.CrashReporter
import com.zex.zexbrowse.PromotedNotification
import com.zex.zexbrowse.browser.BrowserViewModel
import com.zex.zexbrowse.data.BrowserNotice
import com.zex.zexbrowse.data.BrowserSettings
import com.zex.zexbrowse.data.CUSTOM_SEARCH_PREFIX
import com.zex.zexbrowse.data.CustomSearchEngine
import com.zex.zexbrowse.data.decodeCustomSearchEngines
import com.zex.zexbrowse.data.DownloadEntity
import com.zex.zexbrowse.data.encodeCustomSearchEngines
import com.zex.zexbrowse.data.LemwoodMirror
import com.zex.zexbrowse.data.NetworkDiagnostics
import com.zex.zexbrowse.data.NetworkProbe
import com.zex.zexbrowse.data.RecommendCategory
import com.zex.zexbrowse.data.RecommendCategoryMeta
import com.zex.zexbrowse.data.appsInCategory
import com.zex.zexbrowse.data.countInCategory
import com.zex.zexbrowse.data.recommendCategoryMeta
import com.zex.zexbrowse.data.RecommendedApp
import com.zex.zexbrowse.data.SettingsStore
import com.zex.zexbrowse.data.SearchSuggestions
import com.zex.zexbrowse.data.UpdateChecker
import com.zex.zexbrowse.data.ReleaseInfo
import com.zex.zexbrowse.download.DownloadFileName
import com.zex.zexbrowse.util.DebugInfo
import com.zex.zexbrowse.util.DebugSettingsSnapshot
import com.zex.zexbrowse.util.DebugUnlock
import com.zex.zexbrowse.download.DownloadManager
import com.zex.zexbrowse.data.SecurityScanner
import com.zex.zexbrowse.download.UpdateDownloadResult
import com.zex.zexbrowse.ExternalLinkDiagnostics
import com.zex.zexbrowse.BuildConfig
import com.zex.zexbrowse.R
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Page { HOME, APP_RECOMMENDATIONS, BROWSER, INCOGNITO, TABS, SETTINGS, BOOKMARKS, DOWNLOADS, HISTORY, DOWNLOAD_DIRECTORY, GENERAL_SETTINGS, LANGUAGE, SITE_SETTINGS, COOKIE_SETTINGS, HTTPS_ONLY, WEB_PERMISSIONS, SECURITY, ADBLOCK, NOTIFICATION, ADVANCED_SETTINGS, DEBUG, NETWORK_DIAGNOSTICS, SEARCH_ENGINE, USER_AGENT, CUSTOM_USER_AGENT, CLIPBOARD_PERMISSION, MIC_PERMISSION, EXTERNAL_APP_PERMISSION, ABOUT }

data class QuickSite(val title: String, val url: String, val icon: String)

/** 自定义浏览器标识：标题 + UA 内容 */
private data class CustomUserAgent(val title: String, val ua: String)

/** 设置搜索项：子选项标题 + 所属页面分组（决定副标题与图标）+ 可匹配关键词 + 点击跳转动作 */
private data class SettingSearchItem(
    val title: String,
    val group: SearchGroup,
    val keywords: List<String>,
    val onClick: () -> Unit,
    val accent: Boolean = false,
    val subtitle: String? = null
)

/** 设置子项所属的页面：副标题取该页面名称，图标与入口卡片保持一致 */
private enum class SearchGroup(val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GENERAL(R.string.s166, Icons.Rounded.Tune),
    LANGUAGE(R.string.s173, Icons.Rounded.Language),
    SITE(R.string.s207, Icons.Rounded.Language),
    PRIVACY(R.string.s225, Icons.Rounded.PrivacyTip),
    WEB_PERMISSIONS(R.string.s250, Icons.Rounded.Security),
    SECURITY(R.string.s240, Icons.Rounded.GppGood),
    AD_BLOCK(R.string.ad_block_title, Icons.Rounded.Block),
    NOTIFICATION(R.string.notification_title, Icons.Rounded.Notifications),
    ADVANCED(R.string.debug_section_advanced, Icons.Rounded.Tune),
    DEBUG(R.string.debug_title, Icons.Rounded.BugReport),
    HISTORY(R.string.s058, Icons.Default.History),
    BOOKMARKS(R.string.s098, Icons.Rounded.Bookmarks),
    DEFAULT_BROWSER(R.string.s042, Icons.Rounded.Public),
    NETWORK(R.string.netdiag_title, Icons.Rounded.NetworkCheck),
    LINK_DIAGNOSTICS(R.string.s277, Icons.Default.Language),
    ABOUT(R.string.s281, Icons.Default.Info),
    CONTACT(R.string.s288, Icons.Rounded.ContactSupport)
}

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

/**
 * 无痕页专用主色。
 *
 * 仅限 [IncognitoHomeScreen] 使用：其余所有页面一律跟随用户在「颜色主题」里的选择，
 * 不得引用本常量。用固定色而非主题色，是为了让无痕环境始终有一致的视觉特征，
 * 不受用户主题变化影响。
 */
private val IncognitoAccentColor = Color(0xFF7B4DDB)

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
 * 无痕浏览专属页面。
 *
 * 与普通主页、普通标签列表刻意区分：整页用紫色调打底（不跟随主题色），
 * 让用户一眼看出自己处在无痕环境，不会误以为在看普通标签。
 *
 * 版式参照网页报错页：大标题 + 说明条目 + 主按钮，居中排列。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IncognitoHomeScreen(
    onSearch: (String) -> Unit,
    onExit: () -> Unit
) {
    // 用 remember 而非 rememberSaveable：无痕页的搜索词不应进 savedInstanceState，
    // 否则会被系统的 Activity 状态持久化一同写盘，与本模式的承诺相违。
    var query by remember { mutableStateOf("") }
    // 无痕专属色：只在本页使用，不跟随用户的颜色主题
    val accent = IncognitoAccentColor
    val deep = Color(0xFF2A1B45)
    // 滚动状态提到 Column 外：verticalScroll 与聚焦时的自动滚动必须共用同一实例，
    // 否则 animateScrollTo 操作的是另一个 ScrollState，页面不会动
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var searchTop by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(deep)) {
        // 顶部光晕：深紫底色上铺一层同色系渐变，避免整屏纯色显得死板
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        Column(
            // imePadding 让整页在键盘弹出时缩小而不是被遮挡，
            // 配合 verticalScroll 与聚焦时的自动滚动，输入框能滚到键盘上方
            Modifier.fillMaxSize().statusBarsPadding().imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Rounded.VisibilityOff,
                null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFFD6C4FF)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.incognito_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.incognito_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC9BCE8),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            // 说明条目：与报错页一致的点列排版
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    R.string.incognito_point_history,
                    R.string.incognito_point_cookie,
                    R.string.incognito_point_exit,
                    R.string.incognito_point_normal
                ).forEach { res ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB49BE8))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(res),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD9CFF2)
                        )
                    }
                }
            }

            // 能力边界单独排版：上面几条是本模式能保证的，这条是不能保证的，
            // 混在同一个点列里容易被当成同一类承诺，因此用分隔线 + 更小字号区隔
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Color(0x33B49BE8))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFB49BE8)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.incognito_point_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB7A8D6)
                )
            }

            Spacer(Modifier.height(32.dp))

            // 搜索入口：在无痕环境下直接搜，结果落在无痕标签里
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .onGloballyPositioned { coords ->
                        searchTop = coords.positionInParent().y.toInt()
                    }
                    .onFocusChanged { focus ->
                        // 聚焦时主动把输入框滚到可视区：键盘弹出后页面高度会缩，
                        // 不滚上去的话输入框会被压在键盘下方。
                        // 仅在已测量出位置时滚动，避免首帧 searchTop=0 把页面跳回顶部
                        if (focus.isFocused && searchTop > 0) {
                            scope.launch { scrollState.animateScrollTo(searchTop) }
                        }
                    },
                placeholder = {
                    Text(stringResource(R.string.incognito_search_hint), color = Color(0xFFA896C8))
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFB49BE8)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { if (query.isNotBlank()) onSearch(query.trim()) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF5B4A80),
                    focusedBorderColor = Color(0xFFB49BE8),
                    unfocusedContainerColor = Color(0xFF352550),
                    focusedContainerColor = Color(0xFF352550),
                    cursorColor = Color(0xFFD6C4FF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD6C4FF),
                    contentColor = Color(0xFF2A1B45)
                )
            ) {
                Text(stringResource(R.string.incognito_exit), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

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
    val context = LocalContext.current
    val reasons = remember(errorCode, errorCategory) { describeLoadFailure(context, errorCode, errorCategory) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.s000), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.s001))
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
private fun describeLoadFailure(context: Context, errorCode: Int, errorCategory: Int): List<String> {
    val specific = when {
        errorCode == WebRequestError.ERROR_UNKNOWN_HOST -> context.getString(R.string.err_unknown_host)
        errorCode == WebRequestError.ERROR_OFFLINE -> context.getString(R.string.err_offline)
        errorCode == WebRequestError.ERROR_NET_TIMEOUT -> context.getString(R.string.err_timeout)
        errorCode == WebRequestError.ERROR_CONNECTION_REFUSED -> context.getString(R.string.err_refused)
        errorCode == WebRequestError.ERROR_NET_INTERRUPT -> context.getString(R.string.err_interrupted)
        errorCode == WebRequestError.ERROR_NET_RESET -> context.getString(R.string.err_reset)
        errorCode == WebRequestError.ERROR_REDIRECT_LOOP -> context.getString(R.string.err_redirect_loop)
        errorCode == WebRequestError.ERROR_PORT_BLOCKED -> context.getString(R.string.err_port_blocked)
        errorCode == WebRequestError.ERROR_SECURITY_SSL ||
            errorCode == WebRequestError.ERROR_SECURITY_BAD_CERT ||
            errorCode == WebRequestError.ERROR_BAD_HSTS_CERT -> context.getString(R.string.err_cert)
        errorCode == WebRequestError.ERROR_HTTPS_ONLY -> context.getString(R.string.err_https_required)
        errorCode == WebRequestError.ERROR_UNKNOWN_PROTOCOL -> context.getString(R.string.err_bad_protocol)
        errorCode == WebRequestError.ERROR_MALFORMED_URI -> context.getString(R.string.err_bad_url)
        errorCode == WebRequestError.ERROR_FILE_NOT_FOUND -> context.getString(R.string.err_file_missing)
        errorCode == WebRequestError.ERROR_FILE_ACCESS_DENIED -> context.getString(R.string.err_file_denied)
        errorCode == WebRequestError.ERROR_CORRUPTED_CONTENT -> context.getString(R.string.err_corrupt)
        errorCode == WebRequestError.ERROR_CONTENT_CRASHED -> context.getString(R.string.err_crashed)
        errorCode == WebRequestError.ERROR_UNSAFE_CONTENT_TYPE -> context.getString(R.string.err_bad_type)
        errorCode == WebRequestError.ERROR_INVALID_CONTENT_ENCODING -> context.getString(R.string.err_bad_encoding)
        errorCode == WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> context.getString(R.string.err_proxy_refused)
        errorCode == WebRequestError.ERROR_UNKNOWN_PROXY_HOST -> context.getString(R.string.err_proxy_host)
        errorCode == WebRequestError.ERROR_LOCAL_NETWORK_ACCESS_DENIED -> context.getString(R.string.err_local_denied)
        errorCode == WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI -> context.getString(R.string.err_malware)
        errorCode == WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> context.getString(R.string.err_unwanted)
        errorCode == WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> context.getString(R.string.err_harmful)
        errorCode == WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> context.getString(R.string.err_phishing)
        else -> null
    }

    val byCategory = when {
        errorCategory == WebRequestError.ERROR_CATEGORY_NETWORK -> context.getString(R.string.cat_network)
        errorCategory == WebRequestError.ERROR_CATEGORY_SECURITY -> context.getString(R.string.cat_security)
        errorCategory == WebRequestError.ERROR_CATEGORY_CONTENT -> context.getString(R.string.cat_content)
        errorCategory == WebRequestError.ERROR_CATEGORY_URI -> context.getString(R.string.cat_uri)
        errorCategory == WebRequestError.ERROR_CATEGORY_PROXY -> context.getString(R.string.cat_proxy)
        errorCategory == WebRequestError.ERROR_CATEGORY_SAFEBROWSING -> context.getString(R.string.cat_safebrowsing)
        else -> null
    }

    return buildList {
        specific?.let { add(it) }
        byCategory?.takeIf { it != specific }?.let { add(it) }
        add(context.getString(R.string.err_tip_retry))
        add(context.getString(R.string.err_tip_site))
    }
}

private fun decodeBookmarks(raw: String): List<QuickSite> =
    raw.split("\n").mapNotNull { line ->
        val title = line.substringBefore('|').trim()
        val url = line.substringAfter('|', "").trim()
        if (title.isEmpty() || url.isEmpty()) null else QuickSite(title, url, title.take(1))
    }

/** 自定义 UA 列表编解码，每行「标题|UA内容」 */
private fun decodeCustomUserAgents(raw: String): List<CustomUserAgent> =
    raw.split("\n").mapNotNull { line ->
        val title = line.substringBefore('|').trim()
        val ua = line.substringAfter('|', "").trim()
        if (title.isEmpty() || ua.isEmpty()) null else CustomUserAgent(title, ua)
    }

private fun encodeCustomUserAgents(list: List<CustomUserAgent>): String =
    list.joinToString("\n") { "${it.title}|${it.ua}" }

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
    if (!launched) Toast.makeText(context, context.getString(R.string.toast_default_browser_hint), Toast.LENGTH_LONG).show()
}

@Composable
fun ZexBrowseApp(incomingUrl: String? = null, incomingUrlSeq: Long = 0L, onIncomingUrlHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    // 上次运行如果是崩溃退出，启动时把保存的崩溃日志弹出来，方便用户复制反馈
    var previousCrashLog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        previousCrashLog = CrashReporter.readLast(context)
        // 首帧组合成功：清除「启动进行中」标记，表示本次启动已完成（未崩溃）
        CrashReporter.markStarted(context)
    }
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
            Toast.makeText(context, context.getString(R.string.s036), Toast.LENGTH_SHORT).show()
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

    // 旧版只有一个自定义搜索引擎，升级后列表为空会让已有配置从界面上消失。
    // 这里把那一份一次性搬进列表（仅当列表为空且旧配置完整时），移完再清空旧字段。
    LaunchedEffect(Unit) {
        val stored = settingsStore.settings.first()
        if (stored.customSearchEngines.isNotBlank()) return@LaunchedEffect
        val url = stored.customSearchUrl.trim()
        if (url.isEmpty() || !url.contains("{query}")) return@LaunchedEffect
        val engine = CustomSearchEngine(stored.customSearchTitle.ifBlank { context.getString(R.string.s037) }, url)
        settingsStore.customSearchEngines(encodeCustomSearchEngines(listOf(engine)))
        // 迁移前正选着这个自定义搜索时，选中值也要跟着换成新标识，否则它会变成选不中的孤儿
        if (stored.searchEngine == "custom") settingsStore.searchEngine(engine.value)
        settingsStore.customSearch("", "")
    }

    browserViewModel.browserSettings = settings
    // 语言镜像同步：DataStore 是唯一事实源，SharedPreferences 只是给 attachBaseContext 的同步读缓存。
    // 两者可能因异常退出/备份恢复而不一致，启动时以 DataStore 为准修正一次。
    //
    // 必须只跑一次（Unit）而不是跟随 settings.language：
    // 用户刚在设置里选了新语言时 DataStore 已变、但当前界面仍是旧语言，
    // 若这里跟着改写缓存，本次运行中任何 Activity 重建都会念到新语言，
    // 与「重启后生效」的约定矛盾，还会把界面弄成半新半旧。
    LaunchedEffect(Unit) {
        val stored = settings.language
        if (AppLocale.current(context) != stored) {
            AppLocale.remember(context, stored)
        }
    }
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
            val checker = UpdateChecker()
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
            // 手势完成：把整页顺完滑出屏幕，然后换页。
            backProgress.animateTo(1f, tween(100))
            // 先标记「跳过页面切换动画」，再执行返回。
            // 此时上层整页已在屏幕外，屏幕上看到的是下层预渲染的目标页。
            predictivePop = true
            performBack()
            withContext(NonCancellable) {
                // 等一帧再复位：AnimatedContent 的内容换转由内部协程驱动，
                // 即使过渡为空，也要到下一帧才把当前内容换成新页。
                // 若在同一批量复位位移，重新回到原位的仍是旧页，
                // 就是用户看到的「返回后停留一帧原页面」。
                // 此时上层仍在屏幕外，所以多等这一帧不会露底。
                withFrameNanos { }
                // 复位位移与撤掉下层放在同一批量，保证同一帧生效。
                // predictivePop 最后才复位：它控制着 key，提早复位会触发一次无意义的重建。
                backProgress.snapTo(0f)
                backActive = false
                predictivePop = false
            }
        } catch (cancelled: CancellationException) {
            // 手势取消：本协程已处于取消态，回弹动画需放在 NonCancellable 中，
            // 否则 animateTo 会立即再抛 CancellationException，导致回弹不执行、页面停在半途。
            withContext(NonCancellable) {
                // 先回弹到位再撤下层：若先撤下层，上层仍在偏移动画中，
                // 页面下方会短暂露出底色，看上去像闪了一下
                backProgress.animateTo(0f, tween(180))
                backActive = false
                // 取消路径同样要复位，否则下次普通跳转会被跳过动画
                predictivePop = false
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
    // HTTPS-Only：开关或生效范围变化时同步到 Gecko 运行时
    LaunchedEffect(settings.httpsOnlyEnabled, settings.httpsOnlyMode) {
        browserViewModel.applyHttpsOnly(settings.httpsOnlyEnabled, settings.httpsOnlyMode)
    }
    // 广告拦截：开关变化时同步到 Gecko 运行时、现有会话与内置广告隐藏扩展
    LaunchedEffect(settings.adBlockEnabled) {
        browserViewModel.applyAdBlock(settings.adBlockEnabled)
    }
    val colorScheme = resolveColorScheme(settings, useDarkTheme, context)

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
                                resetToHome = { navForward = false; navStack = listOf(Page.HOME) },
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
                        // 每次都显式赋值全部属性，不能只在 progress > 0 时设。
                        // graphicsLayer 会沿用上一次的值：若回零时不重置 clip/shape/translationX，
                        // 下一帧就会带着残留的位移与裁剪继续绘制，表现为「返回后停留一帧原页面」。
                        translationX = size.width * progress
                        val scale = 1f - 0.16f * progress
                        scaleX = scale
                        scaleY = scale
                        // Miuix 风格：跟手缩放时边缘圆角收拢并投下阴影
                        shape = RoundedCornerShape((24f * progress).dp)
                        clip = progress > 0f
                        shadowElevation = (14f * progress).dp.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = 0.35f)
                        spotShadowColor = Color.Black.copy(alpha = 0.35f)
                    }
                ) {
                    // 页面跳转动效：前进时新页从右滑入、旧页向左滑出；后退时相反。
                    // 手势返回提交时直接不播任何过渡，并同时禁用尺寸动画。
                    //
                    // why：AnimatedContent 在 targetState 变化时会把新旧两套内容同时组合并绘制，
                    // 它内部的过渡协程需要至少一帧才会丢弃旧内容。
                    // 在那一帧里上层若已复位位移，重新回到原位的仍是旧页，
                    // 就是用户看到的「返回后停留一帧原页面」。
                    // 因此这里同时做两件事：过渡置空 + 尺寸变换置 null，
                    // 保证新旧内容在同一帧完成交接。
                    // 手势提交时 key 变化 → AnimatedContent 被彻底重建，
                    // 不会把旧页面作为「正在退出的内容」再绘制一帧。
                    // 这是消除「返回后停留一帧」最直接的手段：
                    // 不再依赖过渡时长是否为 0，也不依赖内部协程何时清理旧内容。
                    key(predictivePop) {
                        AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                if (predictivePop) {
                                    EnterTransition.None togetherWith ExitTransition.None using null
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
                                resetToHome = { navForward = false; navStack = listOf(Page.HOME) },
                                goBack = goBack,
                                onSponsor = { sponsorInfo = true }
                            )
                        }
                    }
                }
            }
            // 上次崩溃日志展示：启动可读，支持一键复制 / 清除
            previousCrashLog?.let { log ->
                AlertDialog(
                    onDismissRequest = { CrashReporter.clear(context); previousCrashLog = null },
                    title = { Text(stringResource(R.string.crash_dialog_title)) },
                    text = {
                        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            Text(stringResource(R.string.crash_dialog_hint), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(log, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            runCatching {
                                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                                cm?.setPrimaryClip(android.content.ClipData.newPlainText("crash", log))
                            }
                            Toast.makeText(context, context.getString(R.string.crash_log_copied), Toast.LENGTH_SHORT).show()
                        }) { Text(stringResource(R.string.s252)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { CrashReporter.clear(context); previousCrashLog = null }) { Text(stringResource(R.string.crash_dialog_dismiss)) }
                    }
                )
            }
            startupUpdate?.let { release -> UpdateDialog(release = release, onDismiss = { startupUpdate = null }) }
            if (sponsorInfo) SponsorDialog(onDismiss = { sponsorInfo = false })
            if (showFirstRunStorageHint && startupUpdate == null) {
                AlertDialog(
                    onDismissRequest = { showFirstRunStorageHint = false; scope.launch { settingsStore.storageHintShown(true) } },
                    title = { Text(stringResource(R.string.s038)) },
                    text = { Text(stringResource(R.string.s039)) },
                        confirmButton = { TextButton(onClick = {
                            showFirstRunStorageHint = false
                            scope.launch { settingsStore.storageHintShown(true) }
                            externalDirPicker.launch(null)
                        }) { Text(stringResource(R.string.s040)) } },
                    dismissButton = { TextButton(onClick = { showFirstRunStorageHint = false; scope.launch { settingsStore.storageHintShown(true) } }) { Text(stringResource(R.string.s041)) } }
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
                    title = { Text(stringResource(R.string.s042)) },
                    text = { Text(stringResource(R.string.s043)) },
                    confirmButton = {
                        Button(onClick = {
                            dismissDefaultBrowserPrompt()
                            requestDefaultBrowser()
                        }) { Text(stringResource(R.string.s044)) }
                    },
                    dismissButton = { TextButton(onClick = dismissDefaultBrowserPrompt) { Text(stringResource(R.string.s041)) } }
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
                    title = { Text(stringResource(R.string.s045)) },
                    text = { Text(stringResource(R.string.s046, micSite)) },
                    confirmButton = { TextButton(onClick = { finishMic(true, true) }) { Text(stringResource(R.string.s047)) } },
                    dismissButton = { TextButton(onClick = { finishMic(false, true) }) { Text(stringResource(R.string.s048)) } }
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
    resetToHome: () -> Unit,
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
        Page.INCOGNITO -> IncognitoHomeScreen(
            onSearch = { address ->
                // 优先复用已有无痕标签：连续搜索不堆标签，与主流浏览器行为一致；
                // 只有当前没有无痕标签时才新建。无论哪种走法，结果都不会落到普通标签。
                if (!browserViewModel.loadInIncognito(address)) {
                    browserViewModel.newTab(incognito = true, initialUrl = address)
                }
                navigate(Page.BROWSER)
            },
            onExit = {
                browserViewModel.exitIncognito()
                // 退出后直接回首页：把栈重置成只有首页，避免返回到已关掉的无痕页
                resetToHome()
            }
        )
        Page.BROWSER -> BrowserScreen(
            viewModel = browserViewModel,
            settings = settings,
            settingsStore = settingsStore,
            onTabs = { incognito -> onTabsIncognitoChange(incognito); navigate(Page.TABS) },
            onIncognitoHome = { navigate(Page.INCOGNITO) },
            onDownloads = { navigate(Page.DOWNLOADS) },
            onSettings = { navigate(Page.SETTINGS) }
        )
        Page.TABS -> TabOverview(
            viewModel = browserViewModel,
            initialIncognito = tabsIncognito,
            onBack = goBack,
            onExitIncognito = resetToHome
        )
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
            onLanguage = { navigate(Page.LANGUAGE) },
            onSiteSettings = { navigate(Page.SITE_SETTINGS) },
            onCookieSettings = { navigate(Page.COOKIE_SETTINGS) },
            onHttpsOnly = { navigate(Page.HTTPS_ONLY) },
            onWebPermissions = { navigate(Page.WEB_PERMISSIONS) },
            onSecurity = { navigate(Page.SECURITY) },
            onAdBlock = { navigate(Page.ADBLOCK) },
            onNotification = { navigate(Page.NOTIFICATION) },
            onAdvancedSettings = { navigate(Page.ADVANCED_SETTINGS) },
            onNetworkDiagnostics = { navigate(Page.NETWORK_DIAGNOSTICS) },
            onAbout = { navigate(Page.ABOUT) },
            onContactUs = { openInBrowser(context, CONTACT_URL) },
            onBack = goBack
        )
        Page.GENERAL_SETTINGS -> GeneralSettingsScreen(
            settings = settings,
            setMode = { scope.launch { settingsStore.mode(it) } },
            setApkHash = { scope.launch { settingsStore.apkHash(it) } },
            setHardwareAcceleration = { scope.launch { settingsStore.hardwareAcceleration(it) } },
            setColorTheme = { scope.launch { settingsStore.colorTheme(it) } },
            setCustomColor = { scope.launch { settingsStore.customColor(it) } },
            onDownloads = { navigate(Page.DOWNLOADS) },
            onDownloadDirectory = { navigate(Page.DOWNLOAD_DIRECTORY) },
            onBack = goBack
        )
        Page.LANGUAGE -> LanguageScreen(
            language = settings.language,
            setLanguage = { tag ->
                // 只写 DataStore，不重建 Activity：语言在下次启动时由 attachBaseContext 读取生效。
                // 同步镜像一份 SharedPreferences，供下次启动同步读到（DataStore 读取是异步的）。
                if (tag != settings.language) {
                    AppLocale.remember(context, tag)
                    scope.launch { settingsStore.language(tag) }
                    Toast.makeText(context, context.getString(R.string.toast_language_restart), Toast.LENGTH_LONG).show()
                }
            },
            onBack = goBack
        )
        Page.ADVANCED_SETTINGS -> AdvancedSettingsScreen(
            onDebug = { navigate(Page.DEBUG) },
            onBack = goBack
        )
        Page.DEBUG -> DebugScreen(
            settings = settings,
            onBack = goBack
        )
        Page.NETWORK_DIAGNOSTICS -> NetworkDiagnosticsScreen(onBack = goBack)
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
                Toast.makeText(context, context.getString(R.string.s050), Toast.LENGTH_SHORT).show()
            },
            onHttpsOnly = { navigate(Page.HTTPS_ONLY) },
            onBack = goBack
        )
        Page.HTTPS_ONLY -> HttpsOnlyScreen(
            settings = settings,
            onToggle = { scope.launch { settingsStore.httpsOnlyEnabled(it) } },
            onSelectMode = { scope.launch { settingsStore.httpsOnlyMode(it) } },
            onBack = goBack
        )
        Page.WEB_PERMISSIONS -> WebPermissionsScreen(
            settings = settings,
            onClipboard = { navigate(Page.CLIPBOARD_PERMISSION) },
            onExternalApp = { navigate(Page.EXTERNAL_APP_PERMISSION) },
            onMic = { navigate(Page.MIC_PERMISSION) },
            onBack = goBack
        )
        Page.SECURITY -> SecuritySettingsScreen(
            settings = settings,
            onSelectMode = { mode -> scope.launch { settingsStore.securityMode(mode) } },
            onBack = goBack
        )
        Page.ADBLOCK -> {
            // 进入页面时读一次累计拦截次数；开关变化后再读一次——刚切换时计数不会立刻跳，
            // 真正的增量来自之后的页面加载，所以这里不做轮询，避免无意义的后台查询
            var blockedCount by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(settings.adBlockEnabled) {
                browserViewModel.loadBlockedCount { blockedCount = it }
            }
            AdBlockScreen(
                settings = settings,
                setAdBlock = { enabled -> scope.launch { settingsStore.adBlock(enabled) } },
                blockedCount = blockedCount,
                onBack = goBack
            )
        }
        Page.NOTIFICATION -> NotificationSettingsScreen(onBack = goBack)
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
                Toast.makeText(context, context.getString(R.string.s051), Toast.LENGTH_SHORT).show()
                goBack()
            },
            onBack = goBack
        )
        Page.SEARCH_ENGINE -> SearchEngineScreen(
            settings = settings,
            onSelect = { scope.launch { settingsStore.searchEngine(it) } },
            setEngines = { scope.launch { settingsStore.customSearchEngines(it) } },
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
            setUserAgents = { scope.launch { settingsStore.customUserAgents(it) } },
            selectUserAgent = { title, ua ->
                scope.launch { settingsStore.customUserAgent(title, ua); settingsStore.userAgentMode("custom") }
            },
            resetUserAgent = { scope.launch { settingsStore.userAgentMode("geckoview") } },
            onBack = goBack
        )
        Page.CLIPBOARD_PERMISSION -> ClipboardPermissionScreen(
            settings = settings,
            onSelect = { scope.launch { settingsStore.clipboardReadPolicy(it) } },
            onClear = {
                scope.launch { settingsStore.clearSitePermissions() }
                Toast.makeText(context, context.getString(R.string.s052), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, context.getString(R.string.s052), Toast.LENGTH_SHORT).show()
            },
            onBack = goBack
        )
        Page.EXTERNAL_APP_PERMISSION -> SimplePolicyScreen(
            title = stringResource(R.string.s053),
            description = stringResource(R.string.s054),
            options = listOf("ask" to stringResource(R.string.s055), "allow" to stringResource(R.string.s047), "deny" to stringResource(R.string.s056)),
            current = settings.externalAppPolicy,
            onSelect = { scope.launch { settingsStore.externalAppPolicy(it) } },
            onBack = goBack
        )
        Page.ABOUT -> AboutScreen(
            settings = settings,
            setAutoCheckUpdates = { scope.launch { settingsStore.autoCheckUpdates(it) } },
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

/**
 * 首页头部：品牌名 + 时钟 + 历史/设置入口。
 *
 * 不用 TopAppBar：首页内容本身可以滚到它下面，悬浮的渐变头图能带来更强的层次感；
 * 这里只保留一条轻量标题行，把主要空间留给搜索与常用站点。
 */
@Composable
private fun HomeHeader(onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "ZexBrowse",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            ClockText()
        }
        IconButton(onClick = onHistory) { Icon(Icons.Default.History, stringResource(R.string.s058), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.s059), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** 首页顶栏时钟：独立状态，每秒只重组自身，避免拖拽整个 HomeScreen 重组导致掉帧 */
@Composable
private fun ClockText() {
    val pattern = stringResource(R.string.s057)
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); kotlinx.coroutines.delay(1_000) } }
    Text(SimpleDateFormat(pattern, Locale.getDefault()).format(now), style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun HomeScreen(settings: BrowserSettings, searchHistory: List<String>, onRemoveSearchHistory: (String) -> Unit, setQuickSites: (String) -> Unit, onOpen: (String) -> Unit, onHistory: () -> Unit, onSettings: () -> Unit, onAppRecommendations: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val bookmarks = remember(settings.quickSites) { decodeBookmarks(settings.quickSites) }
    var showQuickSiteDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<QuickSite?>(null) }

    Box(Modifier.fillMaxSize()) {
        // 顶部渐变：作为整页的视觉基底，向下淡出到背景色，避免大面积纯色显得单调
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            // 内容整体可滚动：书签多、系统字体放大时底部也不会不可达
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeader(onHistory = onHistory, onSettings = onSettings)

            // 搜索框：直接用描边圈线形式呈现，不套卡片外壳。
            // 之前卡片 + 输入框两层边框叠在一起会在四角留下双重描边，看着很脏；
            // 单层细圈线既干净也更接近系统搜索框的观感。
            AddressInput(
                value = query,
                onValueChange = { query = it },
                onSubmit = { onOpen(query) },
                searchHistory = searchHistory,
                onRemoveHistory = onRemoveSearchHistory,
                modifier = Modifier.padding(top = 18.dp)
            )

            Spacer(Modifier.height(20.dp))
            AppRecommendationEntry(onClick = onAppRecommendations)

            Spacer(Modifier.height(24.dp))
            // 常用站点标题行：左边标题 + 数量，右边「添加」
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.s060), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (bookmarks.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                bookmarks.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                FilledTonalButton(onClick = { showQuickSiteDialog = true }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.s061))
                }
            }
            Spacer(Modifier.height(12.dp))
            BookmarkGrid(
                sites = bookmarks,
                onOpen = onOpen,
                onEdit = { editingBookmark = it },
                onDelete = { site -> setQuickSites(encodeBookmarks(bookmarks.filter { it.url != site.url })) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQuickSiteDialog) {
        BookmarkDialog(
            onSubmit = { site ->
                setQuickSites(encodeBookmarks(bookmarks + site))
                showQuickSiteDialog = false
            },
            onDismiss = { showQuickSiteDialog = false }
        )
    }

    val editing = editingBookmark
    if (editing != null) {
        BookmarkDialog(
            initial = editing,
            onSubmit = { site ->
                setQuickSites(encodeBookmarks(bookmarks.map { if (it.url == editing.url) site else it }))
                editingBookmark = null
            },
            onDismiss = { editingBookmark = null }
        )
    }
}

/**
 * 应用推荐页的屏幕状态。
 *
 * 用单一枚举而非「可空 Boolean + 布尔派生」的原因：Crossfade 过渡期间会同时
 * 组合并绘制退出与进入两份内容，若退出那份回头去读一个已经变了的变量，
 * 就会在过渡中画出错误的状态（表现为加载完成后闪一下报错页）。
 * 枚举把每个状态固化成独立值，退出内容始终重放它自己的状态。
 */
private enum class RecommendScreenState { LOADING, OFFLINE, READY }

/**
 * 应用推荐列表页。
 *
 * 下载行为沿用本应用已有的下载体系：
 * - APK 直链 → 走应用内下载（下载目录遵循「下载设置」的配置）；
 * - 网页/网盘类链接 → 交给系统处理，由系统默认浏览器或用户选择的应用打开。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppRecommendationsScreen(browserViewModel: BrowserViewModel, onBack: () -> Unit) {
    // 当前打开的分类；为 null 时展示分类卡片列表
    var openCategory by remember { mutableStateOf<RecommendCategory?>(null) }
    // 屏幕状态：加载中 / 不通 / 已就绪
    var screenState by remember { mutableStateOf(RecommendScreenState.LOADING) }
    // 重试计数：每次点重试递增，驱动下面的 LaunchedEffect 重新探测
    var probeAttempt by remember { mutableStateOf(0) }

    // 已进入分类页：直接交给分类页，本页不再渲染任何内容
    val category = openCategory
    if (category != null) {
        RecommendCategoryScreen(
            category = category,
            browserViewModel = browserViewModel,
            onBack = { openCategory = null }
        )
        return
    }

    LaunchedEffect(probeAttempt) {
        screenState = RecommendScreenState.LOADING
        // 瞬时失败很常见（DNS 未就绪、首个连接被重置、弱网丢包）。
        // 若第一次失败就写 OFFLINE，Crossfade 会真的把报错页画出来，
        // 等第二次成功再淡入内容，用户看到的就是「瞬间报错页随后正常」。
        // 因此失败不写状态，先静默重试；两次都失败才确定是不通。
        var ok = NetworkProbe.isReachable()
        if (!ok) {
            kotlinx.coroutines.delay(600)
            ok = NetworkProbe.isReachable()
        }
        screenState = if (ok) RecommendScreenState.READY else RecommendScreenState.OFFLINE
    }

    // 警示牌显示 20 秒后自动收起。
    // 状态用 remember 而非 rememberSaveable：离开页面时组合被销毁，
    // 重新进入会重建并重新计时，正好符合「每次进入都重新计时」的要求。
    var noticeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(screenState) {
        if (screenState != RecommendScreenState.READY) return@LaunchedEffect
        noticeVisible = true
        kotlinx.coroutines.delay(20_000)
        noticeVisible = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.s062)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }
            )
        }
    ) { padding ->
        // 三态 Crossfade：key 直接是屏幕状态本身，退出内容始终重放它自己的
        // 状态值，不会再回头读到「已经变成 READY」的变量而画成报错页
        Crossfade(
            targetState = screenState,
            animationSpec = tween(durationMillis = 320),
            label = "recommendationsCrossfade"
        ) { state ->
            when (state) {
                // 探测中：形变指示器自带官方容器背景（圆形色块），
                // 不传 polygons 即使用官方的 7 种默认形状序列
                RecommendScreenState.LOADING -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.recommend_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 不通：给出提示与重试入口
                RecommendScreenState.OFFLINE -> Box(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.CloudOff,
                            null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.recommend_offline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        FilledTonalButton(onClick = { probeAttempt++ }) {
                            Text(stringResource(R.string.recommend_retry))
                        }
                    }
                }
                // 就绪：分类卡片列表，点击进入各分类详情页
                RecommendScreenState.READY -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(stringResource(R.string.s064), style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        Text(stringResource(R.string.s065), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // 法规与服务调整警示牌：置于列表标题之后、分类卡片之前。
                    // 收起时同时做淡出与垂直收缩，列表下方内容随之上移。
                    item {
                        AnimatedVisibility(
                            visible = noticeVisible,
                            enter = fadeIn(animationSpec = tween(240)) +
                                expandVertically(animationSpec = tween(320)),
                            exit = fadeOut(animationSpec = tween(240)) +
                                shrinkVertically(animationSpec = tween(320))
                        ) {
                            RecommendationServiceNotice()
                        }
                    }
                    items(recommendCategoryMeta) { meta ->
                        val count = countInCategory(meta.category)
                        if (count > 0) {
                            RecommendationCategoryCard(
                                meta = meta,
                                count = count,
                                onClick = { openCategory = meta.category }
                            )
                        }
                    }
                    // 网络提示置于列表末尾：完整读完推荐内容后才看到，且与上述
                    // 法规警示牌（errorContainer）区分开，用中性色不抢注意力
                    item { RecommendationNetworkNotice() }
                }
            }
        }
    }
}

/**
 * 分类详情页：展示单一分类下的推荐条目。
 *
 * 下载/弹窗逻辑与原先的推荐页一致，只是作用范围缩到该分类：
 * - APK 直链 → 应用内下载；
 * - 网页/网盘 → 二次说明后交给系统浏览器；
 * - 弹窗状态全部留在本页，返回分类列表时自然丢弃。
 */
@Composable
private fun RecommendCategoryScreen(
    category: RecommendCategory,
    browserViewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<RecommendedApp?>(null) }
    var downloadingApp by remember { mutableStateOf<RecommendedApp?>(null) }
    var downloadId by remember { mutableStateOf<String?>(null) }
    var browserNoticeApp by remember { mutableStateOf<RecommendedApp?>(null) }

    val meta = recommendCategoryMeta.firstOrNull { it.category == category }
    val apps = remember(category) { appsInCategory(category) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(meta?.titleRes ?: R.string.s062)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }
            )
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.recommend_cat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps) { app ->
                    RecommendationCard(app = app) { detail = app }
                }
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
        // 下载列表的订阅只发生在弹窗内部：下载过程中进度每秒会更新多次，
        // 若在页面层订阅，每次更新都会让下面整个推荐卡片列表重组，产生卡顿。
        RecommendationDownloadDialogHost(
            app = app,
            downloads = browserViewModel.downloads,
            downloadId = downloadId,
            onInstall = { record -> openDownload(context, record) },
            onCancelDownload = {
                // 按地址定位任务：记录 Id 可能还没回填，但地址一定是对的
                browserViewModel.cancelAndRemoveDownloadByUrl(app.downloadUrl)
            },
            onDismiss = { downloadingApp = null; downloadId = null }
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

/**
 * 用外部浏览器打开链接。
 *
 * 设计原则：永远不替用户指定「用哪个浏览器」。
 *
 * Android 没有公开 API 可以查询「别人的默认浏览器是谁」（RoleManager 只公开
 * isRoleAvailable / isRoleHeld / createRequestRoleIntent）。能反映当前默认处理器的
 * 公开手段只有 resolveActivity。因此：
 *
 * 1. resolveActivity 返回的是真浏览器 → 它就是用户设置的默认浏览器，直接用它；
 * 2. 返回的不是浏览器（应用宝、微信等抢占了 http）→ 不自作主张挑一个，
 *    而是弹系统选择器并仅保留真浏览器，由用户自己决定；
 * 3. 设备上只有一个浏览器时，直接用那个（不存在选择余地）。
 */
/**
 * 打开柠枺镜像下载链接。
 *
 * 优先在本应用自己的浏览器内打开：GeckoView 是 Firefox 内核，HTTPS 下
 * `isSecureContext` 与 `crypto.subtle` 可用，站点的 PoW 验证页能在页面里正常完成。
 * 验证通过后页面跳转到带令牌的下载地址，Gecko 触发 onExternalResponse，
 * 此时 body 流被当场读走存成临时文件，Worker 直接用这份本地数据落盘，
 * **全程只请求一次**，不会因为二次请求消耗令牌而 403。
 *
 * 回退：若无法交给本应用自己（极少见），再交给外部浏览器。
 */
private fun openMirrorDownload(context: Context, url: String) {
    // 交给本应用自己：显式 setPackage 自身，经 MainActivity.onNewIntent 进入本应用浏览器标签。
    // 不能用 openInBrowser——那个函数刻意避开自身（那是为“跳浏览器”场景设计的）。
    val self = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .setPackage(context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(self) }.isSuccess) return
    // 回退：本应用无法拉起时，交给外部浏览器
    openInBrowser(context, url)
}

/**
 * 用外部浏览器打开普通网页类链接（联系我们、关于页主页、赞助页等）。
 *
 * 这类链接不涉及一次性令牌，本应用自己打开也不会出问题，所以正常交给
 * 用户设置的默认处理器即可；只在默认处理器不是浏览器（应用宝、微信等抢占）
 * 时，才改从真浏览器里选，避免链接莫名跳进应用商店。
 */
private fun openInBrowser(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pm = context.packageManager

    // 1) 默认处理器：最好是真浏览器，要么就是本应用（本应用也是浏览器）
    @Suppress("DEPRECATION")
    val defaultPkg = pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName
    if (defaultPkg != null && (defaultPkg == context.packageName || isRealBrowser(pm, defaultPkg))) {
        // 直接 startActivity 即可：若默认是本应用，系统会自己路由回当前应用
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }

    // 2) 默认处理器不是浏览器：改从真浏览器里挑
    val browsers = installedBrowsers(context, intent)
    if (browsers.size == 1) {
        intent.setPackage(browsers.first())
        if (runCatching { context.startActivity(intent) }.isSuccess) return
        intent.setPackage(null)
    }

    // 3) 仍有多个候选：交给选择器（已排除非浏览器）
    showBrowserChooser(context, intent)
}

/** 列出能处理该链接的真实浏览器（排除本应用） */
private fun installedBrowsers(context: Context, view: Intent): List<String> {
    val pm = context.packageManager
    return pm.queryIntentActivities(view, 0)
        .mapNotNull { it.activityInfo?.packageName }
        .distinct()
        .filter { it != context.packageName && isRealBrowser(pm, it) }
}

/**
 * 弹系统选择器，并把非浏览器应用从候选里排除。
 *
 * EXTRA_EXCLUDE_COMPONENTS 能逐项排除，这里把所有「非真浏览器」与自身都排掉，
 * 这样选择器里只剩浏览器。且它仍然是系统原生选择器，用户可在其中勾选「始终」。
 */
private fun showBrowserChooser(context: Context, view: Intent) {
    val pm = context.packageManager
    val excluded = pm.queryIntentActivities(view, 0)
        .filter { info ->
            val pkg = info.activityInfo.packageName
            pkg == context.packageName || !isRealBrowser(pm, pkg)
        }
        .map { android.content.ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        .distinct()
        .toTypedArray()

    val handlers = installedBrowsers(context, view)
    if (handlers.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.toast_no_browser), Toast.LENGTH_LONG).show()
        return
    }

    val chooser = Intent.createChooser(view, context.getString(R.string.chooser_browser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (excluded.isNotEmpty()) chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excluded)
    runCatching { context.startActivity(chooser) }
        .onFailure { Toast.makeText(context, context.getString(R.string.toast_no_app), Toast.LENGTH_SHORT).show() }
}

/**
 * 判断一个包是不是真正的浏览器。
 *
 * 两个依据任一成立即可：
 * 1. 能响应 ACTION_MAIN + CATEGORY_APP_BROWSER（安卓给浏览器专用的入口类别）；
 * 2. 在已知浏览器白名单里（兼底未声明 APP_BROWSER 的真浏览器）；
 *
 * 应用宝、微信、淘宝这类只声明普通 http VIEW 的应用两者都不满足，会被排除。
 */
private fun isRealBrowser(pm: android.content.pm.PackageManager, pkg: String): Boolean {
    if (pkg in KNOWN_BROWSERS) return true
    return runCatching {
        val browserIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_BROWSER)
            .setPackage(pkg)
        pm.queryIntentActivities(browserIntent, 0).isNotEmpty()
    }.getOrDefault(false)
}

/**
 * 已知浏览器包名。
 *
 * 用于兼底未声明 CATEGORY_APP_BROWSER 的真浏览器，避免把它们误判为非浏览器。
 * 不含应用宝（com.tencent.android.qqdownloader）、微信、淘宝等应用商店/社交类应用。
 */
private val KNOWN_BROWSERS = setOf(
    "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
    "com.microsoft.emmx", "org.mozilla.firefox", "org.mozilla.firefox_beta",
    "com.brave.browser", "com.opera.browser", "com.opera.mini.native",
    "com.sec.android.app.sbrowser", "org.chromium.chrome",
    "com.UCMobile", "com.uc.browser", "com.quark.browser", "com.tencent.mtt",
    "com.baidu.browser.apps", "com.baidu.searchbox.lite", "com.vivo.browser",
    "com.miui.browser", "com.android.browser", "com.oppo.browser",
    "com.heytap.browser", "com.coloros.browser", "com.bbk.browser",
    "com.transsion.phoenix", "com.ecosia.android", "com.duckduckgo.mobile.android",
    "org.mozilla.focus", "com.yandex.browser", "com.qwant.mobile",
    "com.browser", "com.androidhtmlviewer", "org.lineageos.jelly",
    // 专为轻量/第三方浏览器留位；不收录应用宝、微信、淘宝、QQ 等
    "com.kiwibrowser.browser", "com.explore.web.browser", "com.hsv.freeadblockerbrowser",
    "com.jamal2367.maxthonbrowser", "com.leesc.chrome", "org.chromium.webview_shell"
)

/**
 * 推荐分类卡片：图标 + 分类名 + 条目数 + 进入箭头。
 *
 * 与条目卡片同一套视觉语言（同圆角、同内边距），但用 secondaryContainer
 * 图标底板区分层级，一眼能看出它不是单个应用。
 */
@Composable
private fun RecommendationCategoryCard(
    meta: RecommendCategoryMeta,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(meta.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(meta.titleRes), fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    stringResource(R.string.recommend_cat_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 法规与服务调整警示牌。
 *
 * 用 errorContainer 而非 error，保证在深浅色下都能读且不至于像错误弹窗那样刺眼。
 */
@Composable
private fun RecommendationServiceNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Icon(
                Icons.Rounded.WarningAmber,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.recommend_notice_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.recommend_notice_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 下载稳定性提示。
 *
 * 用 surfaceContainerHigh 而非 errorContainer：这只是使用建议，不是警告，
 * 与上方的法规警示牌在视觉上要能区分开。
 */
@Composable
private fun RecommendationNetworkNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Icon(Icons.Rounded.Info, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.recommend_network_tip),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
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
                Text(if (app.isApk) stringResource(R.string.dl_title) else stringResource(R.string.dl_open_browser))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
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
        title = { Text(stringResource(notice.title)) },
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
                Text(stringResource(notice.confirmLabel))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}

/**
 * 下载进度弹窗的宿主：把下载记录的订阅限定在弹窗作用域内，
 * 避免进度高频更新波及上层的推荐卡片列表。
 */
@Composable
private fun RecommendationDownloadDialogHost(
    app: RecommendedApp,
    downloads: kotlinx.coroutines.flow.Flow<List<DownloadEntity>>,
    downloadId: String?,
    onInstall: (DownloadEntity) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val records by downloads.collectAsState(initial = emptyList())
    val record = remember(records, downloadId) { records.firstOrNull { it.id == downloadId } }
    RecommendationDownloadDialog(
        app = app,
        record = record,
        finished = record?.status == "completed" || record?.status == "hash_mismatch",
        onInstall = { record?.let(onInstall) },
        onDismiss = onDismiss,
        // 取消：停任务并删掉记录与已写入文件，避免半成品残留。
        // 不依赖 record 是否已回填——记录尚未就绪时按 URL 定位任务。
        onCancel = { onCancelDownload(); onDismiss() }
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
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    val failed = record?.status == "failed" || record?.status == "cancelled"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s072, app.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (finished) {
                    Text(stringResource(R.string.s073))
                    record?.sha256?.takeIf { it.isNotBlank() }?.let { hash ->
                        Text("SHA-256：$hash", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (failed) {
                    Text(record?.errorMessage?.ifBlank { stringResource(R.string.dl_status_failed) } ?: stringResource(R.string.dl_status_failed))
                } else {
                    val progress = record?.progress ?: 0
                    if (progress > 0) {
                        // 确定态：沿用原来的平滑波浪进度条
                        WavyProgressBar(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.s075, progress))
                    } else {
                        // 连接中：不确定态波浪进度条，无限滚动
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), amplitude = 0.5f, wavelength = 24.dp)
                        Text(stringResource(R.string.s076))
                    }
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = onInstall) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.s077)) }
            }
        },
        dismissButton = {
            Row {
                if (!finished) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.s078)) }
                }
                TextButton(onClick = onDismiss) { Text(if (finished) stringResource(R.string.btn_close) else stringResource(R.string.btn_bg_download)) }
            }
        }
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
                Text(stringResource(R.string.s062), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.s081), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 地址栏的护盾标识。
 *
 * 图标随结论变化：安全为实心护盾，未加密为开口锁，可疑为感叹护盾，危险为尖叉护盾。
 * 颜色同步变化，点击后由父层弹窗解释具体原因。
 */
@Composable
private fun ShieldBadge(
    assessment: SecurityScanner.RiskAssessment,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val (icon, tint) = when (assessment.level) {
        SecurityScanner.RiskLevel.SAFE -> Icons.Rounded.GppGood to MaterialTheme.colorScheme.primary
        SecurityScanner.RiskLevel.INSECURE -> Icons.Rounded.LockOpen to MaterialTheme.colorScheme.tertiary
        SecurityScanner.RiskLevel.SUSPICIOUS -> Icons.Rounded.GppMaybe to MaterialTheme.colorScheme.tertiary
        SecurityScanner.RiskLevel.DANGEROUS -> Icons.Rounded.GppBad to MaterialTheme.colorScheme.error
    }
    val label = when (assessment.level) {
        SecurityScanner.RiskLevel.SAFE -> context.getString(R.string.risk_safe)
        SecurityScanner.RiskLevel.INSECURE -> assessment.reason(context).ifBlank { context.getString(R.string.risk_insecure) }
        SecurityScanner.RiskLevel.SUSPICIOUS -> assessment.reason(context).ifBlank { context.getString(R.string.risk_suspicious) }
        SecurityScanner.RiskLevel.DANGEROUS -> assessment.reason(context).ifBlank { context.getString(R.string.risk_dangerous) }
    }
    // compact 时一并缩交互区：IconButton 默认最小 48dp，在 48dp 容器里会顶满甚至溢出
    IconButton(onClick = onClick, modifier = Modifier.size(if (compact) 36.dp else 40.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(if (compact) 20.dp else 22.dp))
    }
}

/**
 * 护盾说明弹窗：把判定依据展开讲清楚，避免只给一个颜色让用户猜。
 */
@Composable
private fun SecurityDetailDialog(
    host: String,
    assessment: SecurityScanner.RiskAssessment,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val title = when (assessment.level) {
        SecurityScanner.RiskLevel.SAFE -> context.getString(R.string.risk_safe)
        SecurityScanner.RiskLevel.INSECURE -> context.getString(R.string.risk_insecure)
        SecurityScanner.RiskLevel.SUSPICIOUS -> context.getString(R.string.risk_suspicious)
        SecurityScanner.RiskLevel.DANGEROUS -> context.getString(R.string.risk_dangerous)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (host.isNotBlank()) {
                    Text(host, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    assessment.detail(context).ifBlank {
                        if (assessment.level == SecurityScanner.RiskLevel.SAFE) {
                            context.getString(R.string.risk_no_risk)
                        } else ""
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s087)) } }
    )
}

/**
 * 风险站点拦截弹窗：必须由用户明确选择是否继续。
 *
 * 默认动作是「返回」，避免误触后直接进入风险页面。
 */
@Composable
private fun RiskySiteDialog(
    host: String,
    assessment: SecurityScanner.RiskAssessment,
    onProceed: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onBack,
        icon = { Icon(Icons.Rounded.Phishing, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(assessment.reason(context).ifBlank { stringResource(R.string.risk_default_title) }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (host.isNotBlank()) {
                    Text(host, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(assessment.detail(context).ifBlank { stringResource(R.string.risk_default_detail) })
            }
        },
        confirmButton = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.s063)) }
        },
        dismissButton = {
            TextButton(onClick = onProceed) { Text(stringResource(R.string.s090)) }
        }
    )
}

@Composable
private fun AddressInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onRemoveHistory: (String) -> Unit = {},
    // 是否启用联想与历史下拉：首页需要，浏览器地址栏不需要（会遮挡网页）
    showSuggestionsPanel: Boolean = true,
    // 当前页面的安全结论；为 null 时不显示护盾（首页/空标签）
    security: SecurityScanner.RiskAssessment? = null,
    onSecurityClick: () -> Unit = {},
    // 紧凑模式：浏览器页地址栏用更矮的输入框，把竖向空间留给网页内容与加载条
    compact: Boolean = false
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

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // 紧凑模式压到 52dp：M3 文本框自然高度是 OutlinedTextFieldDefaults.MinHeight（56dp）。
            // 这里用固定 height 覆盖（低于 min 的 heightIn 不起作用）。
            // 不能再矮：value:String 重载没有 contentPadding 参数，默认垂直内边距固定 16dp，
            // 文字行高约 20dp，16+20+16 恰好 52dp，再压就会把字形裁掉。
            modifier = Modifier.fillMaxWidth()
                .then(if (compact) Modifier.height(52.dp) else Modifier.heightIn(min = 56.dp))
                .onFocusChanged { focused = it.isFocused; onFocusChange(it.isFocused) },
            placeholder = { Text(stringResource(R.string.s091), style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = if (security != null) {
                {
                    ShieldBadge(assessment = security, onClick = onSecurityClick, compact = compact)
                }
            } else {
                // 无护盾时用放大镜占位，保持左侧图标位置稳定，避免输入前后文字左右跳动
                { Icon(Icons.Default.Search, null, modifier = Modifier.size(if (compact) 20.dp else 24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotBlank()) {
                        // 不加 size 的 IconButton 最小交互区是 48dp，会与 48dp 容器顶满；
                        // 显式限制为 36dp，避免撑高与视觉拥挤
                        IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, stringResource(R.string.s092), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { if (value.isNotBlank()) onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                // 线框风：常态细描边、聚焦时换主色
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                // 描边内部给一层比页面底色略深的容器色，输入内容与描边不会叠在一起；
                // OutlinedTextField 自身会把容器背景裁到 shape（RoundedCornerShape(26.dp)）内，
                // 因此不会溢出描线之外
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                                    Icon(Icons.Default.Close, stringResource(R.string.s094), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun BookmarkGrid(sites: List<QuickSite>, onOpen: (String) -> Unit, onEdit: (QuickSite) -> Unit, onDelete: (QuickSite) -> Unit) {
    // 用普通 Column 而非 LazyColumn：本组件始终嵌在带 verticalScroll 的首页里，
    // 纵向可滚动组件不能在无限高度约束下测量（会直接抛异常导致启动闪退）；
    // 书签数量本就很少，无需懒加载。
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sites.isEmpty()) {
            // 空态：给一张虚线感的提示卡，而不是一行灰字，避免首页下半部分显得残缺
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Bookmarks,
                        null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.s095),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            sites.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { site ->
                        BookmarkCard(site = site, onOpen = onOpen, onEdit = onEdit, onDelete = onDelete, modifier = Modifier.weight(1f))
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 书签卡片：长按弹菜单（编辑/删除） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkCard(site: QuickSite, onOpen: (String) -> Unit, onEdit: (QuickSite) -> Unit, onDelete: (QuickSite) -> Unit, modifier: Modifier = Modifier) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().height(88.dp)
        ) {
            // 长按弹菜单；点击打开书签
            Row(
                Modifier.fillMaxSize()
                    .combinedClickable(onClick = { onOpen(site.url) }, onLongClick = { menuExpanded = true })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 首字母徽标：比直接放大号字符更整齐，也不会因 emoji/多字标题错位
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            site.icon.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(site.title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        Uri.parse(site.url).host?.removePrefix("www.") ?: site.url,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s096)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuExpanded = false; onEdit(site) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s097)) },
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
    var editingBookmark by remember { mutableStateOf<QuickSite?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.s098)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } },
            actions = {
                IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, stringResource(R.string.s099)) }
            }
        )
    }) { padding ->
        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.s100), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.s101), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddDialog = true }) { Text(stringResource(R.string.s099)) }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(bookmarks, key = { it.url }) { site ->
                    BookmarkManageRow(
                        site = site,
                        onEdit = { editingBookmark = site },
                        onDelete = { setQuickSites(encodeBookmarks(bookmarks.filter { it.url != site.url })) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        BookmarkDialog(
            onSubmit = { site ->
                // 同一网址不重复添加，避免首页出现重复卡片
                if (bookmarks.none { it.url == site.url }) setQuickSites(encodeBookmarks(bookmarks + site))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    val editing = editingBookmark
    if (editing != null) {
        BookmarkDialog(
            initial = editing,
            onSubmit = { site ->
                setQuickSites(encodeBookmarks(bookmarks.map { if (it.url == editing.url) site else it }))
                editingBookmark = null
            },
            onDismiss = { editingBookmark = null }
        )
    }
}

/** 书签管理列表行：长按弹菜单（编辑/删除） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkManageRow(site: QuickSite, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                text = { Text(stringResource(R.string.s096)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuExpanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s097)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

/** 书签对话框：initial 为 null 时是「添加」，否则是「编辑」并预填当前值 */
@Composable
private fun BookmarkDialog(initial: QuickSite? = null, onSubmit: (QuickSite) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var url by remember(initial) { mutableStateOf(initial?.url ?: "") }
    val isEdit = initial != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEdit) R.string.s096 else R.string.s099)) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.s102)) })
                OutlinedTextField(url, { url = it }, label = { Text(stringResource(R.string.s103)) })
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank() && url.isNotBlank()) onSubmit(QuickSite(title, url, title.take(1))) }) { Text(stringResource(if (isEdit) R.string.s104 else R.string.s061)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}

@Composable
private fun BrowserScreen(viewModel: BrowserViewModel, settings: BrowserSettings, settingsStore: SettingsStore, onTabs: (Boolean) -> Unit, onDownloads: () -> Unit, onSettings: () -> Unit, onIncognitoHome: () -> Unit) {
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
    // 风险站点拦截：目标地址、扫描结论、用户选择继续后的回调
    var riskySiteTarget by remember { mutableStateOf<String?>(null) }
    var riskySiteAssessment by remember { mutableStateOf<SecurityScanner.RiskAssessment?>(null) }
    var riskySiteProceed by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // 用户拒绝了通知权限：前台服务仍会运行，但通知栏里看不到任何下载进度
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.toast_notif_denied), Toast.LENGTH_LONG).show()
        }
    }
    // 网页文件选择：单/多选两个启动器，选中后回传给网页。
    // 关键：必须先把 content:// 复制成本地 file:// 再回传（原因见 copyUriToCacheForUpload）。
    var filePickerCallback by remember { mutableStateOf<((Array<Uri>?) -> Unit)?>(null) }
    val filePickerSingle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = filePickerCallback; filePickerCallback = null
        if (callback != null) {
            if (uri == null) {
                callback(null)
            } else {
                scope.launch {
                    val local = withContext(Dispatchers.IO) { copyUriToCacheForUpload(context, uri) }
                    callback(if (local != null) arrayOf(local) else null)
                }
            }
        }
    }
    val filePickerMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val callback = filePickerCallback; filePickerCallback = null
        if (callback != null) {
            if (uris.isNullOrEmpty()) {
                callback(null)
            } else {
                scope.launch {
                    val locals = withContext(Dispatchers.IO) { uris.mapNotNull { copyUriToCacheForUpload(context, it) } }
                    callback(if (locals.isNotEmpty()) locals.toTypedArray() else null)
                }
            }
        }
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
                Toast.makeText(context, context.getString(R.string.toast_bookmark_deleted), Toast.LENGTH_SHORT).show()
            } else {
                // 标题为空或为默认值时回退到域名，避免出现一批「新标签页」书签
                val title = selectedTab?.title?.takeIf { it.isNotBlank() && it != context.getString(R.string.s107) }
                    ?: Uri.parse(currentUrl).host?.removePrefix("www.")
                    ?: currentUrl
                scope.launch {
                    settingsStore.quickSites(encodeBookmarks(currentBookmarks + QuickSite(title, currentUrl, title.take(1))))
                }
                Toast.makeText(context, context.getString(R.string.toast_bookmark_added), Toast.LENGTH_SHORT).show()
            }
        }
        showBrowserMenu = false
    }

    viewModel.onExternalDownload = { url -> scope.launch { downloadUrl = url } }

    // 安全防护：把模式同步给 ViewModel，并接管风险站点的拦截弹窗
    LaunchedEffect(settings.securityMode) { viewModel.securityMode = settings.securityMode }
    viewModel.riskySiteHandler = { url, assessment, proceed ->
        riskySiteTarget = url
        riskySiteAssessment = assessment
        riskySiteProceed = proceed
    }
    // 正在展示的护盾说明
    var securityDetail by remember { mutableStateOf<Pair<String, SecurityScanner.RiskAssessment>?>(null) }
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
                .onFailure { Toast.makeText(context, context.getString(R.string.toast_no_app), Toast.LENGTH_SHORT).show() }
            else -> {
                pendingExternalApp = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { Toast.makeText(context, context.getString(R.string.toast_no_app), Toast.LENGTH_SHORT).show() }
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
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 0.dp)) {
                AddressInput(
                    value = input,
                    onValueChange = { input = it },
                    onSubmit = { viewModel.load(input) },
                    onFocusChange = { inputFocused = it },
                    showSuggestionsPanel = false,
                    // 浏览器地址栏比较缩：顶部紧贴状态栏，把高度让给网页与加载条；
                    // 无痕提示条、进度条、网页内容都能多出一行可视区域
                    compact = true,
                    // 仅在已加载的 http(s) 页面上显示护盾，空白页/内部页不显示
                    security = selectedTab?.risk?.takeIf {
                        val u = selectedTab.url
                        u.startsWith("http", ignoreCase = true)
                    },
                    onSecurityClick = {
                        val tab = selectedTab
                        if (tab != null) {
                            securityDetail = Uri.parse(tab.url).host.orEmpty() to tab.risk
                        }
                    }
                )
            }
        },
        bottomBar = {
            GlassSurface(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)) {
                IconButton(onClick = viewModel::back, enabled = selectedTab?.canGoBack == true) { Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back)) }
                IconButton(onClick = viewModel::forward, enabled = selectedTab?.canGoForward == true) { Icon(Icons.Default.ArrowForward, stringResource(R.string.action_forward)) }
                // 加载中时变为「停止」：避免用户在卡住的页面无法中断加载；加载完成后恢复为「刷新」
                if (selectedTab?.loading == true) {
                    IconButton(onClick = viewModel::stopLoading) { Icon(Icons.Default.Close, stringResource(R.string.action_stop_loading)) }
                } else {
                    IconButton(onClick = viewModel::reload) { Icon(Icons.Default.Refresh, stringResource(R.string.s112)) }
                }
                IconButton(onClick = { showNewTabDialog = true }) { Icon(Icons.Default.Add, stringResource(R.string.s113)) }
                IconButton(onClick = { onTabs(selectedTab?.incognito == true) }) {
                    BadgedBox(badge = { Badge { Text(visibleTabs.size.toString()) } }) { Icon(Icons.Default.Tab, stringResource(R.string.s114)) }
                }
                // 无痕标签标识：标签栏里无痕与普通两个 chip 外观一致，
                // 不标一下用户分不清当前在看哪种标签。不跨标签切换时永远显示，
                // 颜色走主题色（tertiary）而不是专属紫，不干扰用户的配色设置
                if (selectedTab?.incognito == true) {
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        stringResource(R.string.s124),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Box {
                    IconButton(onClick = { showBrowserMenu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.s115)) }
                    DropdownMenu(expanded = showBrowserMenu, onDismissRequest = { showBrowserMenu = false }, offset = androidx.compose.ui.unit.DpOffset(0.dp, 48.dp)) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.s116)) }, leadingIcon = { if (selectedTab?.session?.settings?.userAgentOverride?.contains("X11") == true) Icon(Icons.Default.Check, null) else Icon(Icons.Default.Computer, null) }, onClick = { viewModel.toggleDesktopUserAgent(); showBrowserMenu = false })
                        DropdownMenuItem(
                            text = { Text(stringResource(if (bookmarked) R.string.s097 else R.string.s099)) },
                            leadingIcon = { Icon(if (bookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd, null) },
                            onClick = toggleBookmark
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.s117)) }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = { showBrowserMenu = false; onDownloads() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.s118)) }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { showBrowserMenu = false; onSettings() })
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
                                Text(tab.title.ifBlank { stringResource(R.string.s107) }, maxLines = 1)
                                Icon(Icons.Default.Close, stringResource(R.string.s119), Modifier.size(16.dp).clickable { viewModel.close(tab.id) })
                            }
                        },
                        leadingIcon = { if (tab.id == selectedId) Icon(Icons.Default.Check, null) }
                    )
                }
            }
            if (selectedTab != null) {
                if (selectedTab.loading) Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    WavyProgressBar(progress = selectedTab.progress / 100f, modifier = Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    // 视图销毁（切换硬件加速、离开浏览页）时清除挂载标记，
                    // 否则视图重建后会话其实尚未挂载，loadUri 会被静默丢弃并停在空白页。
                    DisposableEffect(selectedTab.id, settings.hardwareAcceleration) {
                        onDispose { viewModel.onSessionDetached(selectedTab.id) }
                    }
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
            title = { Text(stringResource(R.string.s113)) },
            text = { Text(stringResource(R.string.s122)) },
            confirmButton = { TextButton(onClick = { viewModel.newTab(); showNewTabDialog = false; onTabs(false) }) { Text(stringResource(R.string.s123)) } },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.newTab(incognito = true)
                    showNewTabDialog = false
                    // 不再跳标签管理，而是进入独立的无痕浏览页
                    onIncognitoHome()
                }) { Text(stringResource(R.string.s124)) }
            }
        )
    }
    // 护盾说明弹窗
    securityDetail?.let { (host, assessment) ->
        SecurityDetailDialog(
            host = host,
            assessment = assessment,
            onDismiss = { securityDetail = null }
        )
    }
    // 风险站点拦截弹窗：默认「返回」，避免误触进入风险页
    val riskyTarget = riskySiteTarget
    val riskyAssessment = riskySiteAssessment
    if (riskyTarget != null && riskyAssessment != null) {
        RiskySiteDialog(
            host = Uri.parse(riskyTarget).host.orEmpty(),
            assessment = riskyAssessment,
            onProceed = {
                val proceed = riskySiteProceed
                riskySiteTarget = null
                riskySiteAssessment = null
                riskySiteProceed = null
                proceed?.invoke()
            },
            onBack = {
                riskySiteTarget = null
                riskySiteAssessment = null
                riskySiteProceed = null
            }
        )
    }
    if (downloadUrl != null) {
        DownloadDialog(
            url = downloadUrl!!,
            skipResolve = viewModel.hasCapturedBody(downloadUrl!!),
            onDownload = { fileName, expected ->
                expectedHash = expected
                selectedFileName = fileName
                viewModel.enqueueDownload(downloadUrl!!, selectedFileName, expectedHash)
                Toast.makeText(context, context.getString(R.string.toast_download_started), Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                downloadUrl = null
            },
            onDismiss = { downloadUrl = null }
        )
    }
    if (hashResult != null) {
        AlertDialog(
            onDismissRequest = { hashResult = null },
            title = { Text(stringResource(R.string.s126)) },
            text = { Text(hashResult!!) },
            confirmButton = { TextButton(onClick = { hashResult = null }) { Text(stringResource(R.string.s127)) } }
        )
    }
    // 先取出局部变量：pendingExternalAppUri 是委托属性，不能直接做智能转换
    val pendingUri = pendingExternalAppUri
    if (pendingUri != null) {
        AlertDialog(
            onDismissRequest = { pendingExternalAppUri = null; pendingExternalApp = null },
            title = { Text(stringResource(R.string.s128)) },
            text = { Text(stringResource(R.string.s129, pendingUri)) },
            confirmButton = { TextButton(onClick = { pendingExternalApp?.invoke(); pendingExternalAppUri = null; pendingExternalApp = null }) { Text(stringResource(R.string.s093)) } },
            dismissButton = { TextButton(onClick = { pendingExternalAppUri = null; pendingExternalApp = null }) { Text(stringResource(R.string.s071)) } }
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
            title = { Text(stringResource(R.string.s130)) },
            text = { Text(stringResource(R.string.s131, siteHost(site))) },
            confirmButton = { TextButton(onClick = { finishClipboard(true, true) }) { Text(stringResource(R.string.s047)) } },
            dismissButton = { TextButton(onClick = { finishClipboard(false, true) }) { Text(stringResource(R.string.s048)) } }
        )
    }
}

@Composable
private fun TabOverview(viewModel: BrowserViewModel, initialIncognito: Boolean = false, onBack: () -> Unit, onExitIncognito: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    var incognitoOnly by remember { mutableStateOf(initialIncognito) }
    val displayedTabs = tabs.filter { it.incognito == incognitoOnly }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.s132)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } },
                actions = {
                    TextButton(onClick = {
                        if (incognitoOnly) {
                            // 退出无痕：关闭全部无痕标签并清 Cookie/缓存，然后直接回首页
                            // （不能只走 onBack，无痕标签已全部消失，停在标签页没有意义）
                            viewModel.exitIncognito()
                            onExitIncognito()
                        } else {
                            viewModel.closeAllNormal()
                        }
                    }) { Text(stringResource(if (incognitoOnly) R.string.incognito_exit_all else R.string.s134)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row { AssistChip(onClick = { incognitoOnly = false }, label = { Text(stringResource(R.string.s123)) }, leadingIcon = { if (!incognitoOnly) Icon(Icons.Default.Check, null) }); Spacer(Modifier.width(8.dp)); AssistChip(onClick = { incognitoOnly = true }, label = { Text(stringResource(R.string.s124)) }, leadingIcon = { if (incognitoOnly) Icon(Icons.Default.Check, null) }) } }
            if (displayedTabs.isEmpty()) item { Text(stringResource(if (incognitoOnly) R.string.s135 else R.string.s136)) }
            items(displayedTabs, key = { it.id }) { tab ->
                ElevatedCard(onClick = { viewModel.select(tab.id); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Language, null) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.fillMaxWidth(0.72f)) {
                            Text(tab.title.ifBlank { stringResource(R.string.s107) }, maxLines = 1)
                            Text(tab.url.ifEmpty { stringResource(R.string.s107) }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.close(tab.id) }) { Icon(Icons.Default.Close, stringResource(R.string.s079)) }
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
    Scaffold(topBar = { TopAppBar(title = { Text(if (selectedIds.isEmpty()) stringResource(R.string.s117) else stringResource(R.string.s137, selectedIds.size)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }, actions = { if (selectedIds.isNotEmpty()) IconButton(onClick = { showDeleteConfirmation = true }) { Icon(Icons.Default.Delete, stringResource(R.string.s138)) } }) }) { padding ->
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.s139)) }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { TextButton(onClick = { selectedIds = if (selectedIds.size == downloads.size) emptySet() else downloads.map { it.id }.toSet() }) { Text(stringResource(if (selectedIds.size == downloads.size) R.string.s140 else R.string.s141)) } }
                items(downloads, key = { it.id }) { item -> DownloadRecordCard(item, item.id in selectedIds, { selectedIds = selectedIds.toggle(item.id) }, { viewModel.cancelDownload(item.id) }, { viewModel.pauseDownload(item.id) }, { viewModel.resumeDownload(item.id) }, { openDownload(context, item) }, { showDeleteConfirmation = true; selectedIds = setOf(item.id) }) }
            }
        }
    }
    if (showDeleteConfirmation) AlertDialog(onDismissRequest = { showDeleteConfirmation = false }, title = { Text(stringResource(R.string.s142)) }, text = { Column { Text(stringResource(R.string.s143, selectedIds.size)) ; CheckRow(stringResource(R.string.s144), deleteFiles) { deleteFiles = it } } }, confirmButton = { TextButton(onClick = { downloads.filter { it.id in selectedIds }.forEach { viewModel.deleteDownload(it.id, it.targetUri, deleteFiles) }; Toast.makeText(context, context.getString(R.string.s050), Toast.LENGTH_SHORT).show(); selectedIds = emptySet(); showDeleteConfirmation = false }) { Text(stringResource(R.string.s145)) } }, dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.s071)) } })
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

@Composable
private fun HistoryScreen(viewModel: BrowserViewModel, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState(initial = emptyList())
    var showClearConfirmation by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.s058)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }, actions = { if (history.isNotEmpty()) TextButton(onClick = { showClearConfirmation = true }) { Text(stringResource(R.string.s146)) } }) }) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(stringResource(R.string.s147)) }
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
        ConfirmDialog(stringResource(R.string.s148), stringResource(R.string.s149), {
            viewModel.clearHistory()
            Toast.makeText(context, context.getString(R.string.s150), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(context, context.getString(R.string.toast_no_installer), Toast.LENGTH_SHORT).show()
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
            context.startActivity(Intent.createChooser(action, context.getString(R.string.chooser_open_file)))
        }
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_file_app), Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, context.getString(R.string.toast_allow_unknown), Toast.LENGTH_SHORT).show()
    }
}

private fun copyDownloadLink(context: android.content.Context, url: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
    // 非 @Composable 函数不能调用 stringResource，改用 Context.getString
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.s155), url))
}

@Composable
private fun DownloadRecordCard(item: DownloadEntity, selected: Boolean, onSelect: () -> Unit, onCancel: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val running = item.status == "queued" || item.status == "downloading"
    val paused = item.status == "paused"
    var menuExpanded by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = selected, onCheckedChange = { onSelect() })
            Column(Modifier.fillMaxWidth(0.78f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.fileName, style = MaterialTheme.typography.titleMedium)
                Text(downloadStatus(context, item), style = MaterialTheme.typography.bodySmall)
                if (running || paused) {
                    WavyProgressBar(progress = item.progress / 100f, modifier = Modifier.fillMaxWidth())
                    Text("${item.progress}% · ${formatBytes(context, item.downloadedBytes)} / ${formatBytes(context, item.totalBytes)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (running) {
                            TextButton(onClick = onPause) { Icon(Icons.Default.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.s156)) }
                        } else {
                            TextButton(onClick = onResume) { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.s157)) }
                        }
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.s078)) }
                    }
                } else if (item.status == "completed" || item.status == "hash_mismatch") {
                    IconButton(onClick = onOpen) { Icon(Icons.Default.OpenInNew, stringResource(R.string.s152)) }
                }
                if (item.errorMessage.isNotBlank()) Text(item.errorMessage, color = MaterialTheme.colorScheme.error)
                if (item.sha256.isNotBlank()) Text("SHA-256：${item.sha256}", style = MaterialTheme.typography.bodySmall)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.s115)) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text(stringResource(R.string.s145)) }, onClick = { menuExpanded = false; onDelete() }) }
            }
        }
    }
}

private fun downloadStatus(context: Context, item: DownloadEntity): String = when (item.status) {
    "queued" -> context.getString(R.string.dl_status_queued)
    "downloading" -> context.getString(R.string.dl_status_downloading)
    "paused" -> context.getString(R.string.dl_status_paused)
    "completed" -> context.getString(R.string.dl_status_completed)
    "hash_mismatch" -> context.getString(R.string.dl_status_hash_mismatch)
    "cancelled" -> context.getString(R.string.dl_status_cancelled)
    else -> context.getString(R.string.dl_status_failed)
}

private fun formatBytes(context: Context, value: Long): String = when {
    value < 0 -> context.getString(R.string.common_unknown)
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
    onLanguage: () -> Unit,
    onSiteSettings: () -> Unit,
    onCookieSettings: () -> Unit,
    onHttpsOnly: () -> Unit,
    onWebPermissions: () -> Unit,
    onSecurity: () -> Unit,
    onAdBlock: () -> Unit,
    onNotification: () -> Unit,
    onAdvancedSettings: () -> Unit,
    onNetworkDiagnostics: () -> Unit,
    onAbout: () -> Unit,
    onContactUs: () -> Unit,
    onBack: () -> Unit
) {
    var showLinkDiagnostics by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // 设置搜索：按关键词过滤上方入口卡片
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // 搜索索引：子选项 -> 所属页面。搜索命中后直接展示子选项，点击即跳转到对应页面。
    // 不用 remember：内部需调用 stringResource，而 remember 的 lambda 不是组合上下文。
    // 条目只有十几个，每次重组重建的开销可忽略。
    val searchIndex = listOf(
        SettingSearchItem(stringResource(R.string.s165), SearchGroup.GENERAL, listOf("主题", "浅色", "深色", "夜间", "跟随系统", "亮色", "暗色", "theme", "light", "dark", "night", "system"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s173), SearchGroup.LANGUAGE, listOf("语言", "language", "中文", "英文", "繁体", "简体", "chinese", "english"), onLanguage),
        SettingSearchItem(stringResource(R.string.color_theme_section), SearchGroup.GENERAL, listOf("颜色", "主题", "配色", "取色", "动态", "莫奈", "Monet", "自定义颜色", "主题色", "color", "theme", "palette", "monet"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s184), SearchGroup.GENERAL, listOf("硬件加速", "渲染", "性能", "GPU", "卡顿", "流畅", "掉帧", "加速", "hardware", "acceleration", "render", "performance", "lag", "smooth"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s117), SearchGroup.GENERAL, listOf("下载", "记录", "下载记录", "文件", "进度", "download", "record", "file", "progress"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s194), SearchGroup.GENERAL, listOf("路径", "保存位置", "存储", "目录", "下载位置", "下载路径", "path", "folder", "storage", "directory"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s201), SearchGroup.GENERAL, listOf("校验", "哈希", "SHA", "APK", "安全", "完整性", "sha256", "hash", "verify", "integrity"), onGeneralSettings),
        SettingSearchItem(stringResource(R.string.s206), SearchGroup.SITE, listOf("深色", "黑暗", "夜间", "网页", "暗色", "适配", "网页渲染", "网页深色", "dark", "web", "page", "render"), onSiteSettings),
        SettingSearchItem(stringResource(R.string.s213), SearchGroup.SITE, listOf("搜索", "引擎", "百度", "必应", "DuckDuckGo", "谷歌", "搜索引擎", "search", "engine", "google", "baidu", "bing"), onSiteSettings),
        SettingSearchItem(stringResource(R.string.s219), SearchGroup.SITE, listOf("浏览器标识", "UA", "User-Agent", "桌面", "标识", "网页标识", "桌面模式", "user agent", "desktop"), onSiteSettings),
        SettingSearchItem(stringResource(R.string.s224), SearchGroup.PRIVACY, listOf("Cookie", "追踪", "隐私", "数据", "Cookies", "tracking", "privacy", "data"), onCookieSettings),
        SettingSearchItem(stringResource(R.string.s228), SearchGroup.PRIVACY, listOf("缓存", "清除", "Cookie", "数据", "清理", "站点数据", "cache", "clear", "data", "site"), onCookieSettings),
        SettingSearchItem(stringResource(R.string.s232), SearchGroup.PRIVACY, listOf("退出", "清除", "数据", "无痕", "清理", "退出时清除", "exit", "clear", "incognito"), onCookieSettings),
        SettingSearchItem("HTTPS-Only", SearchGroup.PRIVACY, listOf("HTTPS", "加密", "安全", "协议", "Only", "强制加密", "隐私标签页", "encrypt", "secure", "protocol", "private"), onHttpsOnly),
        SettingSearchItem(stringResource(R.string.s240), SearchGroup.SECURITY, listOf("安全", "防护", "拦截", "诈骗", "木马", "护盾", "钓鱼", "风险", "银狐", "security", "protection", "block", "phishing", "malware", "risk"), onSecurity),
        SettingSearchItem(stringResource(R.string.ad_block_title), SearchGroup.AD_BLOCK, listOf("广告", "拦截", "广告拦截", "追踪", "跟踪", "弹窗", "推广", "adblock", "ad", "ads", "去广告", "block", "tracker", "popup"), onAdBlock),
        SettingSearchItem(stringResource(R.string.notification_title), SearchGroup.NOTIFICATION, listOf("通知", "实时", "流体云", "灵动岛", "实时活动", "状态栏", "notification", "live update", "live activity", "promoted"), onNotification),
        SettingSearchItem(stringResource(R.string.debug_section_advanced), SearchGroup.ADVANCED, listOf("高级", "高级设置", "实验", "开发者", "advanced", "experiment", "developer"), onAdvancedSettings),
        SettingSearchItem(stringResource(R.string.debug_title), SearchGroup.DEBUG, listOf("调试", "开发者", "诊断", "日志", "崩溃", "内核", "签名", "debug", "developer", "diagnostic", "log", "crash", "engine", "signature"), onAdvancedSettings),
        SettingSearchItem(stringResource(R.string.s249), SearchGroup.WEB_PERMISSIONS, listOf("剪贴板", "复制", "粘贴", "权限", "读取", "clipboard", "copy", "paste", "permission"), onWebPermissions),
        SettingSearchItem(stringResource(R.string.s053), SearchGroup.WEB_PERMISSIONS, listOf("外部", "跳转", "APP", "应用", "Intent", "权限", "external", "app", "link", "permission"), onWebPermissions),
        SettingSearchItem(stringResource(R.string.s259), SearchGroup.WEB_PERMISSIONS, listOf("麦克风", "录音", "权限", "语音", "音频", "microphone", "mic", "record", "audio", "permission"), onWebPermissions),
        SettingSearchItem(stringResource(R.string.s058), SearchGroup.HISTORY, listOf("历史", "访问", "记录", "足迹", "历史记录", "history", "visit", "visited"), onHistory),
        SettingSearchItem(stringResource(R.string.s098), SearchGroup.BOOKMARKS, listOf("书签", "收藏", "收藏夹", "书签管理", "首页", "bookmark", "bookmarks", "favorite", "home"), onBookmarks),
        SettingSearchItem(stringResource(R.string.s042), SearchGroup.DEFAULT_BROWSER, listOf("默认", "浏览器", "角色", "默认应用", "默认浏览器", "default", "browser", "role"), onSetDefaultBrowser, accent = true, subtitle = stringResource(R.string.set_default_sub)),
        SettingSearchItem(stringResource(R.string.netdiag_title), SearchGroup.NETWORK, listOf("网络", "诊断", "检测", "延迟", "DNS", "连通", "测速", "network", "diagnostics", "ping", "latency", "dns", "connectivity"), onNetworkDiagnostics),
        SettingSearchItem(stringResource(R.string.s277), SearchGroup.LINK_DIAGNOSTICS, listOf("外链", "诊断", "Intent", "跳转", "链接", "link", "diagnostics", "external"), { showLinkDiagnostics = true }),
        SettingSearchItem(stringResource(R.string.s281), SearchGroup.ABOUT, listOf("关于", "版本", "许可", "贡献者", "赞助", "更新", "about", "version", "license", "contributor", "sponsor", "update"), onAbout),
        SettingSearchItem(stringResource(R.string.s288), SearchGroup.CONTACT, listOf("联系", "反馈", "建议", "问题", "contact", "feedback", "suggestion", "issue"), onContactUs)
    )
    // 默认入口卡片（搜索框为空时展示，保持原有视觉）
    // 注意：remember 的 lambda 不是 @Composable 上下文，不能在里面调用 stringResource，
    // 所以先把需要的文案取成局部变量，再在 remember 里使用。
    val entryGeneral = stringResource(R.string.s166)
    val entryLanguage = stringResource(R.string.s173)
    val entryLanguageSub = stringResource(R.string.settings_language_sub)
    val entrySite = stringResource(R.string.s207)
    val entryWebsiteSub = stringResource(R.string.s295)
    val entryWebPerm = stringResource(R.string.s250)
    val entryWebPermSub = stringResource(R.string.s296)
    val entrySecurity = stringResource(R.string.s240)
    val entryHistory = stringResource(R.string.s058)
    val entryHistorySub = stringResource(R.string.s297)
    val entryBookmarks = stringResource(R.string.s098)
    val entryBookmarksSub = stringResource(R.string.s298)
    val entryDefaultBrowser = stringResource(R.string.s042)
    val entryDefaultBrowserSub = stringResource(R.string.s276)
    val entryLinkDiag = stringResource(R.string.s277)
    val entryLinkDiagSub = stringResource(R.string.s299)
    val entryNetDiag = stringResource(R.string.netdiag_title)
    val entryNetDiagSub = stringResource(R.string.netdiag_sub)
    val entryAbout = stringResource(R.string.s281)
    val entryAboutSub = stringResource(R.string.s300)
    val entryContact = stringResource(R.string.s288)
    val entryContactSub = stringResource(R.string.s301)
    val entryGeneralSub = stringResource(R.string.s293)
    val entryPrivacy = stringResource(R.string.s225)
    val entryAdBlock = stringResource(R.string.ad_block_title)
    val entryNotification = stringResource(R.string.notification_title)
    val entryNotificationSub = stringResource(R.string.notification_sub)
    val entryAdvanced = stringResource(R.string.debug_section_advanced)
    val entryAdvancedSub = stringResource(R.string.advanced_sub)
    // 调试入口只在内存解锁后才追加到列表末尾；未解锁时完全不出现
    val debugUnlocked = DebugUnlock.unlocked
    val entries = remember(isDefaultBrowser, settings.cookiesEnabled, settings.clearOnExit, debugUnlocked, entryGeneral, entryLanguage, entryLanguageSub, entrySite, entryWebsiteSub, entryWebPerm, entryWebPermSub, entrySecurity, entryHistory, entryHistorySub, entryBookmarks, entryBookmarksSub, entryDefaultBrowser, entryDefaultBrowserSub, entryLinkDiag, entryLinkDiagSub, entryNetDiag, entryNetDiagSub, entryAbout, entryAboutSub, entryContact, entryContactSub, entryGeneralSub, entryPrivacy, entryAdBlock, settings.adBlockEnabled, entryNotification, entryNotificationSub, entryAdvanced, entryAdvancedSub) {
        listOfNotNull(
            SettingsEntryItem(entryGeneral, entryGeneralSub, Icons.Rounded.Tune, onGeneralSettings),
            SettingsEntryItem(entryLanguage, entryLanguageSub, Icons.Rounded.Language, onLanguage),
            SettingsEntryItem(entrySite, entryWebsiteSub, Icons.Rounded.Language, onSiteSettings),
            SettingsEntryItem(entryPrivacy, cookieSettingsSubtitle(context, settings), Icons.Rounded.PrivacyTip, onCookieSettings),
            SettingsEntryItem(entryWebPerm, entryWebPermSub, Icons.Rounded.Security, onWebPermissions),
            SettingsEntryItem(entrySecurity, securityModeSubtitle(context, settings), Icons.Rounded.GppGood, onSecurity),
            SettingsEntryItem(entryAdBlock, adBlockSubtitle(context, settings), Icons.Rounded.Block, onAdBlock),
            SettingsEntryItem(entryNotification, entryNotificationSub, Icons.Rounded.Notifications, onNotification),
            SettingsEntryItem(entryHistory, entryHistorySub, Icons.Default.History, onHistory),
            SettingsEntryItem(entryBookmarks, entryBookmarksSub, Icons.Rounded.Bookmarks, onBookmarks),
            SettingsEntryItem(entryDefaultBrowser, entryDefaultBrowserSub, Icons.Rounded.Public, onSetDefaultBrowser, accent = true),
            SettingsEntryItem(entryNetDiag, entryNetDiagSub, Icons.Rounded.NetworkCheck, onNetworkDiagnostics),
            SettingsEntryItem(entryLinkDiag, entryLinkDiagSub, Icons.Default.Language, { showLinkDiagnostics = true }),
            SettingsEntryItem(entryAbout, entryAboutSub, Icons.Default.Info, onAbout),
            SettingsEntryItem(entryContact, entryContactSub, Icons.Rounded.ContactSupport, onContactUs),
            if (debugUnlocked) SettingsEntryItem(entryAdvanced, entryAdvancedSub, Icons.Rounded.Tune, onAdvancedSettings) else null
        ).filterNotNull().filter { !it.accent || !isDefaultBrowser }
    }
    // 搜索命中结果（仅在搜索框有输入时使用）
    // 调试相关条目只在内存解锁后才参与搜索，避免从搜索绕过连点解锁
    val filtered = remember(searchQuery, isDefaultBrowser, debugUnlocked) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else searchIndex.filter { item ->
            (debugUnlocked || (item.group != SearchGroup.DEBUG && item.group != SearchGroup.ADVANCED)) &&
                (!item.accent || !isDefaultBrowser) &&
                (item.title.contains(q, ignoreCase = true) || item.keywords.any { it.contains(q, ignoreCase = true) })
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.s059)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // 设置搜索框
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.s302)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.s214)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, stringResource(R.string.s092)) }
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
                        stringResource(R.string.s303),
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
                            subtitle = entry.subtitle ?: stringResource(entry.group.labelRes),
                            icon = entry.group.icon,
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
            title = { Text(stringResource(R.string.s277)) },
            text = { Text(if (info.isBlank()) stringResource(R.string.s304) else info) },
            confirmButton = { TextButton(onClick = { showLinkDiagnostics = false }) { Text(stringResource(R.string.s079)) } }
        )
    }
}

/** 隐私入口副标题：概括当前各项开关状态 */
private fun cookieSettingsSubtitle(context: Context, settings: BrowserSettings): String {
    val cookieState = context.getString(if (settings.cookiesEnabled) R.string.sub_cookie_on else R.string.sub_cookie_off)
    val exitState = context.getString(if (settings.clearOnExit) R.string.sub_exit_clear else R.string.sub_exit_keep)
    return "$cookieState · $exitState"
}

/** 安全防护入口副标题：显示当前模式 */
private fun securityModeSubtitle(context: Context, settings: BrowserSettings): String = when (settings.securityMode) {
    "enhanced" -> context.getString(R.string.sub_sec_enhanced)
    "basic" -> context.getString(R.string.sub_sec_basic)
    else -> context.getString(R.string.sub_sec_standard)
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

/**
 * 根据「颜色主题」设置解析出实际使用的配色。
 *
 * - dynamic：走系统动态取色（Android 12+）；旧系统或无动态取色时退回默认配色
 * - 预设：用种子色推导明暗两套完整色调
 * - custom：用户自选的种子色，非法值回退默认自定义色
 */
@Composable
private fun resolveColorScheme(settings: BrowserSettings, dark: Boolean, context: android.content.Context): androidx.compose.material3.ColorScheme {
    // 全程 runCatching 兜底：颜色推导一旦抛异常会直接闪退，
    // 宁可回退到默认配色也不能崩。
    return runCatching {
        val theme = ColorTheme.entries.firstOrNull { it.name.equals(settings.colorTheme, ignoreCase = true) }
            ?: ColorTheme.DYNAMIC
        if (theme == ColorTheme.DYNAMIC) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                if (dark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
            } else {
                if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
            }
        } else {
            val seed: Color
            val neutral: Color?
            if (theme == ColorTheme.CUSTOM) {
                seed = parseColorOrNull(settings.customColor) ?: defaultCustomSeed
                neutral = null
            } else {
                seed = presetSeeds[theme] ?: defaultCustomSeed
                neutral = presetNeutrals[theme]
            }
            if (dark) buildDarkScheme(seed, neutral) else buildLightScheme(seed, neutral)
        }
    }.getOrElse {
        if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.s127)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.s071)) } })
}

private fun searchEngineLabel(context: Context, settings: BrowserSettings) = when {
    settings.searchEngine == "baidu" -> context.getString(R.string.label_baidu)
    settings.searchEngine == "bing" -> context.getString(R.string.label_bing)
    settings.searchEngine == "duckduckgo" -> "DuckDuckGo"
    // 自定义搜索引擎的选中值是「custom:<标题>」，显示时去掉前缀只留标题
    settings.searchEngine.startsWith(CUSTOM_SEARCH_PREFIX) -> settings.searchEngine.removePrefix(CUSTOM_SEARCH_PREFIX)
    // 升级前配置的单个自定义搜索
    settings.searchEngine == "custom" -> settings.customSearchTitle
    else -> "Google"
}

private fun userAgentLabel(context: Context, settings: BrowserSettings) = when (settings.userAgentMode) {
    "android_chrome" -> "Android Chrome"
    "desktop_chrome" -> context.getString(R.string.label_desktop_chrome)
    "desktop_firefox" -> context.getString(R.string.label_desktop_firefox)
    "custom" -> settings.customUserAgentTitle
    else -> context.getString(R.string.label_default_geckoview)
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
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.s194)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }, actions = {
        if (!useExternal || externalUri.isNotBlank()) {
            TextButton(onClick = { onSave(if (useExternal) "external" else "internal", externalUri, externalPath) }) { Text(stringResource(R.string.s127)) }
        }
    }) }) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChoiceItem(stringResource(R.string.s315), !useExternal) { useExternal = false }
            Text(stringResource(R.string.s316), style = MaterialTheme.typography.bodySmall)
            ChoiceItem(stringResource(R.string.s317), useExternal) { useExternal = true }
            Text(externalPath.ifEmpty { stringResource(R.string.s318) }, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = useExternal, onClick = { treeLauncher.launch(null) }) { Text(stringResource(R.string.s319)) }
            Text(stringResource(R.string.s320), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SearchEngineScreen(
    settings: BrowserSettings,
    onSelect: (String) -> Unit,
    setEngines: (String) -> Unit,
    onBack: () -> Unit
) {
    val engines = listOf(
        "google" to "Google",
        "baidu" to stringResource(R.string.engine_baidu),
        "bing" to stringResource(R.string.engine_bing),
        "duckduckgo" to "DuckDuckGo"
    )
    val customEngines = remember(settings.customSearchEngines) { decodeCustomSearchEngines(settings.customSearchEngines) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomSearchEngine?>(null) }

    SelectionScreen(stringResource(R.string.s213), onBack, actions = {
        IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, stringResource(R.string.s321)) }
    }) {
        engines.forEach { (value, title) -> item { ChoiceItem(title, settings.searchEngine == value) { onSelect(value) } } }
        item { SectionHeader(stringResource(R.string.s037)) }
        if (customEngines.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.s322),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            // 不用标题做 key：编辑时可能改成与其它条目同名，重复 key 会直接崩溃
            items(customEngines) { engine ->
                CustomSearchEngineRow(
                    engine = engine,
                    selected = settings.searchEngine == engine.value,
                    onSelect = { onSelect(engine.value) },
                    onEdit = { editing = engine },
                    onDelete = {
                        setEngines(encodeCustomSearchEngines(customEngines.filter { it.title != engine.title }))
                        // 删除的是当前选中项时退回 Google，避免选中值指向已不存在的条目
                        if (settings.searchEngine == engine.value) onSelect("google")
                    }
                )
            }
        }
    }

    // 添加与编辑共用一个对话框：editing 为 null 表示新增
    if (showAddDialog) {
        CustomSearchEngineDialog(
            onSubmit = { engine ->
                if (customEngines.none { it.title == engine.title }) {
                    setEngines(encodeCustomSearchEngines(customEngines + engine))
                    onSelect(engine.value)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    val editingEngine = editing
    if (editingEngine != null) {
        CustomSearchEngineDialog(
            initial = editingEngine,
            onSubmit = { engine ->
                setEngines(encodeCustomSearchEngines(customEngines.map { if (it.title == editingEngine.title) engine else it }))
                // 改名后同步选中值，否则当前选中的条目会因标题变化而失效
                if (settings.searchEngine == editingEngine.value) onSelect(engine.value)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

/** 自定义搜索引擎行：点击选中，长按弹编辑/删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomSearchEngineRow(
    engine: CustomSearchEngine,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // 状态绑定到当前条目：列表按位置复用槽位时不会把上一行的展开状态带给另一行
    var menuExpanded by remember(engine) { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(engine.title, maxLines = 1) },
            supportingContent = { Text(engine.url, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
            leadingContent = { if (selected) Icon(Icons.Default.Check, null) },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onSelect, onLongClick = { menuExpanded = true })
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s323)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuExpanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s145)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

/** 自定义搜索引擎对话框：initial 为 null 时是「添加」，否则是「编辑」 */
@Composable
private fun CustomSearchEngineDialog(
    initial: CustomSearchEngine? = null,
    onSubmit: (CustomSearchEngine) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var url by remember(initial) { mutableStateOf(initial?.url ?: "") }
    val isEdit = initial != null
    // URL 必须含 {query} 占位符，否则搜索词无处可去
    val valid = title.isNotBlank() && url.contains("{query}")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEdit) R.string.s324 else R.string.s321)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.s102)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    url,
                    { url = it },
                    label = { Text(stringResource(R.string.s325)) },
                    supportingText = { Text(stringResource(R.string.s326)) },
                    isError = url.isNotBlank() && !url.contains("{query}"),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSubmit(CustomSearchEngine(title.trim(), url.trim())) }) {
                Text(stringResource(if (isEdit) R.string.s104 else R.string.s061))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}

@Composable
private fun UserAgentScreen(settings: BrowserSettings, onSelect: (String) -> Unit, onSimplified: (Boolean) -> Unit, onCustom: () -> Unit, onBack: () -> Unit) {
    val agents = listOf(
        "geckoview" to stringResource(R.string.s314),
        "android_chrome" to "Android Chrome",
        "desktop_chrome" to stringResource(R.string.ua_desktop_chrome),
        "desktop_firefox" to stringResource(R.string.ua_desktop_firefox)
    )
    val customCount = decodeCustomUserAgents(settings.customUserAgents).size
    SelectionScreen(stringResource(R.string.s219), onBack) {
        agents.forEach { (value, title) -> item { ChoiceItem(title, settings.userAgentMode == value) { onSelect(value) } } }
        item { SwitchRow(stringResource(R.string.s327), stringResource(R.string.s328), settings.simplifiedUserAgent, onSimplified, leading = Icons.Default.Computer) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s329)) }, supportingContent = { Text(if (customCount > 0) stringResource(R.string.s330, customCount) else stringResource(R.string.s331)) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onCustom)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomUserAgentScreen(
    settings: BrowserSettings,
    setUserAgents: (String) -> Unit,
    selectUserAgent: (String, String) -> Unit,
    resetUserAgent: () -> Unit,
    onBack: () -> Unit
) {
    val agents = remember(settings.customUserAgents) { decodeCustomUserAgents(settings.customUserAgents) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomUserAgent?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.s329)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } },
            actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, stringResource(R.string.s061)) } }
        )
    }) { padding ->
        if (agents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.s332), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.s333), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddDialog = true }) { Text(stringResource(R.string.s061)) }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(agents, key = { it.title }) { agent ->
                    val selected = settings.userAgentMode == "custom" && settings.customUserAgentTitle == agent.title
                    CustomUserAgentRow(
                        agent = agent,
                        selected = selected,
                        onSelect = { selectUserAgent(agent.title, agent.ua) },
                        onEdit = { editing = agent },
                        onDelete = {
                            setUserAgents(encodeCustomUserAgents(agents.filter { it.title != agent.title }))
                            if (selected) resetUserAgent()
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CustomUserAgentDialog(
            onSubmit = { agent ->
                if (agents.none { it.title == agent.title }) setUserAgents(encodeCustomUserAgents(agents + agent))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    val editingAgent = editing
    if (editingAgent != null) {
        CustomUserAgentDialog(
            initial = editingAgent,
            onSubmit = { agent ->
                setUserAgents(encodeCustomUserAgents(agents.map { if (it.title == editingAgent.title) agent else it }))
                // 若编辑的是当前选中的，同步更新生效内容
                if (settings.customUserAgentTitle == editingAgent.title) selectUserAgent(agent.title, agent.ua)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

/** 自定义 UA 列表行：点击选中，长按弹编辑/删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomUserAgentRow(agent: CustomUserAgent, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(agent.title, maxLines = 1) },
            supportingContent = { Text(agent.ua, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
            leadingContent = { if (selected) Icon(Icons.Default.Check, null) },
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onSelect, onLongClick = { menuExpanded = true })
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s323)) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuExpanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s145)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

/** 自定义 UA 对话框：initial 为 null 时是「添加」，否则是「编辑」 */
@Composable
private fun CustomUserAgentDialog(initial: CustomUserAgent? = null, onSubmit: (CustomUserAgent) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var ua by remember(initial) { mutableStateOf(initial?.ua ?: "") }
    val isEdit = initial != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEdit) R.string.s334 else R.string.s335)) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.s336)) }, singleLine = true)
                OutlinedTextField(ua, { ua = it }, label = { Text(stringResource(R.string.s337)) }, minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank() && ua.isNotBlank()) onSubmit(CustomUserAgent(title.trim(), ua.trim())) }) { Text(stringResource(if (isEdit) R.string.s104 else R.string.s061)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}

@Composable
private fun ClipboardPermissionScreen(settings: BrowserSettings, onSelect: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    SelectionScreen(stringResource(R.string.s249), onBack) {
        item { ChoiceItem(stringResource(R.string.s338), settings.clipboardReadPolicy == "ask") { onSelect("ask") } }
        item { ChoiceItem(stringResource(R.string.s047), settings.clipboardReadPolicy == "allow") { onSelect("allow") } }
        item { ChoiceItem(stringResource(R.string.s056), settings.clipboardReadPolicy == "deny") { onSelect("deny") } }
        // 说明文字：上下留白，避免贴到上方选项与下方按钮的边缘
        item { Text(stringResource(R.string.s339), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
        item { Spacer(Modifier.height(24.dp)) }
        item { OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text(stringResource(R.string.s340)) } }
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
    setApkHash: (Boolean) -> Unit,
    setHardwareAcceleration: (Boolean) -> Unit,
    setColorTheme: (String) -> Unit,
    setCustomColor: (String) -> Unit,
    onDownloads: () -> Unit,
    onDownloadDirectory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCustomColor by remember { mutableStateOf(false) }
    SelectionScreen(stringResource(R.string.s166), onBack) {
        item { SectionHeader(stringResource(R.string.s341)) }
        item { ThemeSelector(settings.darkMode, setMode) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.color_theme_section), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.color_theme_section_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                ColorThemeGrid(
                    current = settings.colorTheme,
                    customHex = settings.customColor,
                    onPick = { setColorTheme(it.name.lowercase()) },
                    onEditCustom = { showCustomColor = true }
                )
            }
        }
        item { SectionHeader(stringResource(R.string.s_performance)) }
        item { SwitchRow(stringResource(R.string.s184), stringResource(R.string.s343), settings.hardwareAcceleration, setHardwareAcceleration, leading = Icons.Rounded.Speed) }
        item { SectionHeader(stringResource(R.string.s344)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s117)) }, supportingContent = { Text(stringResource(R.string.s345)) }, leadingContent = { Icon(Icons.Default.Download, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onDownloads)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s194)) }, supportingContent = { Text(downloadDirectoryLabel(context, settings)) }, leadingContent = { Icon(Icons.Rounded.Folder, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onDownloadDirectory)) }
        item { SwitchRow(stringResource(R.string.s201), stringResource(R.string.s346), settings.apkHashEnabled, setApkHash, leading = Icons.Rounded.VerifiedUser) }
    }

    if (showCustomColor) {
        CustomColorDialog(
            initialHex = settings.customColor,
            onConfirm = { hex ->
                setCustomColor(hex)
                setColorTheme("custom")
                showCustomColor = false
            },
            onDismiss = { showCustomColor = false }
        )
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

private fun downloadDirectoryLabel(context: Context, settings: BrowserSettings): String = when {
    settings.downloadDirectoryMode == "external" && settings.externalDownloadDisplayPath.isBlank() -> context.getString(R.string.label_ext_dir_unset)
    settings.downloadDirectoryMode == "external" -> settings.externalDownloadDisplayPath
    else -> context.getString(R.string.label_internal_dir)
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
    val context = LocalContext.current
    SelectionScreen(stringResource(R.string.s207), onBack) {
        item { SectionHeader(stringResource(R.string.web_render_section)) }
        item { SwitchRow(stringResource(R.string.s206), stringResource(R.string.s348), settings.forceDarkWeb, setForceDarkWeb, leading = Icons.Rounded.DarkMode) }
        item { SectionHeader(stringResource(R.string.s349)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s213)) }, supportingContent = { Text(searchEngineLabel(context, settings)) }, leadingContent = { Icon(Icons.Rounded.ManageSearch, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onSearchEngine)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s219)) }, supportingContent = { Text(userAgentLabel(context, settings)) }, leadingContent = { Icon(Icons.Default.Computer, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onUserAgent)) }
    }
}

/** 隐私（原 Cookie 设置）：Cookie 追踪 / 清除缓存与 Cookie / 退出时清除浏览器数据 */
@Composable
private fun CookieSettingsScreen(
    settings: BrowserSettings,
    setCookie: (Boolean) -> Unit,
    setClearOnExit: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    clearBrowserData: () -> Unit,
    onHttpsOnly: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showExitOptions by remember { mutableStateOf(false) }
    var exitCookies by remember(settings.clearCookiesOnExit) { mutableStateOf(settings.clearCookiesOnExit) }
    var exitCache by remember(settings.clearCacheOnExit) { mutableStateOf(settings.clearCacheOnExit) }
    var exitHistory by remember(settings.clearHistoryOnExit) { mutableStateOf(settings.clearHistoryOnExit) }
    SelectionScreen(stringResource(R.string.s225), onBack) {
        item { SwitchRow(stringResource(R.string.s224), stringResource(R.string.s350), settings.cookiesEnabled, setCookie, leading = Icons.Rounded.Cookie) }
        item {
            ListItem(
                headlineContent = { Text("HTTPS-Only") },
                supportingContent = { Text(httpsOnlySubtitle(context, settings)) },
                leadingContent = { Icon(Icons.Rounded.Lock, null) },
                trailingContent = { Icon(Icons.Default.ArrowForward, null) },
                modifier = Modifier.clickable(onClick = onHttpsOnly)
            )
        }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s228)) }, supportingContent = { Text(stringResource(R.string.s351)) }, leadingContent = { Icon(Icons.Default.Delete, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable { showClearConfirmation = true }) }
        item { SwitchRow(stringResource(R.string.s232), stringResource(R.string.s352), settings.clearOnExit, { showExitOptions = true }, leading = Icons.Default.Delete) }
    }
    if (showClearConfirmation) ConfirmDialog(stringResource(R.string.s353), stringResource(R.string.s354), {
        clearBrowserData(); showClearConfirmation = false
    }, { showClearConfirmation = false })
    if (showExitOptions) {
        AlertDialog(
            onDismissRequest = { showExitOptions = false },
            title = { Text(stringResource(R.string.s232)) },
            text = {
                Column {
                    CheckRow(stringResource(R.string.s355), exitCookies) { exitCookies = it }
                    CheckRow(stringResource(R.string.s356), exitCache) { exitCache = it }
                    CheckRow(stringResource(R.string.s058), exitHistory) { exitHistory = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    setClearOnExit(exitCookies || exitCache || exitHistory, exitCookies, exitCache, exitHistory)
                    showExitOptions = false
                }) { Text(stringResource(R.string.s127)) }
            },
            dismissButton = { TextButton(onClick = { showExitOptions = false }) { Text(stringResource(R.string.s071)) } }
        )
    }
}

/**
 * HTTPS-Only 设置页：开关 + 生效范围两个单选。
 * 开启后自动尝试以 HTTPS 加密协议连接网站，降低明文传输与降级攻击风险。
 */
@Composable
private fun HttpsOnlyScreen(
    settings: BrowserSettings,
    onToggle: (Boolean) -> Unit,
    onSelectMode: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SelectionScreen("HTTPS-Only", onBack) {
        item {
            SwitchRow(
                title = stringResource(R.string.s357),
                subtitle = httpsOnlySubtitle(context, settings),
                checked = settings.httpsOnlyEnabled,
                onCheckedChange = onToggle,
                leading = Icons.Rounded.Lock
            )
        }
        item { HorizontalDivider() }
        item {
            SecurityModeOption(
                title = stringResource(R.string.s358),
                summary = stringResource(R.string.s359),
                detail = stringResource(R.string.s360),
                selected = settings.httpsOnlyMode == "all",
                enabled = settings.httpsOnlyEnabled,
                onClick = { onSelectMode("all") }
            )
        }
        item {
            SecurityModeOption(
                title = stringResource(R.string.s361),
                summary = stringResource(R.string.s362),
                detail = stringResource(R.string.s363),
                selected = settings.httpsOnlyMode == "private",
                enabled = settings.httpsOnlyEnabled,
                onClick = { onSelectMode("private") }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.s364)) },
                supportingContent = { Text(stringResource(R.string.s365)) },
                leadingContent = { Icon(Icons.Rounded.Policy, null) }
            )
        }
    }
}

/** HTTPS-Only 入口副标题：关闭时显示「已关闭」，开启时提示生效范围 */
private fun httpsOnlySubtitle(context: Context, settings: BrowserSettings): String = when {
    !settings.httpsOnlyEnabled -> context.getString(R.string.https_off)
    settings.httpsOnlyMode == "private" -> context.getString(R.string.https_private)
    else -> context.getString(R.string.https_all)
}

/**
 * 安全防护设置页：选择防护模式，并说明各模式差异。
 */
@Composable
private fun SecuritySettingsScreen(
    settings: BrowserSettings,
    onSelectMode: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.s240)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.s369)) },
                    supportingContent = { Text(stringResource(R.string.s370)) },
                    leadingContent = { Icon(Icons.Rounded.GppGood, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
            item { HorizontalDivider() }
            item { SecurityModeOption(stringResource(R.string.s371), stringResource(R.string.s372), stringResource(R.string.s373), settings.securityMode == "enhanced") { onSelectMode("enhanced") } }
            item { SecurityModeOption(stringResource(R.string.s374), stringResource(R.string.s375), stringResource(R.string.s376), settings.securityMode == "standard") { onSelectMode("standard") } }
            item { SecurityModeOption(stringResource(R.string.s377), stringResource(R.string.s378), stringResource(R.string.s379), settings.securityMode == "basic") { onSelectMode("basic") } }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.s380)) },
                    supportingContent = { Text(stringResource(R.string.s381)) },
                    leadingContent = { Icon(Icons.Rounded.Policy, null) }
                )
            }
        }
    }
}

/** 广告拦截入口副标题 */
private fun adBlockSubtitle(context: Context, settings: BrowserSettings): String =
    context.getString(if (settings.adBlockEnabled) R.string.ad_block_on else R.string.ad_block_off)

/**
 * 广告拦截设置页。
 *
 * 单开关：开启后由 GeckoView 原生追踪保护在请求层拦截广告/追踪域请求，
 * 同时启用内置 zex_adblock 扩展隐藏页面内常见广告位；关闭则完全放行。
 */
@Composable
private fun AdBlockScreen(
    settings: BrowserSettings,
    setAdBlock: (Boolean) -> Unit,
    blockedCount: Long?,
    onBack: () -> Unit
) {
    SelectionScreen(stringResource(R.string.ad_block_title), onBack) {
        item {
            SwitchRow(
                title = stringResource(R.string.ad_block_switch_title),
                subtitle = stringResource(R.string.ad_block_switch_sub),
                checked = settings.adBlockEnabled,
                onCheckedChange = setAdBlock,
                leading = Icons.Rounded.Block
            )
        }
        // 拦截次数紧跟在开关下方：这是用户最关心的「到底有没有用」的直接证据，
        // 放在开关旁可见即得，不用再往下找
        item {
            val count = blockedCount
            val countText = if (count == null) {
                stringResource(R.string.ad_block_count_loading)
            } else {
                stringResource(R.string.ad_block_count_value, count)
            }
            ListItem(
                headlineContent = {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                supportingContent = {
                    Text(
                        if (settings.adBlockEnabled) stringResource(R.string.ad_block_count_hint)
                        else stringResource(R.string.ad_block_count_hint_off)
                    )
                },
                leadingContent = { Icon(Icons.Rounded.VerifiedUser, null) }
            )
        }
        item { HorizontalDivider() }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.s364)) },
                supportingContent = { Text(stringResource(R.string.ad_block_how_it_works)) },
                leadingContent = { Icon(Icons.Rounded.GppGood, null) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.ad_block_tip_title)) },
                supportingContent = { Text(stringResource(R.string.ad_block_tip)) },
                leadingContent = { Icon(Icons.Rounded.Policy, null) }
            )
        }
    }
}

/**
 * 通知设置页。
 *
 * 目前只有一项「实时通知」。它不存在本地开关：
 * 能否升格为流体云 / 灵动岛 / 实时活动完全由系统掌控，
 * 所以这里做的是「实时查询 + 跳转系统设置」，而不是伪造一个开关。
 * 这样用户在系统里改完回来，这里的状态也是真的。
 */
@Composable
private fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsEnabled by remember { mutableStateOf(PromotedNotification.areNotificationsEnabled(context)) }
    var promotedEnabled by remember { mutableStateOf(PromotedNotification.isEnabled(context)) }
    val supported = PromotedNotification.isSupported()

    // 回到本页时重新读取：用户可能刚在系统设置里改过开关
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = PromotedNotification.areNotificationsEnabled(context)
                promotedEnabled = PromotedNotification.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SelectionScreen(stringResource(R.string.notification_title), onBack) {
        item {
            val status = when {
                !supported -> stringResource(R.string.notification_unsupported)
                !notificationsEnabled -> stringResource(R.string.notification_blocked)
                promotedEnabled -> stringResource(R.string.notification_on)
                else -> stringResource(R.string.notification_off)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.notification_live_title)) },
                supportingContent = { Text(status) },
                leadingContent = { Icon(Icons.Rounded.Notifications, null) },
                trailingContent = {
                    if (supported) {
                        // 已开启也保留入口：用户可能想关掉，不替系统做决定
                        TextButton(onClick = { PromotedNotification.openSettings(context) }) {
                            Text(
                                stringResource(
                                    if (promotedEnabled) R.string.notification_manage else R.string.notification_enable
                                )
                            )
                        }
                    }
                }
            )
        }
        if (supported && !notificationsEnabled) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notification_need_permission_title)) },
                    supportingContent = { Text(stringResource(R.string.notification_need_permission_body)) },
                    leadingContent = { Icon(Icons.Rounded.Block, null) }
                )
            }
        }
        item { HorizontalDivider() }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.s364)) },
                supportingContent = { Text(stringResource(R.string.notification_how_it_works)) },
                leadingContent = { Icon(Icons.Rounded.GppGood, null) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.notification_tip_title)) },
                supportingContent = { Text(stringResource(R.string.notification_tip)) },
                leadingContent = { Icon(Icons.Rounded.Policy, null) }
            )
        }
    }
}

/** 一个防护模式选项：整行可点，选中态用勾选图标与主色标出 */
@Composable
private fun SecurityModeOption(
    title: String,
    summary: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        supportingContent = {
            Column {
                Text(summary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (selected) stringResource(R.string.s382) else null,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
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
    val context = LocalContext.current
    SelectionScreen(stringResource(R.string.s250), onBack) {
        item { ListItem(headlineContent = { Text(stringResource(R.string.s249)) }, supportingContent = { Text(clipboardPolicyLabel(context, settings.clipboardReadPolicy)) }, leadingContent = { Icon(Icons.Rounded.ContentPaste, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onClipboard)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s053)) }, supportingContent = { Text(policyLabel(context, settings.externalAppPolicy)) }, leadingContent = { Icon(Icons.Default.OpenInNew, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onExternalApp)) }
        item { ListItem(headlineContent = { Text(stringResource(R.string.s259)) }, supportingContent = { Text(policyLabel(context, settings.micPolicy)) }, leadingContent = { Icon(Icons.Rounded.Mic, null) }, trailingContent = { Icon(Icons.Default.ArrowForward, null) }, modifier = Modifier.clickable(onClick = onMic)) }
        item { Text(stringResource(R.string.s383), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
    }
}

private fun policyLabel(context: Context, policy: String) = when (policy) {
    "allow" -> context.getString(R.string.policy_allow)
    "deny" -> context.getString(R.string.policy_deny)
    else -> context.getString(R.string.policy_ask)
}

private fun clipboardPolicyLabel(context: Context, policy: String) = when (policy) {
    "allow" -> context.getString(R.string.policy_allow)
    "deny" -> context.getString(R.string.policy_deny)
    else -> context.getString(R.string.policy_ask)
}

private fun siteHost(uri: String): String = runCatching { java.net.URI(uri).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri

/**
 * 把用户选中的 content:// URI 复制到应用缓存下的真实文件，返回 file:// URI。
 *
 * 为什么必须这么做：GeckoView 的 `FilePrompt.confirm` 内部 `getFile()` 只接受两种 URI：
 * 1. `file://` 路径；
 * 2. 带 "_data" 列（绝对路径）的 content://。
 * 而 `ActivityResultContracts.OpenDocument()` 返回的是 SAF 文档 URI
 * （如 content://com.android.externalstorage.documents/...），这类 URI 没有 _data 列，
 * `getFile()` 直接返回 null → 网页收到空文件路径，表现为“选了文件但上传不上去、也没有 MIME”。
 *
 * 复制到本地后转 file:// 回传，绕开这个限制；同时保留原名与扩展名，
 * 便于 Gecko 根据扩展名推断 MIME 类型。
 */
private fun copyUriToCacheForUpload(context: Context, uri: Uri): Uri? = runCatching {
    // 1) 读取真实文件名（含扩展名），用于 Gecko 推断 MIME
    var fileName = "upload_${System.currentTimeMillis()}"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { fileName = it }
        }
    }
    // 去掉非法路径字符，避免创建文件失败
    val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "upload" }
    val dir = File(context.cacheDir, "web-uploads").apply { if (!exists()) mkdirs() }
    // 顺手清理一天前的旧上传文件，避免缓存无限增长（网页可能在提交时才读，所以不能立即删）
    val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
    dir.listFiles()?.forEach { if (it.lastModified() < cutoff) runCatching { it.delete() } }
    val target = File(dir, "${System.currentTimeMillis()}_$safeName")
    // 3) 打开输入流写入真实文件
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    if (!target.exists() || target.length() <= 0L) { target.delete(); return null }
    Uri.fromFile(target)
}.getOrNull()

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
    SelectionScreen(stringResource(R.string.s259), onBack) {
        item { ChoiceItem(stringResource(R.string.s338), settings.micPolicy == "ask") { onSelect("ask") } }
        item { ChoiceItem(stringResource(R.string.s047), settings.micPolicy == "allow") { onSelect("allow") } }
        item { ChoiceItem(stringResource(R.string.s056), settings.micPolicy == "deny") { onSelect("deny") } }
        item { Text(stringResource(R.string.s384), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.s385)) },
                supportingContent = { Text(stringResource(if (osGranted) R.string.s386 else R.string.s387)) },
                leadingContent = { Icon(Icons.Rounded.Mic, null) },
                trailingContent = {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { Toast.makeText(context, context.getString(R.string.s388), Toast.LENGTH_SHORT).show() }
                    }) { Text(stringResource(R.string.s389)) }
                }
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text(stringResource(R.string.s340)) } }
    }
}

@Composable
private fun SelectionScreen(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }, actions = actions) }) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), content = content) }
}

@Composable
private fun ChoiceItem(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, leadingContent = { if (selected) Icon(Icons.Default.Check, null) }, modifier = Modifier.clickable(onClick = onClick))
}

/**
 * 语言选择页。
 *
 * 选定后只写入存储，不重建当前界面：语言在下次启动时由 attachBaseContext 读取生效。
 * 这与系统级语言设置的直觉一致，也避免重建时丢失当前浏览状态。
 */
@Composable
private fun LanguageScreen(language: String, setLanguage: (String) -> Unit, onBack: () -> Unit) {
    val options = listOf(
        AppLocale.SYSTEM to stringResource(R.string.settings_language_follow_system),
        AppLocale.ZH_CN to stringResource(R.string.settings_language_zh_cn),
        AppLocale.ZH_TW to stringResource(R.string.settings_language_zh_tw),
        AppLocale.EN to stringResource(R.string.settings_language_en)
    )
    SelectionScreen(stringResource(R.string.s173), onBack) {
        options.forEach { (value, label) ->
            item { ChoiceItem(label, language == value) { setLanguage(value) } }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                stringResource(R.string.s390),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 高级设置页。
 *
 * 目前只提供「调试模式」一个卡片选项。本页只在已连点解锁时才可达，
 * 因此卡片本身就是入口，不再需要额外开关。
 */
@Composable
private fun AdvancedSettingsScreen(onDebug: () -> Unit, onBack: () -> Unit) {
    SelectionScreen(stringResource(R.string.debug_section_advanced), onBack) {
        item {
            Card(
                onClick = onDebug,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.debug_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 调试模式页。
 *
 * 所有条目均基于真实装置与运行环境数据，不做任何模拟：
 * - 设备与内核信息：报告、内核、UA、渲染后端等实际参数；
 * - 运行环境自检：逐项核对开关与内核状态是否能对上；
 * - 安装来源与签名：确认装的是自建包还是被重签的包；
 * - 崩溃日志：读取真实捕获到的上一次崩溃栈。
 */
@Composable
private fun DebugScreen(
    settings: BrowserSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val snapshot = remember(
        settings.userAgentMode, settings.hardwareAcceleration, settings.httpsOnlyEnabled,
        settings.httpsOnlyMode, settings.securityMode, settings.adBlockEnabled, settings.darkMode
    ) {
        DebugSettingsSnapshot(
            userAgentMode = settings.userAgentMode,
            hardwareAcceleration = settings.hardwareAcceleration,
            httpsOnlyEnabled = settings.httpsOnlyEnabled,
            httpsOnlyMode = settings.httpsOnlyMode,
            securityMode = settings.securityMode,
            adBlockEnabled = settings.adBlockEnabled,
            darkMode = settings.darkMode
        )
    }

    // 诊断信息只在进入时读一次：UA 读取会跨 JNI，每次重组都读没有意义
    val items = remember(snapshot) {
        DebugInfo.build(context, snapshot)
    }
    val installer = remember { DebugInfo.installerSummary(context) }
    val signature = remember { DebugInfo.signatureSummary(context) }
    val crashLog = remember { CrashReporter.readLast(context) }

    var showCrashLog by remember { mutableStateOf(false) }

    SelectionScreen(stringResource(R.string.debug_title), onBack) {
        item { DebugSectionHeader(stringResource(R.string.debug_info_title)) }
        items.forEach { item ->
            item {
                ListItem(
                    headlineContent = { Text(item.label, style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = {
                        Text(
                            item.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        item { DebugSectionHeader(stringResource(R.string.debug_signature_title)) }
        item {
            ListItem(
                headlineContent = { Text("安装来源", style = MaterialTheme.typography.bodyMedium) },
                supportingContent = { Text(installer, style = MaterialTheme.typography.bodySmall) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("签名摘要 (SHA-256)", style = MaterialTheme.typography.bodyMedium) },
                supportingContent = { Text(signature, style = MaterialTheme.typography.bodySmall) }
            )
        }

        item { HorizontalDivider() }

        // 崩溃日志：只在真实存在崩溃记录时可展开，不做示例数据
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.debug_crash_title)) },
                supportingContent = {
                    Text(
                        if (crashLog == null) stringResource(R.string.debug_crash_empty) else "点击查看上次崩溃栈",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingContent = { Icon(Icons.Rounded.BugReport, null) },
                trailingContent = {
                    if (crashLog != null) {
                        TextButton(onClick = { showCrashLog = true }) { Text(stringResource(R.string.s079)) }
                    }
                }
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                TextButton(onClick = { CrashReporter.clear(context); showCrashLog = false }) {
                    Text(stringResource(R.string.debug_crash_clear))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val text = items.joinToString("\n") { "${it.label}: ${it.value}" }
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("debug", text))
                    Toast.makeText(context, context.getString(R.string.debug_copied), Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.debug_copy)) }
            }
        }

        item { HorizontalDivider() }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.debug_exit_title)) },
                supportingContent = { Text(stringResource(R.string.debug_sub), style = MaterialTheme.typography.bodySmall) },
                leadingContent = { Icon(Icons.Rounded.BugReport, null) },
                modifier = Modifier.clickable {
                    // 锁定只清内存标志：设置页入口立即消失，重新打开需再连点
                    DebugUnlock.lock()
                    Toast.makeText(context, context.getString(R.string.debug_locked), Toast.LENGTH_SHORT).show()
                    onBack()
                }
            )
        }
    }

    if (showCrashLog && crashLog != null) {
        AlertDialog(
            onDismissRequest = { showCrashLog = false },
            title = { Text(stringResource(R.string.debug_crash_title)) },
            text = {
                Text(
                    crashLog,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { showCrashLog = false }) { Text(stringResource(R.string.s079)) } }
        )
    }
}

/**
 * 网络诊断页。
 *
 * 每项结果都来自真实测量（ConnectivityManager / DNS 解析 / TCP 握手 / HTTP 请求），
 * 页面只负责把结果排得能看懂：先给一个总体健康度，再逐项展开细节。
 */
@Composable
private fun NetworkDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<NetworkDiagnostics.Report?>(null) }

    val start: () -> Unit = {
        if (!running) {
            running = true
            report = null
            scope.launch {
                // runAll 内部已切到 IO 并在失败时抛 CancellationException，
                // 这里只负责把结果回填并复位 running
                report = NetworkDiagnostics.runAll(context)
                running = false
            }
        }
    }

    // 进页自动跑一次：用户点进来就是想知道现在通不通
    LaunchedEffect(Unit) { start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.netdiag_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } },
                actions = {
                    if (!running) {
                        TextButton(onClick = { start() }) { Text(stringResource(R.string.netdiag_rerun)) }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部总览卡片：健康度 + 结论，一眼看完
            item {
                NetworkDiagnosticsSummary(report = report, running = running)
            }

            if (report != null) {
                item {
                    Text(
                        stringResource(R.string.netdiag_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }
                items(report!!.results) { result ->
                    NetworkDiagnosticsRow(result)
                }
                item {
                    Text(
                        stringResource(R.string.netdiag_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp)
                    )
                }
            }
        }
    }
}

/** 总体健康度：根据四项结论算百分比，并按结果换色 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkDiagnosticsSummary(report: NetworkDiagnostics.Report?, running: Boolean) {
    val results = report?.results.orEmpty()
    val total = results.count { it.state != NetworkDiagnostics.CheckState.SKIPPED }
    val passed = results.count { it.state == NetworkDiagnostics.CheckState.OK }
    val failed = results.count { it.state == NetworkDiagnostics.CheckState.FAIL }
    val warned = results.count { it.state == NetworkDiagnostics.CheckState.WARN }
    val score = if (total == 0) 0 else passed * 100 / total

    val (container, onContainer) = when {
        running -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        failed > 0 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        warned > 0 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            if (running) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ContainedLoadingIndicator(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.netdiag_running), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "%",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(
                            when {
                                report == null -> R.string.netdiag_running
                                failed > 0 -> R.string.netdiag_has_fail
                                warned > 0 -> R.string.netdiag_has_warn
                                else -> R.string.netdiag_all_ok
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.netdiag_score),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** 单项结果行：左侧状态图标，右侧标题 + 结论 + 细节 */
@Composable
private fun NetworkDiagnosticsRow(result: NetworkDiagnostics.CheckResult) {
    val (icon, tint) = when (result.state) {
        NetworkDiagnostics.CheckState.OK -> Icons.Rounded.CheckCircle to MaterialTheme.colorScheme.primary
        NetworkDiagnostics.CheckState.WARN -> Icons.Rounded.WarningAmber to MaterialTheme.colorScheme.tertiary
        NetworkDiagnostics.CheckState.FAIL -> Icons.Rounded.GppBad to MaterialTheme.colorScheme.error
        NetworkDiagnostics.CheckState.SKIPPED -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(result.summary, style = MaterialTheme.typography.bodyMedium)
                if (result.detail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        result.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeSelector(mode: String, setMode: (String) -> Unit) {
    val labels = linkedMapOf(
        "system" to stringResource(R.string.theme_follow_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark)
    )
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.s165)) },
        supportingContent = { Text(stringResource(R.string.s391)) },
        leadingContent = { Icon(Icons.Rounded.Palette, null) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(labels[mode] ?: stringResource(R.string.theme_follow_system)) }
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
        headlineContent = { Text(stringResource(R.string.s219)) },
        supportingContent = { Text(stringResource(R.string.s392)) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(labels[mode] ?: stringResource(R.string.theme_follow_system)) }
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
private fun DownloadDialog(url: String, skipResolve: Boolean = false, onDownload: (String, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isApk = url.substringBefore('?').endsWith(".apk", ignoreCase = true)
    var expectedHash by rememberSaveable { mutableStateOf("") }
    // 初始名称取 URL 末段；若它是 download.aspx 这类通用入口，则异步探测真实文件名
    val initialName = url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
    var fileName by remember(url) { mutableStateOf(initialName) }
    // skipResolve：已有本地捕获的响应体时不得再发网络探测——对一次性令牌地址二次请求会 403
    var resolving by remember(url) { mutableStateOf(!skipResolve && DownloadFileName.isGeneric(initialName)) }
    LaunchedEffect(url) {
        if (skipResolve) { resolving = false; return@LaunchedEffect }
        if (!DownloadFileName.isGeneric(initialName)) return@LaunchedEffect
        val resolved = DownloadFileName.resolve(url)
        if (resolved != null) fileName = resolved
        resolving = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s393)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (resolving) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.s394), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                // 文件名可编辑：部分站点（网盘跳转、需登录的下载入口）拿不到真实文件名，
                // 此时用户可手动修正，避免落盘成 download.aspx 这类无意义名字。
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(stringResource(R.string.s395)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { copyDownloadLink(context, url); Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show() }) { Text(stringResource(R.string.s397)) }
                if (isApk) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.s398), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(expectedHash, { expectedHash = it }, label = { Text(stringResource(R.string.s399)) }, supportingText = { Text(stringResource(R.string.s400)) })
                }
            }
        },
        confirmButton = {
            // 解析中禁用：否则会把 download.aspx 作为文件名入队，用户看到的与实际落盘不一致
            TextButton(enabled = !resolving && fileName.isNotBlank(), onClick = { onDownload(fileName, expectedHash) }) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.s069))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
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


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutScreen(
    settings: BrowserSettings,
    setAutoCheckUpdates: (Boolean) -> Unit,
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

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.s282)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.s063)) } }) }) { padding ->
        // 条目较多，在窄屏/横屏/大字体下会超出一屏，必须可滚动，否则底部内容点不到
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("ZexBrowse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            // 连点版本号 7 次解锁调试入口（每次点击间隔需在 1.5 秒内）。
            // 解锁只存在内存：进程结束或重装后自动失效，需重新连点。
            Text(
                stringResource(R.string.s401, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                modifier = Modifier.clickable {
                    val remaining = DebugUnlock.tap()
                    if (remaining == 0 && DebugUnlock.unlocked) {
                        Toast.makeText(context, context.getString(R.string.debug_unlocked), Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Text(stringResource(R.string.s402))
            FilledTonalButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BaiXiaoTao520/ZexBrowse"))) }) {
                Icon(painterResource(R.drawable.ic_github), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.s403))
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
            }) { Text(stringResource(R.string.s285)) }
            SwitchRow(stringResource(R.string.s404), stringResource(R.string.s405), settings.autoCheckUpdates, setAutoCheckUpdates, leading = Icons.Default.SystemUpdate)
            // 赞助与支持：与其它入口保持相同的卡片样式
            SettingsEntry(
                title = stringResource(R.string.s406),
                subtitle = stringResource(R.string.s407),
                icon = Icons.Rounded.VolunteerActivism,
                onClick = onSponsor,
                // 本页其余控件都基于 24dp 内边距，卡片不再额外内缩，保持左边缘对齐
                horizontalPadding = 0.dp
            )
            OutlinedButton(
                onClick = {
                    checking = true
                    scope.launch {
                        val checker = UpdateChecker()
                        runCatching { checker.latest() }
                            .onSuccess { release ->
                                if (release != null && checker.isNewer(release.version)) {
                                    update = release
                                } else {
                                    Toast.makeText(context, context.getString(R.string.s408), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .onFailure { error -> message = context.getString(R.string.update_check_failed, error.message ?: context.getString(R.string.update_retry_later)) }
                        checking = false
                    }
                },
                enabled = !checking
            ) { Text(stringResource(if (checking) R.string.s410 else R.string.s411)) }
        }
    }

    val dismissContributors = { contributorsRequest++; contributors = null; contributorsLoading = false }
    if (contributors != null || contributorsLoading) {
        AlertDialog(
            onDismissRequest = dismissContributors,
            title = { Text(stringResource(R.string.s285)) },
            text = {
                val list = contributors
                if (list == null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        // ContainedLoadingIndicator：形状形变在官方容器背景（圆形色块）内进行，
                        // 不传 polygons 即使用官方默认的 7 种形状序列。
                        ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
                    }
                } else if (list.isEmpty()) {
                    Text(stringResource(R.string.s412))
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
                                Text(stringResource(R.string.s413, contributor.contributions), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } }
                }
            },
            confirmButton = { TextButton(onClick = dismissContributors) { Text(stringResource(R.string.s127)) } }
        )
    }

    update?.let { release -> UpdateDialog(release = release, onDismiss = { update = null }) }
    message?.let { content ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(stringResource(R.string.s287)) },
            text = { Text(content) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.s127)) } }
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
                Text(stringResource(R.string.s406))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.s414))
                Text(stringResource(R.string.s415))
                Text(stringResource(R.string.s416))
                Text(stringResource(R.string.s417), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.s418))
                Text(stringResource(R.string.s419))
                Text(stringResource(R.string.s420))
                Text(stringResource(R.string.s421))
            }
        },
        confirmButton = {
            Button(onClick = { openInBrowser(context, SPONSOR_PAGE_URL); onDismiss() }) {
                Icon(Icons.Rounded.VolunteerActivism, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.s422))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateDialog(release: ReleaseInfo, onDismiss: () -> Unit) {
    // 用户点了「下载」后切换到独立下载窗口；为空表示仍在简介窗口
    var stage by remember { mutableStateOf<ReleaseInfo?>(null) }

    val current = stage
    if (current == null) {
        UpdateIntroDialog(
            release = release,
            onStartDownload = { stage = release },
            onDismiss = onDismiss
        )
    } else {
        UpdateDownloadDialog(release = current, onBack = onDismiss)
    }
}

/**
 * 镜像站高速下载通道卡片（位于发现新版本弹窗内）。
 *
 * 仅当镜像站已同步该版本时才渲染。下载前需在网页完成站点的人机验证（PoW），
 * 因此点击只负责用浏览器打开直链，验证与下载都由站点页面完成。
 */
@Composable
private fun LemwoodChannelCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.s423), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.s424),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** 发现新版本的简介窗口：只负责展示更新说明并提供下载入口 */
@Composable
private fun UpdateIntroDialog(
    release: ReleaseInfo,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveUrl = release.downloadUrl
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, stringResource(R.string.s425), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.s426, release.version))
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
                    Text(release.notes.ifBlank { stringResource(R.string.s427) })
                }
                // 无 GitHub 直链但可走镜像站时，不当作错误提示
                if (effectiveUrl.isBlank() && !release.lemwoodAvailable) {
                    Text(stringResource(R.string.s428), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                // 镜像站高速通道：站点已同步该版本时才渲染，
                // 未配置的公开构建不会出现空壳入口。
                if (LemwoodMirror.isAvailable && release.lemwoodAvailable) {
                    LemwoodChannelCard(
                        onClick = {
                            openMirrorDownload(context, LemwoodMirror.apkDownloadUrl(release.version))
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = effectiveUrl.isNotBlank() || release.lemwoodAvailable,
                onClick = onStartDownload
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.s069))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}

/**
 * 独立下载窗口：下载中只提供取消；失败提供取消与重试；成功提供取消与立即安装。
 *
 * 取消时会停止下载并同步删除已写入的临时文件与同名 APK，避免残留。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateDownloadDialog(release: ReleaseInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileName = "ZexBrowse-${release.version}.apk"
    val effectiveUrl = release.downloadUrl
    val channel = stringResource(R.string.s429)

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
        if (effectiveUrl.isBlank()) {
            connecting = false
            error = context.getString(R.string.s430)
            return@LaunchedEffect
        }
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
                error = throwable.message ?: context.getString(R.string.s431)
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
                Icon(Icons.Default.Download, stringResource(R.string.s432), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (result != null) stringResource(R.string.dl_status_completed) else stringResource(R.string.s433, release.version))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.s434, channel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // 先解包：error 是委托属性，不能直接在 when 分支条件里传参
                val errText = error
                when {
                    result != null -> {
                        Text(stringResource(R.string.s435, result!!.file.name))
                        Text(stringResource(R.string.sha256_value, result!!.sha256), style = MaterialTheme.typography.bodySmall)
                        Text(result!!.file.absolutePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    errText != null -> Text(stringResource(R.string.s436, errText), color = MaterialTheme.colorScheme.error)
                    connecting -> {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), amplitude = 0.5f, wavelength = 24.dp)
                        Text(stringResource(R.string.s076))
                    }
                    else -> {
                        WavyProgressBar(progress = progress / 100f, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.s075, progress))
                    }
                }
            }
        },
        confirmButton = {
            if (result != null) {
                Button(onClick = { openUpdateApk(context, result!!.file); onBack() }) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.s077))
                }
            } else if (error != null) {
                Button(onClick = { attempt += 1 }) { Text(stringResource(R.string.s437)) }
            }
        },
        dismissButton = {
            // 三种状态下的「取消」都停下载并删除文件（含已完成的安装包），不留残留
            TextButton(onClick = { cancelAndClean(); onBack() }) { Text(stringResource(R.string.s071)) }
        }
    )
}
