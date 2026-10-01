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
import com.zex.zexbrowse.data.BrowserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse
import java.net.URLEncoder
import java.util.UUID

data class BrowserTab(val id: String = UUID.randomUUID().toString(), val session: GeckoSession, val incognito: Boolean, val title: String = "新标签页", val url: String = "", val progress: Int = 0, val loading: Boolean = false, val failed: Boolean = false, val preview: Bitmap? = null, val canGoBack: Boolean = false, val canGoForward: Boolean = false)
class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = (application as ZexBrowseApplication).runtime
    private val database = BrowserDatabase.create(application)
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList()); val tabs = _tabs.asStateFlow()
    private val _selectedId = MutableStateFlow<String?>(null); val selectedId = _selectedId.asStateFlow()
    var onExternalDownload: (String) -> Unit = {}
    var browserSettings = BrowserSettings()
    val selected get() = _tabs.value.firstOrNull { it.id == _selectedId.value }
    init { newTab() }
    fun newTab(incognito: Boolean = false, initialUrl: String? = null) {
        val sessionSettings = GeckoSessionSettings.Builder().usePrivateMode(incognito)
        userAgentOverride(browserSettings)?.let(sessionSettings::userAgentOverride)
        val session = GeckoSession(sessionSettings.build()); val id = UUID.randomUUID().toString()
        session.open(runtime)
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
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) = update(id) { it.copy(canGoBack = canGoBack) }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) = update(id) { it.copy(canGoForward = canGoForward) }
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? = null
        })
        _tabs.value = _tabs.value + BrowserTab(id, session, incognito); _selectedId.value = id; initialUrl?.let(::load)
    }
    fun select(id: String) { _selectedId.value = id }
    fun close(id: String) { _tabs.value.firstOrNull { it.id == id }?.session?.close(); _tabs.value = _tabs.value.filterNot { it.id == id }; _selectedId.value = _tabs.value.lastOrNull()?.id; if (_tabs.value.isEmpty()) newTab() }
    fun closeAll() { _tabs.value.forEach { it.session.close() }; _tabs.value = emptyList(); _selectedId.value = null; newTab() }
    fun load(input: String) { val target = normalize(input); selected?.session?.loadUri(target) }
    fun back() { selected?.session?.goBack() }; fun forward() { selected?.session?.goForward() }; fun reload() { selected?.session?.reload() }
    private fun update(id: String, transform: (BrowserTab) -> BrowserTab) { _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it } }
    private fun saveHistory(id: String) { val tab = _tabs.value.firstOrNull { it.id == id } ?: return; if (tab.url.startsWith("http")) viewModelScope.launch(Dispatchers.IO) { database.dao().addHistory(HistoryEntity(title = tab.title, url = tab.url)) } }
    private fun userAgentOverride(settings: BrowserSettings): String? = when (settings.userAgentMode) {
        "android_chrome" -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        "desktop_chrome" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        "desktop_firefox" -> "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"
        "custom" -> settings.customUserAgent.trim().takeIf(String::isNotEmpty)
        else -> null
    }
    private fun normalize(value: String): String { val text = value.trim(); return if (text.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) text else if (text.startsWith("www.") || text.matches(Regex("^[^\\s/]+\\.[^\\s/]+.*"))) "https://$text" else "https://www.google.com/search?q=${URLEncoder.encode(text, "UTF-8")}" }
    override fun onCleared() { _tabs.value.forEach { it.session.close() }; super.onCleared() }
}
