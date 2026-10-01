/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zex.zexbrowse.browser.BrowserViewModel
import com.zex.zexbrowse.data.BrowserSettings
import com.zex.zexbrowse.data.SettingsStore
import com.zex.zexbrowse.data.UpdateChecker
import com.zex.zexbrowse.download.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView

enum class Page { HOME, BROWSER, TABS, SETTINGS, ABOUT }

data class QuickSite(val title: String, val url: String, val icon: String)

private val defaultQuickSites = listOf(
    QuickSite("GitHub", "https://github.com", "⌘"),
    QuickSite("Google", "https://www.google.com", "G"),
    QuickSite("Mozilla", "https://www.mozilla.org", "M"),
    QuickSite("Wikipedia", "https://www.wikipedia.org", "W")
)

@Composable
fun ZexBrowseApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settings.collectAsState(initial = BrowserSettings())
    val scope = rememberCoroutineScope()
    val browserViewModel: BrowserViewModel = viewModel()
    var page by rememberSaveable { mutableStateOf(Page.HOME) }

    browserViewModel.browserSettings = settings

    val useDarkTheme = when (settings.darkMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val colorScheme = if (settings.dynamicColor && android.os.Build.VERSION.SDK_INT >= 31) {
        if (useDarkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else if (useDarkTheme) {
        androidx.compose.material3.darkColorScheme()
    } else {
        androidx.compose.material3.lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface {
            AnimatedContent(targetState = page, label = "page") { destination ->
                when (destination) {
                    Page.HOME -> HomeScreen(
                        onOpen = { address -> browserViewModel.load(address); page = Page.BROWSER },
                        onSettings = { page = Page.SETTINGS }
                    )
                    Page.BROWSER -> BrowserScreen(
                        viewModel = browserViewModel,
                        onTabs = { page = Page.TABS },
                        onSettings = { page = Page.SETTINGS }
                    )
                    Page.TABS -> TabOverview(viewModel = browserViewModel, onBack = { page = Page.BROWSER })
                    Page.SETTINGS -> SettingsScreen(
                        settings = settings,
                        setMode = { scope.launch { settingsStore.mode(it) } },
                        setDynamic = { scope.launch { settingsStore.dynamic(it) } },
                        setCookie = { scope.launch { settingsStore.cookies(it) } },
                        setApkHash = { scope.launch { settingsStore.apkHash(it) } },
                        setUserAgentMode = { scope.launch { settingsStore.userAgentMode(it) } },
                        setCustomUserAgent = { scope.launch { settingsStore.customUserAgent(it) } },
                        onAbout = { page = Page.ABOUT },
                        onBack = { page = Page.HOME }
                    )
                    Page.ABOUT -> AboutScreen(onBack = { page = Page.SETTINGS })
                }
            }
        }
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
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun HomeScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var quickSites by remember { mutableStateOf(defaultQuickSites) }
    var showQuickSiteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ZexBrowse", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "设置") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            AddressInput(value = query, onValueChange = { query = it }, onSubmit = { onOpen(query) })
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("快捷访问", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showQuickSiteDialog = true }) { Text("编辑") }
            }
            QuickSiteGrid(
                sites = quickSites,
                onOpen = onOpen,
                onRestore = { quickSites = defaultQuickSites }
            )
        }
    }

    if (showQuickSiteDialog) {
        QuickSiteDialog(
            onAdd = { site -> quickSites = quickSites + site; showQuickSiteDialog = false },
            onDismiss = { showQuickSiteDialog = false }
        )
    }
}

@Composable
private fun AddressInput(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索或输入网址") },
            singleLine = true,
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, "清空") }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { if (value.isNotBlank()) onSubmit() })
        )
        IconButton(onClick = { if (value.isNotBlank()) onSubmit() }) { Icon(Icons.Default.Search, "打开") }
    }
}

@Composable
private fun QuickSiteGrid(sites: List<QuickSite>, onOpen: (String) -> Unit, onRestore: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sites.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { site ->
                    ElevatedCard(Modifier.weight(1f).height(92.dp).clickable { onOpen(site.url) }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(site.icon, style = MaterialTheme.typography.headlineSmall)
                            Text(site.title)
                        }
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("恢复默认快捷站点") } }
    }
}

@Composable
private fun QuickSiteDialog(onAdd: (QuickSite) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加快捷站点") },
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
private fun BrowserScreen(viewModel: BrowserViewModel, onTabs: () -> Unit, onSettings: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedId by viewModel.selectedId.collectAsState()
    val selectedTab = tabs.firstOrNull { it.id == selectedId }
    var input by remember(selectedTab?.id) { mutableStateOf(selectedTab?.url.orEmpty()) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var expectedHash by remember { mutableStateOf("") }
    var hashResult by remember { mutableStateOf<String?>(null) }
    var showNewTabDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { target ->
        val url = downloadUrl
        if (target != null && url != null) {
            scope.launch {
                val actual = DownloadManager(context).download(url, target)
                hashResult = if (expectedHash.isBlank()) {
                    "SHA-256：$actual"
                } else if (actual.equals(expectedHash.trim(), ignoreCase = true)) {
                    "SHA-256 匹配"
                } else {
                    "SHA-256 不匹配（文件损坏/篡改）"
                }
            }
        }
    }

    viewModel.onExternalDownload = { downloadUrl = it }

    Scaffold(
        topBar = { AddressInput(value = input, onValueChange = { input = it }, onSubmit = { viewModel.load(input) }) },
        bottomBar = {
            GlassSurface(Modifier.fillMaxWidth().padding(10.dp)) {
                IconButton(onClick = viewModel::back, enabled = selectedTab?.canGoBack == true) { Icon(Icons.Default.ArrowBack, "后退") }
                IconButton(onClick = viewModel::forward, enabled = selectedTab?.canGoForward == true) { Icon(Icons.Default.ArrowForward, "前进") }
                IconButton(onClick = viewModel::reload) { Icon(Icons.Default.Refresh, "刷新") }
                IconButton(onClick = { showNewTabDialog = true }) { Icon(Icons.Default.Add, "新建标签") }
                IconButton(onClick = onTabs) {
                    BadgedBox(badge = { Badge { Text(tabs.size.toString()) } }) { Icon(Icons.Default.Tab, "标签") }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Default.MoreVert, "菜单") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                items(tabs, key = { it.id }) { tab ->
                    AssistChip(
                        onClick = { viewModel.select(tab.id) },
                        label = { Text(tab.title, maxLines = 1) },
                        leadingIcon = { if (tab.id == selectedId) Icon(Icons.Default.Check, null) }
                    )
                }
            }
            if (selectedTab != null) {
                if (selectedTab.loading) LinearProgressIndicator(progress = { selectedTab.progress / 100f }, modifier = Modifier.fillMaxWidth())
                AndroidView(
                    factory = { GeckoView(it) },
                    update = { it.setSession(selectedTab.session) },
                    modifier = Modifier.fillMaxSize()
                )
                if (selectedTab.failed) {
                    Text(
                        "页面加载失败，请检查网络后重试。",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showNewTabDialog) {
        AlertDialog(
            onDismissRequest = { showNewTabDialog = false },
            title = { Text("新建标签") },
            text = { Text("无痕标签不会保存浏览历史、Cookie 或缓存。") },
            confirmButton = { TextButton(onClick = { viewModel.newTab(); showNewTabDialog = false; onTabs() }) { Text("普通标签") } },
            dismissButton = { TextButton(onClick = { viewModel.newTab(incognito = true); showNewTabDialog = false; onTabs() }) { Text("无痕标签") } }
        )
    }
    if (downloadUrl != null) {
        DownloadDialog(
            url = downloadUrl!!,
            onDownload = { fileName, expected -> expectedHash = expected; targetLauncher.launch(fileName); downloadUrl = null },
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
}

@Composable
private fun TabOverview(viewModel: BrowserViewModel, onBack: () -> Unit) {
    val tabs by viewModel.tabs.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部标签") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { TextButton(onClick = viewModel::closeAll) { Text("关闭全部") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tabs, key = { it.id }) { tab ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { viewModel.select(tab.id); onBack() }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Language, null) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
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
private fun SettingsScreen(
    settings: BrowserSettings,
    setMode: (String) -> Unit,
    setDynamic: (Boolean) -> Unit,
    setCookie: (Boolean) -> Unit,
    setApkHash: (Boolean) -> Unit,
    setUserAgentMode: (String) -> Unit,
    setCustomUserAgent: (String) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit
) {
    val userAgentLabels = linkedMapOf(
        "geckoview" to "默认 GeckoView",
        "android_chrome" to "Android Chrome",
        "desktop_chrome" to "桌面 Chrome",
        "desktop_firefox" to "桌面 Firefox",
        "custom" to "自定义"
    )
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { ThemeSelector(settings.darkMode, setMode) }
            item { SwitchRow("动态莫奈取色", "使用系统动态颜色", settings.dynamicColor, setDynamic) }
            item { HorizontalDivider() }
            item { SwitchRow("启用 Cookie", "关闭后新会话不保存 Cookie", settings.cookiesEnabled, setCookie) }
            item { ListItem(headlineContent = { Text("无痕会话") }, supportingContent = { Text("通过浏览页底部新建标签按钮创建；无痕会话不保存历史、Cookie 或缓存。") }) }
            item { ListItem(headlineContent = { Text("清除缓存与 Cookie") }, leadingContent = { Icon(Icons.Default.Delete, null) }, modifier = Modifier.clickable { setCookie(false); setCookie(true) }) }
            item { HorizontalDivider() }
            item { UserAgentSelector(settings.userAgentMode, userAgentLabels, setUserAgentMode) }
            if (settings.userAgentMode == "custom") {
                item {
                    OutlinedTextField(
                        value = settings.customUserAgent,
                        onValueChange = setCustomUserAgent,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text("自定义 User-Agent") },
                        supportingText = { Text("留空时使用 GeckoView 默认标识") }
                    )
                }
            }
            item { SwitchRow("APK SHA-256 校验", "下载 APK 时计算哈希值", settings.apkHashEnabled, setApkHash) }
            item { ListItem(headlineContent = { Text("关于 ZexBrowse") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable(onClick = onAbout)) }
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
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun DownloadDialog(url: String, onDownload: (String, String) -> Unit, onDismiss: () -> Unit) {
    val isApk = url.substringBefore('?').endsWith(".apk", ignoreCase = true)
    var expectedHash by rememberSaveable { mutableStateOf("") }
    val fileName = url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载文件") },
        text = {
            Column {
                Text(fileName)
                if (isApk) {
                    Spacer(Modifier.height(12.dp))
                    Text("APK 下载完成后将自动计算 SHA-256。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(expectedHash, { expectedHash = it }, label = { Text("官方 SHA-256（可选）") }, supportingText = { Text("下载后会显示匹配或不匹配结果") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDownload(fileName, expectedHash) }) { Text("选择保存位置") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<com.zex.zexbrowse.data.ReleaseInfo?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("关于") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("ZexBrowse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("版本 1.0.1（构建号 2）")
            Text("本项目采用 Mozilla Public License 2.0 (MPL-2.0) 开源。GeckoView 及其相关组件遵循 Mozilla 的相应开源许可。Jetpack Compose、Material 3 和 AndroidX 库遵循各自许可证。")
            FilledTonalButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BaiXiaoTao520/ZexBrowse"))) }) {
                Icon(Icons.Default.OpenInNew, null)
                Spacer(Modifier.width(8.dp))
                Text("GitHub 仓库")
            }
            OutlinedButton(
                onClick = {
                    checking = true
                    scope.launch(Dispatchers.IO) {
                        runCatching { UpdateChecker().latest() }
                            .onSuccess { release ->
                                if (release != null && UpdateChecker().isNewer(release.version)) update = release else message = "当前已是最新版本"
                            }
                            .onFailure { message = "更新检查失败，请稍后重试" }
                        checking = false
                    }
                },
                enabled = !checking
            ) { Text(if (checking) "正在检查…" else "检查更新") }
        }
    }

    update?.let { release ->
        AlertDialog(
            onDismissRequest = { update = null },
            title = { Text("发现新版本 ${release.version}") },
            text = { Text(release.notes.ifBlank { "暂无更新说明" }) },
            confirmButton = {
                TextButton(onClick = {
                    if (release.downloadUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)))
                    update = null
                }) { Text("下载") }
            },
            dismissButton = { TextButton(onClick = { update = null }) { Text("稍后") } }
        )
    }
    message?.let { content ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("更新") },
            text = { Text(content) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }
        )
    }
}
