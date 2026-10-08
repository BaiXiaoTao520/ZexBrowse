/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zex.zexbrowse.ZexBrowseApplication
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.HistoryEntity
import com.zex.zexbrowse.download.DownloadScheduler
import com.zex.zexbrowse.data.BrowserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse
import org.mozilla.geckoview.StorageController
import java.net.URLEncoder
import java.util.UUID

data class BrowserTab(val id: String = UUID.randomUUID().toString(), val session: GeckoSession, val incognito: Boolean, val title: String = "新标签页", val url: String = "", val progress: Int = 0, val loading: Boolean = false, val failed: Boolean = false, val preview: Bitmap? = null, val canGoBack: Boolean = false, val canGoForward: Boolean = false)
class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = (application as ZexBrowseApplication).runtime
    private val database = BrowserDatabase.create(application)
    private val downloadScheduler = DownloadScheduler(application)
    val downloads = database.downloadDao().observeAll()
    val history = database.dao().history()
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList()); val tabs = _tabs.asStateFlow()
    private val _selectedId = MutableStateFlow<String?>(null); val selectedId = _selectedId.asStateFlow()
    var onExternalDownload: (String) -> Unit = {}
    // 界面层注入：网页请求麦克风时据此申请系统权限并回传结果
    var mediaPermissionHandler: ((Array<out GeckoSession.PermissionDelegate.MediaSource>, GeckoSession.PermissionDelegate.MediaCallback) -> Unit)? = null
    var browserSettings = BrowserSettings()
    private var incognitoContextId: String? = null
    private var lastColorScheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
    private var lastUserAgent: String? = null
    // 待加载地址：因外部链接新建的标签，需等会话挂载到 GeckoView 后再 loadUri，
    // 否则在 open() 后立即 loadUri 会因会话尚未附加而被丢弃（表现为空白新标签）
    private val pendingInitialLoads = HashMap<String, String>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    val selected get() = _tabs.value.firstOrNull { it.id == _selectedId.value }
    init {
        lastColorScheme = runtime.settings.getPreferredColorScheme()
        newTab()
    }
    fun newTab(incognito: Boolean = false, initialUrl: String? = null): GeckoSession {
        val (session, id) = createSession(incognito)
        _tabs.value = _tabs.value + BrowserTab(id, session, incognito); _selectedId.value = id
        session.open(runtime)
        if (initialUrl != null) {
            val target = normalize(initialUrl)
            pendingInitialLoads[id] = target
            // 兑底：若因故未收到挂载回调，稍后仍尝试加载一次
            mainHandler.postDelayed({ loadPending(id) }, 1200)
        }
        return session
    }

    // GeckoView 视图挂载完会话后调用；此时 loadUri 才能可靠生效
    fun onSessionAttached(id: String) { loadPending(id) }

    private fun loadPending(id: String) {
        val target = pendingInitialLoads.remove(id) ?: return
        _tabs.value.firstOrNull { it.id == id }?.session?.let { session -> runCatching { session.loadUri(target) } }
    }

    // GeckoView requires onNewSession to return a session that has NOT been opened yet;
    // GeckoView opens it itself. Opening it here throws AssertionError.
    private fun createNewSessionForUri(uri: String, incognito: Boolean): GeckoSession {
        val (session, id) = createSession(incognito)
        _tabs.value = _tabs.value + BrowserTab(id, session, incognito); _selectedId.value = id
        return session
    }

    private fun createSession(incognito: Boolean): Pair<GeckoSession, String> {
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(incognito)
            // 会话转为非活动（切换标签/离开浏览页）时也保持媒体播放，避免音频被打断
            .suspendMediaWhenInactive(false)
        if (incognito) {
            val contextId = incognitoContextId ?: UUID.randomUUID().toString().also { incognitoContextId = it }
            sessionSettings.contextId(contextId)
        }
        userAgentOverride(browserSettings)?.let(sessionSettings::userAgentOverride)
        val session = GeckoSession(sessionSettings.build())
        val id = UUID.randomUUID().toString()
        session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) = update(id) { it.copy(url = url, loading = true, failed = false, progress = 0) }
            override fun onPageStop(session: GeckoSession, success: Boolean) { update(id) { it.copy(loading = false, failed = !success, progress = if (success) 100 else it.progress) }; if (success && !incognito) saveHistory(id) }
            override fun onProgressChange(session: GeckoSession, progress: Int) = update(id) { it.copy(progress = progress) }
        })
        session.setContentDelegate(object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) { update(id) { it.copy(title = title?.takeIf(String::isNotBlank) ?: it.title) } }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) { onExternalDownload(response.uri) }
        })
        session.setPermissionDelegate(object : GeckoSession.PermissionDelegate {
            // 网页发起麦克风/摄像头等媒体请求时触发，转发给界面层去申请系统运行时权限
            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                val handler = mediaPermissionHandler
                if (handler != null && audio != null && audio.isNotEmpty()) handler(audio, callback) else callback.reject()
            }
        })
        session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, permissions: List<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) { if (url != null) update(id) { it.copy(url = url) } }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                if (uri.isDownloadUrl()) {
                    onExternalDownload(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }
            override fun onSubframeLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                if (uri.isDownloadUrl()) {
                    onExternalDownload(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) = update(id) { it.copy(canGoBack = canGoBack) }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) = update(id) { it.copy(canGoForward = canGoForward) }
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (uri.isDownloadUrl()) {
                    onExternalDownload(uri)
                    return null
                }
                return GeckoResult.fromValue(createNewSessionForUri(uri, incognito))
            }
        })
        return session to id
    }

    fun select(id: String) { _selectedId.value = id }
    fun close(id: String) {
        runCatching { _tabs.value.firstOrNull { it.id == id }?.session?.close() }
        _tabs.value = _tabs.value.filterNot { it.id == id }
        clearIncognitoContextIfEmpty()
        _selectedId.value = _tabs.value.lastOrNull()?.id
        if (_tabs.value.isEmpty()) newTab()
    }
    // 仅关闭普通标签，不动无痕标签
    fun closeAllNormal() {
        _tabs.value.filter { !it.incognito }.forEach { tab -> runCatching { tab.session.close() } }
        _tabs.value = _tabs.value.filter { it.incognito }
        if (_tabs.value.isEmpty()) newTab() else _selectedId.value = _tabs.value.last().id
    }
    fun closeAllIncognito() {
        _tabs.value.filter { it.incognito }.forEach { tab -> runCatching { tab.session.close() } }
        _tabs.value = _tabs.value.filterNot { it.incognito }
        incognitoContextId?.let(runtime.storageController::clearDataForSessionContext)
        incognitoContextId = null
        _selectedId.value = _tabs.value.lastOrNull()?.id ?: run { newTab(); _selectedId.value }
    }
    // 退出无痕浏览：彻底关闭全部无痕标签并清除其 Cookie/缓存，普通标签不受影响
    fun exitIncognito() {
        val incognitoTabs = _tabs.value.filter { it.incognito }
        if (incognitoTabs.isEmpty()) return
        incognitoTabs.forEach { tab -> runCatching { tab.session.close() } }
        _tabs.value = _tabs.value.filterNot { it.incognito }
        incognitoContextId?.let(runtime.storageController::clearDataForSessionContext)
        incognitoContextId = null
        if (_tabs.value.isEmpty()) newTab() else _selectedId.value = _tabs.value.last().id
    }
    fun toggleDesktopUserAgent() {
        val desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        val isDesktop = selected?.session?.settings?.userAgentOverride?.contains("X11") == true
        selected?.session?.settings?.let { s ->
            s.setUserAgentOverride(if (isDesktop) userAgentOverride(browserSettings) else desktopUA)
        }
        selected?.session?.reload()
    }

    fun applyForceDark(dark: Boolean, force: Boolean) {
        runCatching {
            val scheme = when {
                !dark -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
                force -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
                else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
            }
            runtime.settings.setPreferredColorScheme(scheme)
            if (lastColorScheme != scheme) {
                lastColorScheme = scheme
                _tabs.value.forEach { tab -> runCatching { tab.session.reload() } }
            }
        }
    }

    fun applyCurrentUserAgentToSelected() {
        val ua = userAgentOverride(browserSettings)
        // 仅当 UA 真正变化时才重新加载，避免无谓刷新打断正在播放的音频
        if (ua == lastUserAgent) return
        lastUserAgent = ua
        selected?.session?.let { session ->
            runCatching { session.settings?.setUserAgentOverride(ua) }
            runCatching { session.reload() }
        }
    }
    fun load(input: String) { val target = normalize(input); selected?.session?.loadUri(target) }
    fun back() { selected?.session?.goBack() }; fun forward() { selected?.session?.goForward() }; fun reload() { selected?.session?.reload() }
    fun enqueueDownload(url: String, fileName: String, expectedHash: String) {
        viewModelScope.launch {
            downloadScheduler.enqueue(
                url, fileName, expectedHash,
                browserSettings.downloadDirectoryMode == "external",
                browserSettings.externalDownloadTreeUri
            )
        }
    }
    fun cancelDownload(id: String) { viewModelScope.launch { downloadScheduler.cancel(id) } }
    fun deleteDownload(id: String, uri: String, deleteFile: Boolean) { viewModelScope.launch { downloadScheduler.delete(id, uri, deleteFile) } }
    fun clearHistory() { applicationScope.launch(Dispatchers.IO) { database.dao().clearHistory() } }
    fun clearBrowserData(cookies: Boolean = true, cache: Boolean = true, history: Boolean = true) {
        var flags = 0L
        if (cookies) flags = flags or StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES
        if (cache) flags = flags or StorageController.ClearFlags.ALL_CACHES
        if (flags != 0L) runCatching { runtime.storageController.clearData(flags) }
        if (history) applicationScope.launch(Dispatchers.IO) { database.dao().clearHistory() }
    }
    // 退出清理需要不随 ViewModel 销毁而取消的协程，否则进程结束时历史可能未清完
    private val applicationScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
    private fun clearIncognitoContextIfEmpty() {
        if (_tabs.value.none { it.incognito }) {
            incognitoContextId?.let(runtime.storageController::clearDataForSessionContext)
            incognitoContextId = null
        }
    }
    private fun update(id: String, transform: (BrowserTab) -> BrowserTab) {
        // 内容未变化时不重新赋值，避免 StateFlow 发出等价列表导致无谓重组
        var changed = false
        val next = _tabs.value.map { tab ->
            if (tab.id == id) {
                val updated = transform(tab)
                if (updated != tab) changed = true
                updated
            } else tab
        }
        if (changed) _tabs.value = next
    }
    private fun String.isDownloadUrl(): Boolean {
        val path = substringBefore('?').substringBefore('#').lowercase()
        return listOf(".apk", ".zip", ".7z", ".rar", ".tar", ".gz", ".bz2", ".xz", ".pdf", ".exe", ".msi", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".csv", ".iso", ".img", ".dmg")
            .any { path.endsWith(it) }
    }
    private fun saveHistory(id: String) { val tab = _tabs.value.firstOrNull { it.id == id } ?: return; if (tab.url.startsWith("http")) viewModelScope.launch(Dispatchers.IO) { database.dao().addHistory(HistoryEntity(title = tab.title, url = tab.url)) } }
    private fun userAgentOverride(settings: BrowserSettings): String? {
        if (settings.simplifiedUserAgent) return "Mozilla/5.0 (Android) Gecko/131 Firefox/131"
        val userAgent = when (settings.userAgentMode) {
            "android_chrome" -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            "desktop_chrome" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            "desktop_firefox" -> "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"
            "custom" -> settings.customUserAgent.trim().takeIf(String::isNotEmpty)
            else -> null
        }
        return userAgent
    }
    private fun normalize(value: String): String {
        val text = value.trim()
        if (text.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return text
        if (text.startsWith("www.") || text.matches(Regex("^[^\\s/]+\\.[^\\s/]+.*"))) return "https://$text"
        val encoded = URLEncoder.encode(text, "UTF-8")
        val template = when (browserSettings.searchEngine) {
            "baidu" -> "https://www.baidu.com/s?wd={query}"
            "bing" -> "https://www.bing.com/search?q={query}"
            "duckduckgo" -> "https://duckduckgo.com/?q={query}"
            "custom" -> browserSettings.customSearchUrl
            else -> "https://www.google.com/search?q={query}"
        }
        return (template.takeIf { it.contains("{query}") } ?: "https://www.google.com/search?q={query}").replace("{query}", encoded)
    }
    override fun onCleared() { _tabs.value.forEach { it.session.close() }; super.onCleared() }
}
