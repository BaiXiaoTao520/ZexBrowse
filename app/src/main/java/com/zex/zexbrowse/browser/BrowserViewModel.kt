/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.browser

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zex.zexbrowse.ZexBrowseApplication
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.HistoryEntity
import com.zex.zexbrowse.download.DownloadScheduler
import com.zex.zexbrowse.data.BrowserSettings
import com.zex.zexbrowse.media.MediaPlaybackController
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
    private fun forceDarkApplication() = getApplication<Application>() as ZexBrowseApplication
    // 网页媒体播放期间的前台服务与音频焦点管理（避免后台/锁屏时音频断续）
    private val mediaPlayback = MediaPlaybackController(getApplication())
    private val database = BrowserDatabase.create(application)
    private val downloadScheduler = DownloadScheduler(application)
    val downloads = database.downloadDao().observeAll()
    val history = database.dao().history()
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList()); val tabs = _tabs.asStateFlow()
    private val _selectedId = MutableStateFlow<String?>(null); val selectedId = _selectedId.asStateFlow()
    // 界面层注入：网页/外链触发下载时回调（用于弹出下载确认框）。
    // 冷启动直接打开直链时，该回调可能在界面就绪前就被触发，此时先缓存，待界面注入后补发，避免下载被丢弃。
    private var externalDownloadHandler: ((String) -> Unit)? = null
    private val bufferedExternalDownloads = ArrayDeque<String>()
    var onExternalDownload: (String) -> Unit
        get() = externalDownloadHandler ?: {}
        set(value) {
            externalDownloadHandler = value
            while (bufferedExternalDownloads.isNotEmpty()) value(bufferedExternalDownloads.removeFirst())
        }
    private fun notifyExternalDownload(url: String) {
        val handler = externalDownloadHandler
        if (handler != null) handler(url) else bufferedExternalDownloads.addLast(url)
    }
    // 界面层注入：网页请求选择文件（<input type="file"> 等）时回调。
    // 参数：mimeTypes（可为空）、是否允许多选、结果回调（用户选中的文件 URI，取消为 null）。
    var filePickerHandler: ((Array<out String>?, Boolean, (Array<Uri>?) -> Unit) -> Unit)? = null
    // 界面层注入：网页请求打开外部应用（非 http(s) 的自定义 scheme 等）时回调，由界面弹窗询问用户是否允许跳转。
    // 是否询问由用户在「网页权限管理 → 跳转至外部 APP 权限」中的策略决定。
    var externalAppHandler: ((String) -> Unit)? = null
    // 界面层注入：网站请求读取剪贴板时回调。参数：发起请求的站点、结果回调（true=允许，false=拒绝）。
    // 是否先询问用户由界面层根据用户的剪贴板读取策略决定，本层只负责转发与返回结果。
    var clipboardReadHandler: ((String, (Boolean) -> Unit) -> Unit)? = null
    // 界面层注入：网页请求麦克风等媒体权限时回调。参数：发起请求的站点、可用的音频设备、结果回调。
    // 是否询问用户由界面层根据用户的麦克风策略决定，本层只负责转发与返回结果。
    var mediaPermissionHandler: ((String, Array<out GeckoSession.PermissionDelegate.MediaSource>, GeckoSession.PermissionDelegate.MediaCallback) -> Unit)? = null
    var browserSettings = BrowserSettings()
    private var incognitoContextId: String? = null
    private var lastColorScheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
    // 记录强制暗化扩展当前是否启用，用于在开关变化时触发页面重载使样式生效/失效
    private var lastForceDark: Boolean? = null
    // 记录已应用到会话的 UA，避免重复设置导致无谓刷新；
    // 初始化为 null 表示“尚未应用任何 UA”，首次应用时按需设置
    private var lastUserAgent: String? = null
    // 待加载地址：因外部链接新建的标签，需等会话挂载到 GeckoView 后再 loadUri，
    // 否则在 open() 后立即 loadUri 会因会话尚未附加而被丢弃（表现为空白新标签）
    private val pendingInitialLoads = HashMap<String, String>()
    private val pendingLoadAttempts = HashMap<String, Int>()
    // 记录「地址是下载直链、未能加载成页面」的标签对应的下载网址。
    // 这类标签会话停在 about:blank，刷新无意义；改为用工具栏刷新按钮手动重新触发下载弹窗。
    private val pendingDownloads = HashMap<String, String>()
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
            pendingInitialLoads[id] = normalize(initialUrl)
            android.util.Log.d("ZexIntent", "newTab initial url=${pendingInitialLoads[id]} id=$id")
            // 首次尝试由挂载回调触发；这里再排一次，防止回调缺失
            mainHandler.postDelayed({ onSessionAttached(id) }, 0)
        }
        return session
    }

    /**
     * 外部链接（其它应用打开/分享）优先复用当前标签，避免首启时出现两个标签页。
     *
     * 仅当当前标签还停在空白页（无地址或 about:blank）时才复用；
     * 否则返回 false，由调用方新建标签，避免覆盖用户正在浏览的网页。
     */
    fun loadExternalUrl(url: String): Boolean {
        val tab = selected ?: return false
        val currentUrl = tab.url
        val isBlankTab = currentUrl.isBlank() || currentUrl == "about:blank"
        if (!isBlankTab) return false
        // 已有一个未开始加载的待加载地址时不覆盖（避免连续外链互相覆盖）
        if (pendingInitialLoads.containsKey(tab.id)) return false
        // 复用新标签的首次加载链路：视图可能尚未就绪，交由 pendingInitialLoads 重试确保生效
        pendingInitialLoads[tab.id] = normalize(url)
        mainHandler.postDelayed({ onSessionAttached(tab.id) }, 0)
        return true
    }

    // GeckoView 视图挂载完会话后调用；此时 loadUri 才能可靠生效。
    // 仅在尚未开始尝试时触发，后续交给内部重试链，避免重复触发导致重复加载。
    fun onSessionAttached(id: String) {
        if (pendingInitialLoads.containsKey(id) && (pendingLoadAttempts[id] ?: 0) == 0) loadPending(id)
    }

    // 外部链接的待加载地址需重试：视图刚挂载时 display 可能尚未就绪，
    // loadUri 会被静默丢弃，故在确认页面真正开始加载（onPageStart）前保留待加载项并重试。
    private fun loadPending(id: String) {
        val target = pendingInitialLoads[id] ?: return
        // 直链下载地址不会被加载成页面：交由下载流程处理一次即可。
        // 否则每次重试都会被 onLoadRequest 判定为下载并再次弹窗，导致弹窗反复出现、无法关闭。
        if (target.isDownloadUrl()) {
            // 记录该标签对应的下载直链，供工具栏刷新按钮手动重新触发下载
            pendingDownloads[id] = target
            clearPending(id)
            notifyExternalDownload(target)
            return
        }
        val tab = _tabs.value.firstOrNull { it.id == id }
        // 不能用 tab.url / tab.loading 判断是否已开始加载：会话初挂载会先产生 about:blank，
        // 把 tab.url 填成非空、并可能触发一次 onPageStart，从而提前清除待加载地址，
        // 表现为「打开了浏览器页却是空白标签」。只在真正的 http(s) 页面开始加载时才停止重试。
        if (tab == null) { clearPending(id); return }
        val attempt = (pendingLoadAttempts[id] ?: 0) + 1
        if (attempt > MaxPendingLoadAttempts) { clearPending(id); return }
        pendingLoadAttempts[id] = attempt
        android.util.Log.d("ZexIntent", "loadPending id=$id attempt=$attempt url=$target")
        runCatching { tab.session.loadUri(target) }
        mainHandler.postDelayed({ loadPending(id) }, PendingLoadRetryMillis)
    }

    private fun clearPending(id: String) {
        pendingInitialLoads.remove(id)
        pendingLoadAttempts.remove(id)
    }

    /**
     * 判断两个地址是否指向同一份下载：忽略查询串与锚点的差异。
     * 刷新下载直链时 URL 可能与首次记录时略有出入（如追加的跟踪参数），
     * 按前缀比较足以避免误删记录。
     */
    private fun isSameDownloadTarget(current: String, pending: String): Boolean {
        if (current == pending) return true
        val a = current.substringBefore('?').substringBefore('#')
        val b = pending.substringBefore('?').substringBefore('#')
        return a == b
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
        // 媒体播放委托：播放时启动前台服务 + 申请音频焦点，停止时释放（修复后台/锁屏音频断续）
        mediaPlayback.attach(session)
        session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                // 只有真正开始加载「别的页面」时，才丢弃下载直链记录；
                // 若开始加载的正是这条下载直链本身（或刷新它），必须保留，
                // 否则用户取消一次下载弹窗后，刷新按钮就再也无法重新弹出窗口。
                if (url.startsWith("http")) {
                    clearPending(id)
                    val pendingDownload = pendingDownloads[id]
                    if (pendingDownload == null || !isSameDownloadTarget(url, pendingDownload)) {
                        pendingDownloads.remove(id)
                    }
                }
                android.util.Log.d("ZexIntent", "onPageStart id=$id url=$url")
                update(id) { it.copy(url = url, loading = true, failed = false, progress = 0) }
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) { update(id) { it.copy(loading = false, failed = !success, progress = if (success) 100 else it.progress) }; if (success && !incognito) saveHistory(id) }
            override fun onProgressChange(session: GeckoSession, progress: Int) = update(id) { it.copy(progress = progress) }
        })
        session.setContentDelegate(object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) { update(id) { it.copy(title = title?.takeIf(String::isNotBlank) ?: it.title) } }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) { pendingDownloads[id] = response.uri; clearPending(id); notifyExternalDownload(response.uri) }
        })
        session.setPromptDelegate(object : GeckoSession.PromptDelegate {
            // 网页触发文件选择（如上传图片/视频）时，交由界面层拉起系统文件选择器，选择完成后回传给网页。
            override fun onFilePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val handler = filePickerHandler
                if (handler == null) {
                    result.complete(prompt.dismiss())
                    return result
                }
                handler(prompt.mimeTypes, prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) { uris ->
                    if (uris == null || uris.isEmpty()) result.complete(prompt.dismiss())
                    else result.complete(prompt.confirm(getApplication<Application>(), uris))
                }
                return result
            }
        })
        session.setSelectionActionDelegate(object : GeckoSession.SelectionActionDelegate {
            // 网站请求读取剪贴板：交由界面层弹出询问框（按站点永久记住允许/拒绝），再回传结果。
            override fun onShowClipboardPermissionRequest(
                session: GeckoSession,
                permission: GeckoSession.SelectionActionDelegate.ClipboardPermission
            ): GeckoResult<AllowOrDeny> {
                val result = GeckoResult<AllowOrDeny>()
                val handler = clipboardReadHandler
                if (handler == null) {
                    result.complete(AllowOrDeny.DENY)
                    return result
                }
                handler(permission.uri) { allow ->
                    result.complete(if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
                }
                return result
            }
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
                // 策略由界面层处理（始终拒绝直接回绝、始终允许直接放行、每次询问先弹窗）
                val handler = mediaPermissionHandler
                if (handler != null && audio != null && audio.isNotEmpty()) handler(uri, audio, callback) else callback.reject()
            }
        })
        session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, permissions: List<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) { if (url != null) { android.util.Log.d("ZexIntent", "onLocationChange id=$id url=$url"); update(id) { it.copy(url = url) } } }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                if (uri.isDownloadUrl()) {
                    pendingDownloads[id] = uri
                    clearPending(id)
                    notifyExternalDownload(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                if (uri.isExternalAppUri()) { requestExternalApp(uri); return GeckoResult.fromValue(AllowOrDeny.DENY) }
                return null
            }
            override fun onSubframeLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                if (uri.isDownloadUrl()) {
                    clearPending(id)
                    notifyExternalDownload(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) = update(id) { it.copy(canGoBack = canGoBack) }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) = update(id) { it.copy(canGoForward = canGoForward) }
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (uri.isDownloadUrl()) {
                    notifyExternalDownload(uri)
                    return null
                }
                if (uri.isExternalAppUri()) { requestExternalApp(uri); return null }
                return GeckoResult.fromValue(createNewSessionForUri(uri, incognito))
            }
        })
        return session to id
    }

    fun select(id: String) { _selectedId.value = id }
    fun close(id: String) {
        clearPending(id)
        pendingDownloads.remove(id)
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
            // 应用深色主题时向网页传入深色偏好（未适配的网页由内置扩展强制暗化）；
            // 浅色主题时传入浅色偏好，避免浅色应用里出现深色网页。
            val scheme = if (dark) GeckoRuntimeSettings.COLOR_SCHEME_DARK else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            // 强制暗化仅在「应用深色主题 + 开启强制适配」时生效；
            // 关闭开关后必须真正停用内置扩展，否则网页仍会被强制暗化（历史遗留问题）。
            val forceDarkEnabled = dark && force
            val schemeChanged = lastColorScheme != scheme
            val forceDarkChanged = lastForceDark != forceDarkEnabled
            runtime.settings.setPreferredColorScheme(scheme)
            forceDarkApplication().setForceDarkExtensionEnabled(forceDarkEnabled)
            if (schemeChanged || forceDarkChanged) {
                lastColorScheme = scheme
                lastForceDark = forceDarkEnabled
                // 扩展的启用/停用需重新加载页面才能生效
                _tabs.value.forEach { tab -> runCatching { tab.session.reload() } }
            }
        }
    }

    fun applyCurrentUserAgentToSelected() {
        val ua = userAgentOverride(browserSettings)
        // 仅当 UA 真正变化时才重新加载，避免无谓刷新打断正在播放的音频
        if (ua == lastUserAgent) return
        lastUserAgent = ua
        selected?.let { tab ->
            runCatching { tab.session.settings?.setUserAgentOverride(ua) }
            // 该标签正等待外部链接首次加载（会话尚未/刚挂载）时不能 reload：
            // 此时会话内容仍为 about:blank，reload 会打断 pendingInitialLoads 的加载重试，
            // 表现为自定义 UA 下从外部应用打开链接只停在空白页。
            if (!pendingInitialLoads.containsKey(tab.id)) runCatching { tab.session.reload() }
        }
    }
    fun load(input: String) { val target = normalize(input); selected?.session?.loadUri(target) }
    fun back() { selected?.session?.goBack() }; fun forward() { selected?.session?.goForward() }
    fun reload() {
        val tab = selected ?: return
        val pending = pendingDownloads[tab.id]
        // 标签地址是下载直链（未被加载成页面、会被 onLoadRequest 拒绝）时，刷新按钮改为手动重新触发下载弹窗：
        // 既避免自动重试导致「点取消后反复弹出」，也避免一次性弹框被取消后「再也弹不出来」。
        if (pending != null) notifyExternalDownload(pending) else tab.session.reload()
    }
    /** 立即停止当前标签的加载（用于加载中的工具栏按钮，避免用户卡在加载状态） */
    fun stopLoading() { selected?.session?.stop() }
    /**
     * 加入下载队列（与网页下载、外链下载同一套后台下载体系）。
     * 下载记录 id 通过 [onEnqueued] 回调返回，便于调用方跟踪进度（不阻塞界面线程）。
     */
    fun enqueueDownload(url: String, fileName: String, expectedHash: String, onEnqueued: (String) -> Unit = {}) {
        selected?.let { tab -> if (pendingDownloads[tab.id] == url) pendingDownloads.remove(tab.id) }
        viewModelScope.launch {
            val id = runCatching {
                downloadScheduler.enqueue(
                    url, fileName, expectedHash,
                    browserSettings.downloadDirectoryMode == "external",
                    browserSettings.externalDownloadTreeUri
                )
            }.getOrNull()
            if (id != null) onEnqueued(id)
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
    private companion object {
        const val MaxPendingLoadAttempts = 20
        const val PendingLoadRetryMillis = 400L
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
    // 需交给外部应用打开的链接：非浏览器自身处理的 scheme（http/https 及本地/内部 scheme 之外的自定义 scheme、intent: 等）
    private fun String.isExternalAppUri(): Boolean {
        val scheme = substringBefore("://").substringBefore(':').lowercase()
        if (scheme.isEmpty()) return false
        return scheme !in listOf("http", "https", "file", "about", "data", "blob", "javascript", "resource", "moz-extension", "content", "jar", "wyciwyg", "view-source")
    }
    private fun requestExternalApp(uri: String) {
        // 跳转外部应用仍先由界面层弹窗询问；策略为「始终拒绝」时界面层直接忽略，不再询问。
        // 策略为「始终允许」时界面层不弹窗直接跳转（实现见界面层 externalAppHandler）。
        externalAppHandler?.invoke(uri)
    }
    private fun saveHistory(id: String) { val tab = _tabs.value.firstOrNull { it.id == id } ?: return; if (tab.url.startsWith("http")) viewModelScope.launch(Dispatchers.IO) { database.dao().addHistory(HistoryEntity(title = tab.title, url = tab.url)) } }
    private fun userAgentOverride(settings: BrowserSettings): String? {
        // 简化标识：去掉设备型号、Build 号等隐私信息，但必须保持 Firefox UA 的合法格式。
        // 注意 Gecko/ 后面固定为 20100101（不是版本号），且需带 rv: 标记；
        // 写成 "Gecko/131" 这类非法格式会被必应等站点的反爬规则直接拒绝（请求被 blocked）。
        if (settings.simplifiedUserAgent) return "Mozilla/5.0 (Android 14; Mobile; rv:131.0) Gecko/20100101 Firefox/131.0"
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
    override fun onCleared() {
        // 释放媒体资源（前台服务 / 音频焦点），避免应用退出后残留常驻通知或占着焦点
        mediaPlayback.release()
        _tabs.value.forEach { it.session.close() }
        super.onCleared()
    }
}
