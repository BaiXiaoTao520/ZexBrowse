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

@Database(entities = [HistoryEntity::class, BookmarkEntity::class, DownloadEntity::class], version = 2, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun dao(): BrowserDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS downloads (id TEXT NOT NULL PRIMARY KEY, fileName TEXT NOT NULL, sourceUrl TEXT NOT NULL, targetUri TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL, downloadedBytes INTEGER NOT NULL, totalBytes INTEGER NOT NULL, sha256 TEXT NOT NULL, expectedSha256 TEXT NOT NULL, errorMessage TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        @Volatile private var instance: BrowserDatabase? = null

        fun create(context: Context): BrowserDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BrowserDatabase::class.java, "zexbrowse.db")
                .addMigrations(migration1To2)
                .build()
                .also { instance = it }
        }
    }
}

data class BrowserSettings(
    val darkMode: String = "system",
    val dynamicColor: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val apkHashEnabled: Boolean = true,
    val userAgentMode: String = "geckoview",
    val customUserAgentTitle: String = "自定义",
    val customUserAgent: String = "",
    val simplifiedUserAgent: Boolean = false,
    val searchEngine: String = "google",
    val customSearchTitle: String = "自定义搜索",
    val customSearchUrl: String = "",
    val clearOnExit: Boolean = false,
    val clearCookiesOnExit: Boolean = true,
    val clearCacheOnExit: Boolean = true,
    val clearHistoryOnExit: Boolean = true,
    val downloadDirectoryMode: String = "internal",
    val externalDownloadTreeUri: String = "",
    val externalDownloadDisplayPath: String = "/storage/emulated/0/Download/"
)

class SettingsStore(private val context: Context) {
    private val modeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val cookiesKey = booleanPreferencesKey("cookies")
    private val apkHashKey = booleanPreferencesKey("apk_hash")
    private val userAgentModeKey = stringPreferencesKey("user_agent_mode")
    private val customUserAgentTitleKey = stringPreferencesKey("custom_user_agent_title")
    private val customUserAgentKey = stringPreferencesKey("custom_user_agent")
    private val simplifiedUserAgentKey = booleanPreferencesKey("simplified_user_agent")
    private val searchEngineKey = stringPreferencesKey("search_engine")
    private val customSearchTitleKey = stringPreferencesKey("custom_search_title")
    private val customSearchUrlKey = stringPreferencesKey("custom_search_url")
    private val clearOnExitKey = booleanPreferencesKey("clear_on_exit")
    private val clearCookiesOnExitKey = booleanPreferencesKey("clear_cookies_on_exit")
    private val clearCacheOnExitKey = booleanPreferencesKey("clear_cache_on_exit")
    private val clearHistoryOnExitKey = booleanPreferencesKey("clear_history_on_exit")
    private val downloadDirectoryModeKey = stringPreferencesKey("download_directory_mode")
    private val externalDownloadTreeUriKey = stringPreferencesKey("external_download_tree_uri")
    private val externalDownloadDisplayPathKey = stringPreferencesKey("external_download_display_path")

    val settings = context.dataStore.data.map { preferences ->
        BrowserSettings(
            darkMode = preferences[modeKey] ?: "system",
            dynamicColor = preferences[dynamicColorKey] ?: true,
            cookiesEnabled = preferences[cookiesKey] ?: true,
            apkHashEnabled = preferences[apkHashKey] ?: true,
            userAgentMode = preferences[userAgentModeKey] ?: "geckoview",
            customUserAgentTitle = preferences[customUserAgentTitleKey] ?: "自定义",
            customUserAgent = preferences[customUserAgentKey] ?: "",
            simplifiedUserAgent = preferences[simplifiedUserAgentKey] ?: false,
            searchEngine = preferences[searchEngineKey] ?: "google",
            customSearchTitle = preferences[customSearchTitleKey] ?: "自定义搜索",
            customSearchUrl = preferences[customSearchUrlKey] ?: "",
            clearOnExit = preferences[clearOnExitKey] ?: false,
            clearCookiesOnExit = preferences[clearCookiesOnExitKey] ?: true,
            clearCacheOnExit = preferences[clearCacheOnExitKey] ?: true,
            clearHistoryOnExit = preferences[clearHistoryOnExitKey] ?: true,
            downloadDirectoryMode = preferences[downloadDirectoryModeKey] ?: "internal",
            externalDownloadTreeUri = preferences[externalDownloadTreeUriKey] ?: "",
            externalDownloadDisplayPath = preferences[externalDownloadDisplayPathKey] ?: "/storage/emulated/0/Download/"
        )
    }

    private suspend fun update(transform: (MutablePreferences) -> Unit) = context.dataStore.edit(transform)

    suspend fun mode(value: String) = update { it[modeKey] = value }
    suspend fun dynamic(value: Boolean) = update { it[dynamicColorKey] = value }
    suspend fun cookies(value: Boolean) = update { it[cookiesKey] = value }
    suspend fun apkHash(value: Boolean) = update { it[apkHashKey] = value }
    suspend fun userAgentMode(value: String) = update { it[userAgentModeKey] = value }
    suspend fun customUserAgent(title: String, value: String) = update { preferences ->
        preferences[customUserAgentTitleKey] = title
        preferences[customUserAgentKey] = value
    }
    suspend fun customUserAgent(value: String) = customUserAgent("自定义", value)
    suspend fun simplifiedUserAgent(value: Boolean) = update { it[simplifiedUserAgentKey] = value }
    suspend fun searchEngine(value: String) = update { it[searchEngineKey] = value }
    suspend fun customSearch(title: String, url: String) = update { preferences ->
        preferences[customSearchTitleKey] = title
        preferences[customSearchUrlKey] = url
    }
    suspend fun downloadDirectory(mode: String, treeUri: String = "", displayPath: String = "/storage/emulated/0/Download/") = update { preferences ->
        preferences[downloadDirectoryModeKey] = mode
        preferences[externalDownloadTreeUriKey] = treeUri
        preferences[externalDownloadDisplayPathKey] = displayPath
    }
    suspend fun clearOnExit(enabled: Boolean, cookies: Boolean, cache: Boolean, history: Boolean) = update { preferences ->
        preferences[clearOnExitKey] = enabled
        preferences[clearCookiesOnExitKey] = cookies
        preferences[clearCacheOnExitKey] = cache
        preferences[clearHistoryOnExitKey] = history
    }
}
