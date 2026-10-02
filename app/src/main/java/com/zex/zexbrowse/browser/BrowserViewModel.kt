/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
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
    var browserSettings = BrowserSettings()
    private var incognitoContextId: String? = null
    private var lastColorScheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
    private var lastForceDark = false
    private var forceDarkInitialized = false
    val selected get() = _tabs.value.firstOrNull { it.id == _selectedId.value }
    init {
        lastColorScheme = runtime.settings.getPreferredColorScheme()
        newTab()
    }
    fun newTab(incognito: Boolean = false, initialUrl: String? = null): GeckoSession {
        val (session, id) = createSession(incognito)
        _tabs.value = _tabs.value + BrowserTab(id, session, incognito); _selectedId.value = id
        session.open(runtime)
        initialUrl?.let(::load)
        return session
    }

    // GeckoView requires onNewSession to return a session that has NOT been opened yet;
    // GeckoView opens it itself. Opening it here throws AssertionError.
    private fun createNewSessionForUri(uri: String, incognito: Boolean): GeckoSession {
        val (session, id) = createSession(incognito)
        _tabs.value = _tabs.value + BrowserTab(id, session, incognito); _selectedId.value = id
        return session
    }

    private fun createSession(incognito: Boolean): Pair<GeckoSession, String> {
        val sessionSettings = GeckoSessionSettings.Builder().usePrivateMode(incognito)
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
    fun closeAll() { _tabs.value.forEach { tab -> runCatching { tab.session.close() } }; _tabs.value = emptyList(); _selectedId.value = null; incognitoContextId?.let(runtime.storageController::clearDataForSessionContext); incognitoContextId = null; newTab() }
    fun closeAllIncognito() {
        _tabs.value.filter { it.incognito }.forEach { tab -> runCatching { tab.session.close() } }
        _tabs.value = _tabs.value.filterNot { it.incognito }
        incognitoContextId?.let(runtime.storageController::clearDataForSessionContext)
        incognitoContextId = null
        _selectedId.value = _tabs.value.lastOrNull()?.id ?: run { newTab(); _selectedId.value }
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
            // 深色模式下始终向网页传入深色偏好（由网页自行适配）；扩展是否注入暗色样式仅由 force 控制
            val scheme = if (dark) GeckoRuntimeSettings.COLOR_SCHEME_DARK else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            runCatching { runtime.settings.setPreferredColorScheme(scheme) }
            val shouldForce = dark && force
            val forceChanged = lastForceDark != shouldForce || !forceDarkInitialized
            val schemeChanged = lastColorScheme != scheme
            lastForceDark = shouldForce
            lastColorScheme = scheme
            forceDarkInitialized = true
            val app = getApplication<Application>() as? ZexBrowseApplication
            if (forceChanged && app != null) {
                // 关闭强制适配时，跟随禁用暗色扩展；重新开启时再启用
                app.setDarkExtensionEnabled(shouldForce) { reloadAllTabs() }
            } else if (schemeChanged) {
                reloadAllTabs()
            }
        }
    }

    private fun reloadAllTabs() {
        val run = Runnable {
            _tabs.value.toList().forEach { tab -> runCatching { tab.session.reload() } }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) run.run() else Handler(Looper.getMainLooper()).post(run)
    }

    fun applyCurrentUserAgentToSelected() {
        val ua = userAgentOverride(browserSettings)
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
    private fun update(id: String, transform: (BrowserTab) -> BrowserTab) { _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it } }
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
