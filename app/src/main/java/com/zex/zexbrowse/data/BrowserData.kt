/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("zexbrowse_settings")

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val sourceUrl: String,
    val targetUri: String,
    val status: String = "queued",
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = -1,
    val sha256: String = "",
    val expectedSha256: String = "",
    val errorMessage: String = "",
    /** 服务器返回的 ETag：续传时用 If-Range 校验文件是否还是同一份 */
    val etag: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(item: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("UPDATE downloads SET status = :status, progress = :progress, downloadedBytes = :downloaded, totalBytes = :total WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int, downloaded: Long, total: Long)

    @Query("UPDATE downloads SET status = :status, progress = 100, sha256 = :sha256, errorMessage = '' WHERE id = :id")
    suspend fun complete(id: String, status: String, sha256: String)

    @Query("UPDATE downloads SET status = :status, errorMessage = :message WHERE id = :id")
    suspend fun finishWithMessage(id: String, status: String, message: String)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun find(id: String): DownloadEntity?

    /** 按源地址取最新一条记录：推荐页刚入队、Id 尚未回填时靠它定位任务 */
    @Query("SELECT * FROM downloads WHERE sourceUrl = :url ORDER BY createdAt DESC LIMIT 1")
    suspend fun findBySourceUrl(url: String): DownloadEntity?

    /** 暂停：保留已下载字节数，状态标为 paused，界面据此显示「继续」 */
    @Query("UPDATE downloads SET status = 'paused', errorMessage = '' WHERE id = :id")
    suspend fun pause(id: String)

    /** 续传前把已落盘的偏移与 ETag 写回，状态恢复为等待下载 */
    @Query("UPDATE downloads SET status = 'queued', downloadedBytes = :downloaded, totalBytes = :total, etag = :etag, errorMessage = '' WHERE id = :id")
    suspend fun resume(id: String, downloaded: Long, total: Long, etag: String)

    /** 续传/改名后同步服务器真实文件名 */
    @Query("UPDATE downloads SET fileName = :fileName WHERE id = :id")
    suspend fun rename(id: String, fileName: String)

    /** 只记录 ETag，不动状态：下载进行中不动 state，避免界面闪回「等待下载」 */
    @Query("UPDATE downloads SET etag = :etag WHERE id = :id")
    suspend fun updateEtag(id: String, etag: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BrowserDao {
    @Insert
    suspend fun addHistory(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBookmark(item: BookmarkEntity)

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun history(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Database(entities = [HistoryEntity::class, BookmarkEntity::class, DownloadEntity::class], version = 3, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun dao(): BrowserDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS downloads (id TEXT NOT NULL PRIMARY KEY, fileName TEXT NOT NULL, sourceUrl TEXT NOT NULL, targetUri TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL, downloadedBytes INTEGER NOT NULL, totalBytes INTEGER NOT NULL, sha256 TEXT NOT NULL, expectedSha256 TEXT NOT NULL, errorMessage TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        // 断点续传需要记录 ETag：续传时用 If-Range 校验，
        // 服务器换过文件就必须从头下，否则会拼出损坏文件。
        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN etag TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var instance: BrowserDatabase? = null

        fun create(context: Context): BrowserDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BrowserDatabase::class.java, "zexbrowse.db")
                .addMigrations(migration1To2, migration2To3)
                .build()
                .also { instance = it }
        }
    }
}

data class BrowserSettings(
    val darkMode: String = "system",
    val forceDarkWeb: Boolean = true,
    val dynamicColor: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val apkHashEnabled: Boolean = true,
    val userAgentMode: String = "geckoview",
    val customUserAgentTitle: String = "自定义",
    val customUserAgent: String = "",
    /** 自定义 UA 列表，每行「标题|内容」，与自定义搜索、书签同格式 */
    val customUserAgents: String = "",
    val simplifiedUserAgent: Boolean = false,
    val searchEngine: String = "google",
    val customSearchTitle: String = "自定义搜索",
    val customSearchUrl: String = "",
    /** 自定义搜索引擎列表，每行「标题|搜索 URL」，与自定义 UA、书签同格式 */
    val customSearchEngines: String = "",
    val clearOnExit: Boolean = false,
    val clearCookiesOnExit: Boolean = true,
    val clearCacheOnExit: Boolean = true,
    val clearHistoryOnExit: Boolean = true,
    val downloadDirectoryMode: String = "internal",
    val externalDownloadTreeUri: String = "",
    val externalDownloadDisplayPath: String = "",
    val quickSites: String = "",
    val autoCheckUpdates: Boolean = true,
    val storageHintShown: Boolean = false,
    /** 首启「设为默认浏览器」引导是否已展示过；仅用于避免重复打扰，与是否已成为默认无关 */
    val defaultBrowserHintShown: Boolean = false,
    val clipboardReadPolicy: String = "ask",
    val micPolicy: String = "ask",
    val externalAppPolicy: String = "ask",
    /** 硬件加速：开启时 GeckoView 使用 SurfaceView 后端（默认），关闭后改用 TextureView 以降低部分机型的渲染开销 */
    val hardwareAcceleration: Boolean = true,
    val sitePermissions: Map<String, String> = emptyMap(),
    /** 麦克风权限的按站点记忆，格式同 [sitePermissions] */
    val siteMicPermissions: Map<String, String> = emptyMap(),
    /** 首页搜索框的搜索历史（新→旧，最多保留 10 条） */
    val searchHistory: List<String> = emptyList(),
    /** 安全防护模式：enhanced（增强）/ standard（标准）/ basic（基础） */
    val securityMode: String = "standard",
    /** HTTPS-Only 开关：开启后自动尝试以 HTTPS 加密协议连接网站，增强安全性 */
    val httpsOnlyEnabled: Boolean = true,
    /** HTTPS-Only 生效范围：all（所有标签页）/ private（仅隐私标签页） */
    val httpsOnlyMode: String = "all"
)

/** 自定义搜索引擎：标题 + 搜索 URL 模板（模板中用 {query} 表示关键词） */
data class CustomSearchEngine(val title: String, val url: String) {
    /** 选中项标识：以标题区分，存入 searchEngine 字段 */
    val value: String get() = "$CUSTOM_SEARCH_PREFIX$title"
}

/** 自定义搜索引擎选中值的前缀，避免与内置的 google / baidu 等取值冲突 */
const val CUSTOM_SEARCH_PREFIX = "custom:"

/** 自定义搜索引擎列表编解码，每行「标题|搜索 URL」，与自定义 UA、书签同格式 */
fun decodeCustomSearchEngines(raw: String): List<CustomSearchEngine> =
    raw.split("\n").mapNotNull { line ->
        val title = line.substringBefore('|').trim()
        val url = line.substringAfter('|', "").trim()
        if (title.isEmpty() || url.isEmpty() || !url.contains("{query}")) null
        else CustomSearchEngine(title, url)
    }

fun encodeCustomSearchEngines(list: List<CustomSearchEngine>): String =
    list.joinToString("\n") { "${it.title}|${it.url}" }

/**
 * 取出当前选中的自定义搜索引擎的 URL 模板。
 *
 * 选中值形如 `custom:<标题>`；找不到对应条目时回退到旧版的单个自定义搜索配置，
 * 保证升级前已配置好的自定义搜索引擎仍然生效。
 */
fun resolveCustomSearchTemplate(settings: BrowserSettings): String {
    val title = settings.searchEngine.removePrefix(CUSTOM_SEARCH_PREFIX)
    val matched = decodeCustomSearchEngines(settings.customSearchEngines).firstOrNull { it.title == title }
    return matched?.url ?: settings.customSearchUrl
}

class SettingsStore(private val context: Context) {
    private val modeKey = stringPreferencesKey("theme_mode")
    private val forceDarkWebKey = booleanPreferencesKey("force_dark_web")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val cookiesKey = booleanPreferencesKey("cookies")
    private val apkHashKey = booleanPreferencesKey("apk_hash")
    private val userAgentModeKey = stringPreferencesKey("user_agent_mode")
    private val customUserAgentTitleKey = stringPreferencesKey("custom_user_agent_title")
    private val customUserAgentKey = stringPreferencesKey("custom_user_agent")
    private val customUserAgentsKey = stringPreferencesKey("custom_user_agents")
    private val simplifiedUserAgentKey = booleanPreferencesKey("simplified_user_agent")
    private val searchEngineKey = stringPreferencesKey("search_engine")
    private val customSearchTitleKey = stringPreferencesKey("custom_search_title")
    private val customSearchUrlKey = stringPreferencesKey("custom_search_url")
    private val customSearchEnginesKey = stringPreferencesKey("custom_search_engines")
    private val clearOnExitKey = booleanPreferencesKey("clear_on_exit")
    private val clearCookiesOnExitKey = booleanPreferencesKey("clear_cookies_on_exit")
    private val clearCacheOnExitKey = booleanPreferencesKey("clear_cache_on_exit")
    private val clearHistoryOnExitKey = booleanPreferencesKey("clear_history_on_exit")
    private val downloadDirectoryModeKey = stringPreferencesKey("download_directory_mode")
    private val externalDownloadTreeUriKey = stringPreferencesKey("external_download_tree_uri")
    private val externalDownloadDisplayPathKey = stringPreferencesKey("external_download_display_path")
    private val quickSitesKey = stringPreferencesKey("quick_sites")
    private val autoCheckUpdatesKey = booleanPreferencesKey("auto_check_updates")
    private val storageHintShownKey = booleanPreferencesKey("storage_hint_shown")
    private val defaultBrowserHintShownKey = booleanPreferencesKey("default_browser_hint_shown")
    private val clipboardReadPolicyKey = stringPreferencesKey("clipboard_read_policy")
    private val micPolicyKey = stringPreferencesKey("mic_policy")
    private val externalAppPolicyKey = stringPreferencesKey("external_app_policy")
    private val hardwareAccelerationKey = booleanPreferencesKey("hardware_acceleration")
    private val sitePermissionsKey = stringPreferencesKey("site_permissions")
    private val siteMicPermissionsKey = stringPreferencesKey("site_mic_permissions")
    private val searchHistoryKey = stringPreferencesKey("search_history")
    private val securityModeKey = stringPreferencesKey("security_mode")
    private val httpsOnlyEnabledKey = booleanPreferencesKey("https_only_enabled")
    private val httpsOnlyModeKey = stringPreferencesKey("https_only_mode")

    val settings = context.dataStore.data.map { preferences ->
        BrowserSettings(
            darkMode = preferences[modeKey] ?: "system",
            forceDarkWeb = preferences[forceDarkWebKey] ?: true,
            dynamicColor = preferences[dynamicColorKey] ?: true,
            cookiesEnabled = preferences[cookiesKey] ?: true,
            apkHashEnabled = preferences[apkHashKey] ?: true,
            userAgentMode = preferences[userAgentModeKey] ?: "geckoview",
            customUserAgentTitle = preferences[customUserAgentTitleKey] ?: "自定义",
            customUserAgent = preferences[customUserAgentKey] ?: "",
            customUserAgents = preferences[customUserAgentsKey] ?: "",
            simplifiedUserAgent = preferences[simplifiedUserAgentKey] ?: false,
            searchEngine = preferences[searchEngineKey] ?: "google",
            customSearchTitle = preferences[customSearchTitleKey] ?: "自定义搜索",
            customSearchUrl = preferences[customSearchUrlKey] ?: "",
            customSearchEngines = preferences[customSearchEnginesKey] ?: "",
            clearOnExit = preferences[clearOnExitKey] ?: false,
            clearCookiesOnExit = preferences[clearCookiesOnExitKey] ?: true,
            clearCacheOnExit = preferences[clearCacheOnExitKey] ?: true,
            clearHistoryOnExit = preferences[clearHistoryOnExitKey] ?: true,
            downloadDirectoryMode = preferences[downloadDirectoryModeKey] ?: "internal",
            externalDownloadTreeUri = preferences[externalDownloadTreeUriKey] ?: "",
            externalDownloadDisplayPath = preferences[externalDownloadDisplayPathKey] ?: "",
            quickSites = preferences[quickSitesKey] ?: "",
            autoCheckUpdates = preferences[autoCheckUpdatesKey] ?: true,
            storageHintShown = preferences[storageHintShownKey] ?: false,
            defaultBrowserHintShown = preferences[defaultBrowserHintShownKey] ?: false,
            clipboardReadPolicy = preferences[clipboardReadPolicyKey] ?: "ask",
            micPolicy = preferences[micPolicyKey] ?: "ask",
            externalAppPolicy = preferences[externalAppPolicyKey] ?: "ask",
            hardwareAcceleration = preferences[hardwareAccelerationKey] ?: true,
            sitePermissions = (preferences[sitePermissionsKey] ?: "")
                .split("\n")
                .filter { it.isNotBlank() && it.contains('=') }
                .associate { it.substringBefore('=') to it.substringAfter('=') },
            siteMicPermissions = (preferences[siteMicPermissionsKey] ?: "")
                .split("\n")
                .filter { it.isNotBlank() && it.contains('=') }
                .associate { it.substringBefore('=') to it.substringAfter('=') },
            searchHistory = (preferences[searchHistoryKey] ?: "")
                .split("\n")
                .filter { it.isNotBlank() },
            securityMode = preferences[securityModeKey] ?: "standard",
            httpsOnlyEnabled = preferences[httpsOnlyEnabledKey] ?: true,
            httpsOnlyMode = preferences[httpsOnlyModeKey] ?: "all"
        )
    }

    private suspend fun update(transform: (MutablePreferences) -> Unit) = context.dataStore.edit(transform)

    suspend fun mode(value: String) = update { it[modeKey] = value }
    suspend fun forceDarkWeb(value: Boolean) = update { it[forceDarkWebKey] = value }
    suspend fun quickSites(value: String) = update { it[quickSitesKey] = value }
    suspend fun dynamic(value: Boolean) = update { it[dynamicColorKey] = value }
    suspend fun cookies(value: Boolean) = update { it[cookiesKey] = value }
    suspend fun apkHash(value: Boolean) = update { it[apkHashKey] = value }
    suspend fun userAgentMode(value: String) = update { it[userAgentModeKey] = value }
    suspend fun customUserAgent(title: String, value: String) = update { preferences ->
        preferences[customUserAgentTitleKey] = title
        preferences[customUserAgentKey] = value
    }
    suspend fun customUserAgent(value: String) = customUserAgent("自定义", value)
    suspend fun customUserAgents(value: String) = update { it[customUserAgentsKey] = value }
    suspend fun simplifiedUserAgent(value: Boolean) = update { it[simplifiedUserAgentKey] = value }
    suspend fun searchEngine(value: String) = update { it[searchEngineKey] = value }
    suspend fun customSearch(title: String, url: String) = update { preferences ->
        preferences[customSearchTitleKey] = title
        preferences[customSearchUrlKey] = url
    }
    suspend fun customSearchEngines(value: String) = update { it[customSearchEnginesKey] = value }
    suspend fun downloadDirectory(mode: String, treeUri: String = "", displayPath: String = "") = update { preferences ->
        preferences[downloadDirectoryModeKey] = mode
        preferences[externalDownloadTreeUriKey] = treeUri
        preferences[externalDownloadDisplayPathKey] = displayPath
    }
    suspend fun autoCheckUpdates(value: Boolean) = update { it[autoCheckUpdatesKey] = value }
    suspend fun storageHintShown(value: Boolean) = update { it[storageHintShownKey] = value }
    suspend fun defaultBrowserHintShown(value: Boolean) = update { it[defaultBrowserHintShownKey] = value }
    suspend fun hardwareAcceleration(value: Boolean) = update { it[hardwareAccelerationKey] = value }
    suspend fun clipboardReadPolicy(value: String) = update { it[clipboardReadPolicyKey] = value }
    suspend fun micPolicy(value: String) = update { it[micPolicyKey] = value }
    suspend fun externalAppPolicy(value: String) = update { it[externalAppPolicyKey] = value }
    suspend fun securityMode(value: String) = update { it[securityModeKey] = value }
    suspend fun httpsOnlyEnabled(value: Boolean) = update { it[httpsOnlyEnabledKey] = value }
    suspend fun httpsOnlyMode(value: String) = update { it[httpsOnlyModeKey] = value }

    /** 记录一次搜索：去重后置顶，仅保留最近 10 条 */
    suspend fun addSearchHistory(keyword: String) = update { preferences ->
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@update
        val current = (preferences[searchHistoryKey] ?: "")
            .split("\n")
            .filter { it.isNotBlank() && it != trimmed }
        preferences[searchHistoryKey] = (listOf(trimmed) + current).take(10).joinToString("\n")
    }

    /** 从搜索历史中移除单条 */
    suspend fun removeSearchHistory(keyword: String) = update { preferences ->
        val remaining = (preferences[searchHistoryKey] ?: "")
            .split("\n")
            .filter { it.isNotBlank() && it != keyword }
        preferences[searchHistoryKey] = remaining.joinToString("\n")
    }

    suspend fun clearSearchHistory() = update { it[searchHistoryKey] = "" }
    suspend fun setSitePermission(key: String, value: String) = update { preferences ->
        val current = preferences[sitePermissionsKey] ?: ""
        val entries = current.split("\n").filter { it.isNotBlank() && it.substringBefore('=') != key }
        preferences[sitePermissionsKey] = (entries + "$key=$value").joinToString("\n")
    }
    suspend fun clearSitePermissions() = update { it[sitePermissionsKey] = "" }
    suspend fun setSiteMicPermission(key: String, value: String) = update { preferences ->
        val current = preferences[siteMicPermissionsKey] ?: ""
        val entries = current.split("\n").filter { it.isNotBlank() && it.substringBefore('=') != key }
        preferences[siteMicPermissionsKey] = (entries + "$key=$value").joinToString("\n")
    }
    suspend fun clearSiteMicPermissions() = update { it[siteMicPermissionsKey] = "" }
    suspend fun clearOnExit(enabled: Boolean, cookies: Boolean, cache: Boolean, history: Boolean) = update { preferences ->
        preferences[clearOnExitKey] = enabled
        preferences[clearCookiesOnExitKey] = cookies
        preferences[clearCacheOnExitKey] = cache
        preferences[clearHistoryOnExitKey] = history
    }
}
