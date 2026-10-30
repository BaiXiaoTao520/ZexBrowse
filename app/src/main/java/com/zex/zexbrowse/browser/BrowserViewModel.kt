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
import com.zex.zexbrowse.BuildConfig
import com.zex.zexbrowse.CrashReporter
import com.zex.zexbrowse.R
import com.zex.zexbrowse.ZexBrowseApplication
import com.zex.zexbrowse.data.BrowserDatabase
import com.zex.zexbrowse.data.CUSTOM_SEARCH_PREFIX
import com.zex.zexbrowse.data.HistoryEntity
import com.zex.zexbrowse.download.DownloadFileName
import com.zex.zexbrowse.download.DownloadScheduler
import com.zex.zexbrowse.data.BrowserSettings
import com.zex.zexbrowse.data.LemwoodMirror
import com.zex.zexbrowse.data.SecurityScanner
import com.zex.zexbrowse.data.resolveCustomSearchTemplate
import com.zex.zexbrowse.media.MediaPlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import org.mozilla.geckoview.StorageController
import java.net.URLEncoder
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BrowserTab(val id: String = UUID.randomUUID().toString(), val session: GeckoSession, val incognito: Boolean, val title: String = "", val url: String = "", val progress: Int = 0, val loading: Boolean = false, val failed: Boolean = false, val failedUrl: String = "", val errorCode: Int = 0, val errorCategory: Int = 0, val preview: Bitmap? = null, val canGoBack: Boolean = false, val canGoForward: Boolean = false, val risk: SecurityScanner.RiskAssessment = SecurityScanner.RiskAssessment())
// 下载响应体临时捕获结果：bin 为数据文件，done/fail 为完成标记
class CapturedBody(val bin: File, val done: File, val fail: File)
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
    // 界面层注入：命中风险站点时的拦截回调。参数：目标地址、扫描结论、继续访问回调。
    // 为 null 表示尚未接入界面，不做拦截（仅显示图标）。
    var riskySiteHandler: ((url: String, assessment: SecurityScanner.RiskAssessment, proceed: () -> Unit) -> Unit)? = null
    // 用户选「仍然访问」后的放行名单：避免同一个地址被反复拦回
    private val allowedOnce = HashSet<String>()

    fun clearSecurityAllowance() { allowedOnce.clear() }

    /** 当前安全防护模式（由界面层同步） */
    var securityMode: String = "standard"

    /**
     * 根据防护模式判断是否拦截。
     *
     * - enhanced：可疑及以上均拦，并提示未加密连接
     * - standard：只拦危险级（默认，避免干扰正常浏览）
     * - basic：不拦，只显示护盾图标
     */
    private fun shouldBlockRisky(uri: String): Boolean {
        if (uri in allowedOnce) return false
        if (securityMode == "basic") return false
        val level = SecurityScanner.assessUrl(uri).level
        return when (securityMode) {
            "enhanced" -> level == SecurityScanner.RiskLevel.SUSPICIOUS || level == SecurityScanner.RiskLevel.DANGEROUS
            else -> level == SecurityScanner.RiskLevel.DANGEROUS
        }
    }

    private fun notifyExternalDownload(url: String) {
        val handler = externalDownloadHandler
        if (handler != null) handler(url) else bufferedExternalDownloads.addLast(url)
    }
    /**
     * 将 onExternalResponse 交下来的响应体读完存到临时文件。
     *
     * body 流有读超时（默认 30 秒），且不能跨回调长时间留着，所以必须立刻在后台读完。
     * 读完写 done 标记，读失败写 fail 标记；Worker 据此选择「本地文件落盘」还是「回退网络」。
     */
    private fun captureDownloadBody(url: String, response: WebResponse) {
        val body = response.body ?: return
        val dir = File(getApplication<Application>().cacheDir, "download-capture")
        val id = UUID.randomUUID().toString()
        val bin = File(dir, "$id.bin")
        val done = File(dir, "$id.done")
        val fail = File(dir, "$id.fail")
        // 先同步登记文件句柄，确保紧接着的 enqueueDownload 能取到（不依赖后台是否已执行）
        capturedBodies[url] = CapturedBody(bin, done, fail)
        applicationScope.launch(Dispatchers.IO) {
            try {
                dir.mkdirs()
                // 顺手清掉一天前的残留（用户取消下载时可能留下临时文件）
                val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                dir.listFiles()?.forEach { if (it.lastModified() < cutoff) runCatching { it.delete() } }
                body.use { input -> bin.outputStream().use { out -> input.copyTo(out, DEFAULT_BUFFER_SIZE) } }
                done.writeText(bin.length().toString())
            } catch (t: Throwable) {
                runCatching { bin.delete() }
                runCatching { fail.writeText(t.message ?: "capture failed") }
            }
        }
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
    // 不随 ViewModel 销毁而取消的协程：退出清理、写历史等必须在进程结束前跑完。
    private val applicationScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
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
    // 已把会话挂到 GeckoView 上的标签 id。
    // 未挂载前 loadUri 会被静默丢弃，load() 据此决定「立即加载」还是「先入队等挂载」。
    private val attachedSessions = HashSet<String>()
    // 记录「地址是下载直链、未能加载成页面」的标签对应的下载网址。
    // 这类标签会话停在 about:blank，刷新无意义；改为用工具栏刷新按钮手动重新触发下载弹窗。
    private val pendingDownloads = HashMap<String, String>()
    // 待确认的加载失败：Gecko 的 onPageStop/onLoadError 与 onExternalResponse（下载信号）
    // 回调时序不确定，下载可能先触发失败回调。这里先延迟一小段时间再确认失败，
    // 若期间收到下载信号则取消，避免把「下载」误判成「页面加载失败」而弹出错误页。
    private val pendingFailures = HashMap<String, Runnable>()
    // 已主动拒绝的外部应用导航：对应地址不再当成「加载失败」弹错误页。
    // 拒绝一个 sogou:// 之类的 scheme 时 Gecko 会回 onLoadError（UNKNOWN_PROTOCOL），
    // 若照常处理，用户会从正常的结果页被踢到完整错误页。
    private val blockedExternalUris = HashMap<String, MutableSet<String>>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    // 下载响应体捕获：Gecko 在 onExternalResponse 时已把响应体拉了下来，body 流只在回调期间有效，
    // 必须当场读走存成临时文件。之后下载直接从本地文件落盘，不再用 URL 第二次请求，
    // 从根本上避开柠枺等站点「一次性令牌二次请求 → 403」的问题。
    // key = 下载地址
    private val capturedBodies = ConcurrentHashMap<String, CapturedBody>()
    val selected get() = _tabs.value.firstOrNull { it.id == _selectedId.value }

    /**
     * 内核版本号（如 "158.0"），用于生成 UA。
     *
     * 优先用构建配置的版本：GeckoSession.getDefaultUserAgent() 在部分渠道/首次初始化时
     * 会返回一个与真实内核不同的旧版本号（实测会停在 150），
     * 导致版本检测网站报「不是最新版」。
     * 构建配置与依赖坐标同源（app/build.gradle.kts 的 geckoViewVersion），
     * 所以它一定等于实际打包进 APK 的内核版本，比运行时自报更可靠。
     *
     * 注意：必须声明在 init 之前！init 里的 newTab() → createSession() → userAgentOverride()
     * 会读取本属性；Kotlin 按声明顺序初始化字段，若本属性（by lazy 的委托字段）声明在 init 之后，
     * 构造期读取会命中尚未初始化的委托字段 → NullPointerException 导致启动闪退。
     */
    private val engineVersion: String by lazy {
        val configured = FALLBACK_ENGINE_VERSION.substringBefore('.').takeIf { it.toIntOrNull() != null }
        val runtime = runCatching { GeckoSession.getDefaultUserAgent() }.getOrNull().orEmpty()
        val runtimeMajor = Regex("Firefox/([0-9]+)").find(runtime)?.groupValues?.getOrNull(1)
            ?: Regex("rv:([0-9]+)").find(runtime)?.groupValues?.getOrNull(1)
        // 取两者中较大的主版本号：避免内核已升级但 UA 仍报旧版
        val major = listOfNotNull(configured, runtimeMajor)
            .mapNotNull { it.toIntOrNull() }
            .maxOrNull()
            ?: return@lazy FALLBACK_ENGINE_VERSION
        "$major.0"
    }

    init {
        // 兜底：ViewModel 构造期一旦抛异常，Compose 无法创建 ViewModel → 整个应用会直接闪退。
        // 这里把初始化包住：最坏情况只是初始标签没建好（首页仍能打开），而不是应用打不开。
        // 同时把异常写入崩溃日志，便于后续排查。
        runCatching { lastColorScheme = runtime.settings.getPreferredColorScheme() }
        runCatching { newTab() }
            .onFailure { CrashReporter.record(application, it) }
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

    /**
     * 在无痕环境里加载地址：复用最后一个无痕标签，没有则返回 false 由调用方新建。
     *
     * 与 [loadExternalUrl] 的区别：这里不看当前页是否空白——无痕页的搜索框
     * 就是一个新入口，用户希望在同一无痕标签里连续跳转，而不是每次堆一个新标签。
     * 全程只碰 incognito 标签，绝不影响普通标签。
     */
    fun loadInIncognito(input: String): Boolean {
        val tab = _tabs.value.lastOrNull { it.incognito } ?: return false
        _selectedId.value = tab.id
        val target = normalize(input)
        cancelPendingFailure(tab.id)
        if (attachedSessions.contains(tab.id)) {
            runCatching { tab.session.loadUri(target) }
        } else {
            pendingInitialLoads[tab.id] = target
            pendingLoadAttempts.remove(tab.id)
            mainHandler.postDelayed({ onSessionAttached(tab.id) }, 0)
        }
        return true
    }

    // GeckoView 视图挂载完会话后调用；此时 loadUri 才能可靠生效。
    // 仅在尚未开始尝试时触发，后续交给内部重试链，避免重复触发导致重复加载。
    fun onSessionAttached(id: String) {
        attachedSessions.add(id)
        if (pendingInitialLoads.containsKey(id) && (pendingLoadAttempts[id] ?: 0) == 0) loadPending(id)
    }

    /**
     * GeckoView 视图销毁时调用。
     *
     * 视图被重建（切换硬件加速、页面离开组合）后，旧视图已不再是该会话的宿主，
     * 必须清掉挂载标记；否则 load() 会以为「已挂载」而直接 loadUri，
     * 在真正挂载完成前又被静默丢弃，重新变成空白页。
     */
    fun onSessionDetached(id: String) {
        attachedSessions.remove(id)
    }

    // 外部链接的待加载地址需重试：视图刚挂载时 display 可能尚未就绪，
    // loadUri 会被静默丢弃，故在确认页面真正开始加载（onPageStart）前保留待加载项并重试。
    private fun loadPending(id: String) {
        val target = pendingInitialLoads[id] ?: return
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
     * 延迟确认加载失败：等待可能的下载信号，避免下载场景误弹错误页。
     *
     * 延迟到期后再做一次实时校验：搜索站（如搜狗）结果页会不断触发后台跳转/重定向，
     * Gecko 可能因此报一次 abort，但主页面其实已经渲染完成。
     * 若到时页面已完整加载，就不能再把正常结果页换成错误页。
     */
    private fun scheduleFailure(id: String, uri: String?, errorCode: Int, errorCategory: Int) {
        cancelPendingFailure(id)
        val task = Runnable {
            pendingFailures.remove(id)
            val tab = _tabs.value.firstOrNull { it.id == id }
            // 页面已真正加载成功（进度跑满且仍在同一条 http(s) 地址）时，丢弃这次失败。
            // 这类 abort 来自页面内的推广跳转，不代表当前页面不能用。
            if (tab != null && !tab.loading && tab.progress >= 100 && tab.url.startsWith("http") && !tab.failed) {
                android.util.Log.d("ZexIntent", "skipFailure id=$id url=${tab.url} 页面已加载完成")
                return@Runnable
            }
            update(id) { it.copy(failed = true, failedUrl = uri ?: it.url, errorCode = errorCode, errorCategory = errorCategory) }
        }
        pendingFailures[id] = task
        mainHandler.postDelayed(task, FailureConfirmDelayMillis)
    }

    /** 收到下载信号时取消待确认的失败，确保下载不会弹错误页 */
    private fun cancelPendingFailure(id: String) {
        pendingFailures.remove(id)?.let { mainHandler.removeCallbacks(it) }
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
            // 广告拦截：开启时对该会话启用 GeckoView 原生追踪保护（拦截广告/追踪请求）
            .useTrackingProtection(browserSettings.adBlockEnabled)
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
                // 页面已真正开始加载，待加载重试链必须立即停止。
                // 否则它会在 400ms 后再次 loadUri，把正在进行的重定向链条打断，
                // 表现为搜索后停在空白页。必须只在「真正的 http(s) 地址」
                // 上清除：会话初挂载会先产生一次 about:blank，那一次不能算数。
                if (url.startsWith("http")) clearPending(id)
                // 新一次加载开始，上一次排队的失败确认作废，避免中途误弹错误页
                cancelPendingFailure(id)
                update(id) { tab ->
                    // 加载失败后 Gecko 会导航到 about:blank 之类的内部页；
                    // 此时必须保留 failed 与失败网址，否则错误页会被冲掉、变成空白页。
                    if (url.startsWith("about:") && tab.failed) tab.copy(loading = true)
                    else tab.copy(url = url, loading = true, failed = false, failedUrl = "", errorCode = 0, errorCategory = 0, progress = 0)
                }
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                update(id) { it.copy(loading = false, progress = if (success) 100 else it.progress) }
                if (success) {
                    cancelPendingFailure(id)
                    if (!incognito) saveHistory(id)
                } else {
                    val tab = _tabs.value.firstOrNull { it.id == id }
                    // 已收到下载信号（onExternalResponse）时忽略此次 abort，不弹错误页
                    val isDownloadAbort = pendingDownloads[id] != null
                    // 当前标签停留在被拒绝的外部应用链接上时同样不算失败：
                    // 搜狗等搜索页会自动尝试跳 App，此时当前页仍是正常的结果页，不能换成错误页
                    val currentUrl = tab?.url.orEmpty()
                    val isBlockedExternal = currentUrl.isNotBlank() && isBlockedExternal(id, currentUrl)
                    if (!isDownloadAbort && !isBlockedExternal) scheduleFailure(id, tab?.url, 0, 0)
                }
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) = update(id) { it.copy(progress = progress) }
            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
                // 证书与混合内容状态由 Gecko 判定，比自己去解析 URL 可靠
                val mixedActive = securityInfo.mixedModeActive
                val mixedPassive = securityInfo.mixedModePassive
                val hasCert = securityInfo.certificate != null
                val secure = securityInfo.isSecure && !securityInfo.isException
                update(id) { tab ->
                    tab.copy(
                        risk = SecurityScanner.assess(
                            url = tab.url,
                            isSecure = secure,
                            mixedActive = mixedActive,
                            mixedPassive = mixedPassive,
                            hasCertificate = hasCert,
                            // 传入 GeckoView 自己的常量，不靠硬编码数值
                            contentLoaded = GeckoSession.ProgressDelegate.SecurityInformation.CONTENT_LOADED
                        )
                    )
                }
            }
        })
        session.setContentDelegate(object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) { update(id) { it.copy(title = title?.takeIf(String::isNotBlank) ?: it.title) } }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                // 收到下载信号：立即取消可能已排队的「加载失败」，避免下载时误弹错误页
                cancelPendingFailure(id)
                // 柠泽站点的 PoW 验证页是 HTML，Gecko 也会当它是一次外部响应。
                // 若照单全收，用户会看到一个名为 verify 的「下载」，点下去又因没有真正的文件而卡住。
                // 验证页应当留在网页内继续跑验证，不能弹下载框。
                if (!isRealFileDownload(response)) {
                    android.util.Log.d("ZexIntent", "ignoreNonFileResponse uri=${response.uri} type=${response.headers["Content-Type"]}")
                    return
                }
                pendingDownloads[id] = response.uri
                clearPending(id)
                // 仅对柠枺镜像下载就地捕获响应体（它的一次性令牌二次请求必 403）。
                // 普通站点下载保持原有「弹框 → 按 URL 下载」流程不变，避免大文件在确认前占双份磁盘。
                if (isLemwoodUrl(response.uri)) captureDownloadBody(response.uri, response)
                notifyExternalDownload(response.uri)
            }
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
            override fun onLocationChange(session: GeckoSession, url: String?, permissions: List<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                if (url != null) {
                    android.util.Log.d("ZexIntent", "onLocationChange id=$id url=$url")
                    val risk = SecurityScanner.assessUrl(url)
                    update(id) { tab ->
                        if (url.startsWith("about:") && tab.failed) tab
                        else tab.copy(url = url, risk = risk)
                    }
                }
            }
            // 记录失败的具体错误码与分类，供错误页展示原因；返回 null 表示不自定义错误页，交回 Gecko 默认处理。
            override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
                // 已收到下载信号时，Gecko 会因导航被下载接管而回调 onLoadError，
                // 这并非真正的加载失败，不能弹错误页。
                val pendingDownload = pendingDownloads[id]
                val isDownloadAbort = pendingDownload != null && (uri == null || isSameDownloadTarget(uri, pendingDownload))
                // 被我们主动拒绕的外部应用导航同样不是失败，否则当前页面会被错误页覆盖
                val isBlockedExternal = uri != null && isBlockedExternal(id, uri)
                if (!isDownloadAbort && !isBlockedExternal) scheduleFailure(id, uri, error.code, error.category)
                return null
            }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                // 下载交给 Gecko 自己判断（onExternalResponse）：
                // 很多站点的下载地址先 302 到验证/登录页，按后缀提前拒绝会让验证页根本加载不出来。
                val uri = request.uri

                // 风险站点拦截：只拦用户主动发起的导航与顶层页面，不拦资源加载。
                // 不用 isDirectNavigation：它表示「由应用调 load 发起」，语义不同。
                val userInitiated = request.hasUserGesture || request.isDirectNavigation
                if (shouldBlockRisky(uri) && userInitiated) {
                    val assessment = SecurityScanner.assessUrl(uri)
                    val handler = riskySiteHandler
                    if (handler != null) {
                        handler(uri, assessment) {
                            // 用户选择继续：记入放行名单后重新加载
                            allowedOnce.add(uri)
                            session.loadUri(uri)
                        }
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }

                // 外部应用 scheme：只有用户真的点了链接才值得询问。
                // 搜索站的结果页常带自动跳转的推广链接（如 sogou://），
                // 若连它们也弹窗，用户搜索一会儿就会被莫名其妙地问「是否打开外部应用」。
                if (uri.isExternalAppUri()) {
                    // 无论是否询问，都要登记：拒绝后 Gecko 会回 onLoadError，
                    // 没登记就会被当成加载失败，把用户从当前页面踢到错误页。
                    markBlockedExternal(id, uri)
                    if (userInitiated) requestExternalApp(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }
            override fun onSubframeLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                return null
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) = update(id) { it.copy(canGoBack = canGoBack) }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) = update(id) { it.copy(canGoForward = canGoForward) }
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (uri.isExternalAppUri()) { markBlockedExternal(id, uri); return null }
                return GeckoResult.fromValue(createNewSessionForUri(uri, incognito))
            }
        })
        return session to id
    }

    fun select(id: String) { _selectedId.value = id }
    fun close(id: String) {
        clearPending(id)
        attachedSessions.remove(id)
        cancelPendingFailure(id)
        pendingDownloads.remove(id)
        blockedExternalUris.remove(id)
        runCatching { _tabs.value.firstOrNull { it.id == id }?.session?.close() }
        _tabs.value = _tabs.value.filterNot { it.id == id }
        clearIncognitoContextIfEmpty()
        _selectedId.value = _tabs.value.lastOrNull()?.id
        if (_tabs.value.isEmpty()) newTab()
    }
    // 仅关闭普通标签，不动无痕标签
    fun closeAllNormal() {
        _tabs.value.filter { !it.incognito }.forEach { tab ->
            attachedSessions.remove(tab.id)
            runCatching { tab.session.close() }
        }
        _tabs.value = _tabs.value.filter { it.incognito }
        if (_tabs.value.isEmpty()) newTab() else _selectedId.value = _tabs.value.last().id
    }
    fun closeAllIncognito() {
        _tabs.value.filter { it.incognito }.forEach { tab ->
            attachedSessions.remove(tab.id)
            runCatching { tab.session.close() }
        }
        _tabs.value = _tabs.value.filterNot { it.incognito }
        purgeIncognitoContext()
        _selectedId.value = _tabs.value.lastOrNull()?.id ?: run { newTab(); _selectedId.value }
    }
    // 退出无痕浏览：彻底关闭全部无痕标签并清除其 Cookie/缓存，普通标签不受影响
    fun exitIncognito() {
        val incognitoTabs = _tabs.value.filter { it.incognito }
        if (incognitoTabs.isEmpty()) return
        incognitoTabs.forEach { tab ->
            attachedSessions.remove(tab.id)
            runCatching { tab.session.close() }
        }
        _tabs.value = _tabs.value.filterNot { it.incognito }
        purgeIncognitoContext()
        if (_tabs.value.isEmpty()) newTab() else _selectedId.value = _tabs.value.last().id
    }

    /**
     * 清除无痕存储上下文里的全部数据。
     *
     * clearDataForSessionContext 本身返回 void（异步投递给 Gecko），因此不能在调用后
     * 立即把 incognitoContextId 置空就完事——若调用本身抛异常，句柄已丢，残留数据再也清不掉。
     * 这里先把句柄取出交给应用级协程，异常时写崩溃日志留痕；
     * 协程跑在 applicationScope（SupervisorJob + IO）上，不随 ViewModel 销毁而取消。
     */
    private fun purgeIncognitoContext() {
        val contextId = incognitoContextId ?: return
        incognitoContextId = null
        applicationScope.launch {
            runCatching {
                runtime.storageController.clearDataForSessionContext(contextId)
            }.onFailure {
                // 清除失败时记入崩溃日志，便于排查残留数据问题；不向用户弹窗避免干扰
                CrashReporter.record(application, it)
            }
        }
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

    /**
     * 应用 HTTPS-Only 设置到 Gecko 运行时。
     *
     * 用 GeckoView 原生能力（setAllowInsecureConnections）：
     * - 关闭 → ALLOW_ALL（允许所有非 HTTPS 连接）
     * - 仅隐私标签页 → HTTPS_ONLY_PRIVATE
     * - 所有标签页 → HTTPS_ONLY
     */
    fun applyHttpsOnly(enabled: Boolean, mode: String) {
        val level = when {
            !enabled -> GeckoRuntimeSettings.ALLOW_ALL
            mode == "private" -> GeckoRuntimeSettings.HTTPS_ONLY_PRIVATE
            else -> GeckoRuntimeSettings.HTTPS_ONLY
        }
        runCatching { runtime.settings.setAllowInsecureConnections(level) }
    }

    /**
     * 应用「广告拦截」设置。
     *
     * 双层防护：
     * 1) 请求层：更新 GeckoView 反跟踪类别（关闭时置为 NONE），
     *    并同步每个已有会话的 useTrackingProtection（新建会话在 createSession 中已按设置设置）；
     * 2) 展示层：启用/停用内置 zex_adblock 扩展，用 CSS 隐藏网络层拦不到的常见内联广告位。
     *
     * 会话设置变更需重载页面才能生效，所以状态真正变化时对所有标签页 reload 一次。
     */
    fun applyAdBlock(enabled: Boolean) {
        val categories = if (enabled) {
            ContentBlocking.AntiTracking.AD or
                ContentBlocking.AntiTracking.ANALYTIC or
                ContentBlocking.AntiTracking.SOCIAL or
                ContentBlocking.AntiTracking.CRYPTOMINING
        } else {
            ContentBlocking.AntiTracking.NONE
        }
        runCatching { runtime.settings.contentBlocking.setAntiTracking(categories) }
        // 拦截数据库始终开启：关闭拦截后仍要能读到历史累计值（它只记录拦截事件的汇总，
        // 不记录访问内容），否则用户一关开关次数就归零，看起来像数据丢了
        runCatching { runtime.settings.contentBlocking.setContentBlockingDatabaseStatus(true) }
        runCatching { (getApplication<Application>() as ZexBrowseApplication).setAdBlockExtensionEnabled(enabled) }

        var changed = false
        _tabs.value.forEach { tab ->
            runCatching {
                if (tab.session.settings.useTrackingProtection != enabled) {
                    tab.session.settings.setUseTrackingProtection(enabled)
                    changed = true
                }
            }
        }
        if (changed) {
            _tabs.value.forEach { tab -> runCatching { tab.session.reload() } }
        }
    }

    /**
     * 读取累计拦截次数（真实数据）。
     *
     * 数据来自 GeckoView 的拦截数据库（contentBlockingDatabase）：每次真正拦下广告/追踪请求，
     * Gecko 会记一条事件，这里只做汇总读取，不自己计数——否则刷新页面、撤销拦截等
     * 情形都会让自己维护的计数器失真。
     */
    fun loadBlockedCount(onResult: (Long) -> Unit) {
        runCatching {
            runtime.contentBlockingController.sumAllTrackingDbEvents()
                .accept({ total: Int? -> onResult((total ?: 0).toLong()) }, { onResult(0L) })
        }.onFailure { onResult(0L) }
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
    /**
     * 加载一个地址（地址栏、首页搜索、外部链接共用）。
     *
     * 不能直接调 loadUri：首页搜索时浏览页还没组合，
     * GeckoView 尚未通过 setSession 挂到会话上，此时 loadUri 会被静默丢弃，
     * 表现为「搜索后直接是一个空白页」，且因为是竞态（渲染与挂载先后不定）只有概率出现。
     * 解法：视图已通过 onSessionAttached 声明就绪时才直接加载，
     * 否则先入队，等挂载回调到来时由重试链控制加载时机。
     */
    fun load(input: String) {
        val target = normalize(input)
        val tab = selected ?: return
        cancelPendingFailure(tab.id)
        if (attachedSessions.contains(tab.id)) {
            runCatching { tab.session.loadUri(target) }
            return
        }
        // 视图尚未挂载：交给待加载重试链，它会在 onPageStart 后自行停止
        pendingInitialLoads[tab.id] = target
        pendingLoadAttempts.remove(tab.id)
        mainHandler.postDelayed({ onSessionAttached(tab.id) }, 0)
    }
    fun back() { selected?.session?.goBack() }; fun forward() { selected?.session?.goForward() }
    fun reload() {
        val tab = selected ?: return
        // 手动重试时先清掉待确认失败，避免旧计时器在新一次加载过程中再次弹出错误页
        cancelPendingFailure(tab.id)
        val pending = pendingDownloads[tab.id]
        // 标签地址是下载直链（未被加载成页面、会被 onLoadRequest 拒绝）时，刷新按钮改为手动重新触发下载弹窗：
        // 既避免自动重试导致「点取消后反复弹出」，也避免一次性弹框被取消后「再也弹不出来」。
        if (pending != null) {
            notifyExternalDownload(pending)
        } else if (tab.failed && tab.failedUrl.isNotBlank()) {
            // 加载失败后 Gecko 已把会话地址改成 about:blank，直接 reload 只会重载空白页；
            // 必须重新加载失败的原始网址，否则错误页里的「重新加载」和工具栏重试都会变白屏。
            tab.session.loadUri(tab.failedUrl)
        } else {
            tab.session.reload()
        }
    }
    /** 立即停止当前标签的加载（用于加载中的工具栏按钮，避免用户卡在加载状态） */
    fun stopLoading() { selected?.session?.stop() }
    /**
     * 加入下载队列（与网页下载、外链下载同一套后台下载体系）。
     * 下载记录 id 通过 [onEnqueued] 回调返回，便于调用方跟踪进度（不阻塞界面线程）。
     */
    fun enqueueDownload(url: String, fileName: String, expectedHash: String, onEnqueued: (String) -> Unit = {}) {
        selected?.let { tab -> if (pendingDownloads[tab.id] == url) pendingDownloads.remove(tab.id) }
        // 若 onExternalResponse 已把响应体捕获到临时文件，取下它的路径传给 Worker，
        // Worker 直接读本地文件落盘，不再用 URL 请求（一次性令牌二次请求会 403）。
        val captured = capturedBodies.remove(url)
        viewModelScope.launch {
            val id = runCatching {
                downloadScheduler.enqueue(
                    url, fileName, expectedHash,
                    browserSettings.downloadDirectoryMode == "external",
                    browserSettings.externalDownloadTreeUri,
                    captured?.bin?.absolutePath
                )
            }.getOrNull()
            if (id != null) onEnqueued(id)
        }
    }
    fun cancelDownload(id: String) { viewModelScope.launch { downloadScheduler.cancel(id) } }
    /**
     * 取消推荐页的下载：按下载地址定位任务并清掉半成品与记录。
     * 不用记录 Id——入队是异步的，弹窗刚打开时 Id 可能还没回填。
     */
    fun cancelAndRemoveDownloadByUrl(url: String) {
        viewModelScope.launch {
            val record = database.downloadDao().findBySourceUrl(url) ?: return@launch
            downloadScheduler.cancel(record.id)
            downloadScheduler.delete(record.id, record.targetUri, deleteFile = true)
        }
    }
    fun pauseDownload(id: String) { viewModelScope.launch { downloadScheduler.pause(id) } }
    /** 继续用暂停的下载；记录已不存在时提示重新下载 */
    fun resumeDownload(id: String) {
        viewModelScope.launch {
            if (!downloadScheduler.resume(id)) {
                android.widget.Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.err_record_lost), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    /** 该地址是否已有本地捕获的响应体（用于下载弹框跳过可能二次消费令牌的网络名字探测） */
    fun hasCapturedBody(url: String): Boolean = capturedBodies.containsKey(url)
    /** 判断 URL 是否属于柠枺镜像站（仅它的下载需要就地捕获响应体） */
    private fun isLemwoodUrl(url: String): Boolean {
        if (!LemwoodMirror.isAvailable) return false
        val site = LemwoodMirror.siteUrl()
        return site.isNotBlank() && url.startsWith(site)
    }

    /**
     * 判断这次外部响应是不是真的文件下载。
     *
     * Gecko 把「服务器要求存盘」的响应交给 onExternalResponse，但柠泽的 PoW 验证页
     * 也会走到这里，而且验证页的响应往往**也带 Content-Disposition**，
     * 之前只看「有没有文件名」就放行，于是出现一个名为 verify、大小对不上的假下载。
     *
     * 因此改为按下面顺序判定：
     * 1. MIME 是网页 → 不是文件（即使带了 filename 也不弹下载框）；
     * 2. 文件名是 verify / 无扩展名等无意义名字 → 不是真文件；
     * 3. 有合法扩展名或落在柠泽 /download/ 直链上 → 真文件；
     * 4. 其余保持原有行为（当作下载）。
     */
    private fun isRealFileDownload(response: WebResponse): Boolean {
        val contentType = response.headers["Content-Type"].orEmpty().lowercase()
        // 验证页是 HTML：优先判掉，不能因为带了 filename 就当成下载
        if (contentType.startsWith("text/html") || contentType.startsWith("application/xhtml")) {
            return false
        }
        val disposition = response.headers["Content-Disposition"].orEmpty()
        val fileName = DownloadFileName.fromContentDisposition(disposition)
        if (fileName != null) {
            // 文件名本身必须像个真实文件（带扩展名、不是 verify 之类）
            val lower = fileName.lowercase()
            val looksReal = lower.contains('.') &&
                !lower.startsWith("verify") &&
                lower.substringAfterLast('.') !in setOf("html", "htm", "php", "aspx", "jsp")
            if (looksReal) return true
            // 带的是 verify.html 这类：明显是验证页，不能下载
            return false
        }
        // 柠泽下载直链：即使没带 Content-Disposition 也应当是真文件
        if (isLemwoodUrl(response.uri) && response.uri.contains("/download/")) return true
        // 其余保持原有行为（当作下载），不影响普通站点
        return true
    }
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
    private fun clearIncognitoContextIfEmpty() {
        if (_tabs.value.none { it.incognito }) {
            purgeIncognitoContext()
        }
    }
    private companion object {
        const val MaxPendingLoadAttempts = 20
        const val PendingLoadRetryMillis = 400L
        /** 失败确认延迟：等待可能的下载信号（onExternalResponse）到达，避免误弹错误页 */
        const val FailureConfirmDelayMillis = 900L
        /** 极少数情况下取不到默认 UA 时的兜底版本，取自构建配置，与依赖坐标保持同一来源 */
        val FALLBACK_ENGINE_VERSION = BuildConfig.GECKOVIEW_VERSION
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

    /** 记录一个被拒绝的外部应用导航，供 onLoadError 识别「这不是真的加载失败」 */
    private fun markBlockedExternal(id: String, uri: String) {
        val set = blockedExternalUris.getOrPut(id) { mutableSetOf() }
        // 搜索页会不断冒新的推广链接，限制条数避免无上限增长
        if (set.size > 64) set.clear()
        set.add(uri)
        // 同一跳转的查询串/锚点可能变化（推广链接普遍带追踪参数），
        // 额外按 scheme+host 登记一份，避免精确匹配落空后又弹错误页。
        schemeHostKey(uri)?.let { set.add(it) }
    }

    /** 取 scheme://host 作为宽松匹配键；解析不出 host 时返回 null */
    private fun schemeHostKey(uri: String): String? = runCatching {
        val parsed = java.net.URI(uri)
        val host = parsed.host?.takeIf { it.isNotBlank() }
        if (host == null) null else "${parsed.scheme}://$host"
    }.getOrNull()

    /** 该地址（或它所属的 scheme+host）是否已被我们主动拒绘过 */
    private fun isBlockedExternal(id: String, uri: String): Boolean {
        val set = blockedExternalUris[id] ?: return false
        if (uri in set) return true
        val key = schemeHostKey(uri) ?: return false
        return key in set
    }
    private fun saveHistory(id: String) { val tab = _tabs.value.firstOrNull { it.id == id } ?: return; if (tab.url.startsWith("http")) viewModelScope.launch(Dispatchers.IO) { database.dao().addHistory(HistoryEntity(title = tab.title, url = tab.url)) } }

    private fun userAgentOverride(settings: BrowserSettings): String? {
        // 简化标识：去掉设备型号、Build 号等隐私信息，但必须保持 Firefox UA 的合法格式，
        // 且版本号必须跟随实际内核（不能写死，否则升级内核后 UA 仍声称旧版本）。
        // 注意 Gecko/ 后面固定为 20100101（不是版本号），且需带 rv: 标记；
        // 写成 "Gecko/131" 这类非法格式会被必应等站点的反爬规则直接拒绝（请求被 blocked）。
        if (settings.simplifiedUserAgent) {
            return "Mozilla/5.0 (Android 14; Mobile; rv:$engineVersion) Gecko/20100101 Firefox/$engineVersion"
        }
        val userAgent = when (settings.userAgentMode) {
            "android_chrome" -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            "desktop_chrome" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            "desktop_firefox" -> "Mozilla/5.0 (X11; Linux x86_64; rv:$engineVersion) Gecko/20100101 Firefox/$engineVersion"
            "custom" -> settings.customUserAgent.trim().takeIf(String::isNotEmpty)
            // 默认模式也显式声明 Firefox 版本号：内核自报的 UA 可能停在旧版本（实测 150），
            // 会让版本检测网站误判「不是最新版」。这里直接用构建配置的内核版本。
            else -> "Mozilla/5.0 (Android 14; Mobile; rv:$engineVersion) Gecko/20100101 Firefox/$engineVersion"
        }
        return userAgent
    }

    private fun normalize(value: String): String {
        val text = value.trim()
        if (text.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) return text
        if (text.startsWith("www.") || text.matches(Regex("^[^\\s/]+\\.[^\\s/]+.*"))) return "https://$text"
        val encoded = URLEncoder.encode(text, "UTF-8")
        // 自定义搜索引擎统一以「custom:<标题>」形式保存，可添加多个、各自独立生效；
        // 旧版只有单个自定义搜索（custom），此时回退到 customSearchUrl 以兼容已有配置。
        val template = when {
            browserSettings.searchEngine == "baidu" -> "https://www.baidu.com/s?wd={query}"
            browserSettings.searchEngine == "bing" -> "https://www.bing.com/search?q={query}"
            browserSettings.searchEngine == "duckduckgo" -> "https://duckduckgo.com/?q={query}"
            browserSettings.searchEngine.startsWith(CUSTOM_SEARCH_PREFIX) -> resolveCustomSearchTemplate(browserSettings)
            browserSettings.searchEngine == "custom" -> browserSettings.customSearchUrl
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
