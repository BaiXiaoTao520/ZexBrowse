/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("zexbrowse_settings")

@Entity(tableName = "history") data class HistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val url: String, val visitedAt: Long = System.currentTimeMillis())
@Entity(tableName = "bookmarks") data class BookmarkEntity(@PrimaryKey val url: String, val title: String, val createdAt: Long = System.currentTimeMillis())
@Dao interface BrowserDao { @Insert suspend fun addHistory(item: HistoryEntity); @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBookmark(item: BookmarkEntity); @Query("SELECT * FROM history ORDER BY visitedAt DESC") fun history(): Flow<List<HistoryEntity>> }
@Database(entities = [HistoryEntity::class, BookmarkEntity::class], version = 1) abstract class BrowserDatabase : RoomDatabase() { abstract fun dao(): BrowserDao; companion object { fun create(context: Context) = Room.databaseBuilder(context, BrowserDatabase::class.java, "zexbrowse.db").build() } }

data class BrowserSettings(
    val darkMode: String = "system",
    val dynamicColor: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val apkHashEnabled: Boolean = true,
    val userAgentMode: String = "geckoview",
    val customUserAgent: String = ""
)
class SettingsStore(private val context: Context) {
    private val mode = stringPreferencesKey("theme_mode"); private val dynamic = booleanPreferencesKey("dynamic_color"); private val cookies = booleanPreferencesKey("cookies"); private val hash = booleanPreferencesKey("apk_hash"); private val userAgentMode = stringPreferencesKey("user_agent_mode"); private val customUserAgent = stringPreferencesKey("custom_user_agent")
    val settings = context.dataStore.data.map { BrowserSettings(it[mode] ?: "system", it[dynamic] ?: true, it[cookies] ?: true, it[hash] ?: true, it[userAgentMode] ?: "geckoview", it[customUserAgent] ?: "") }
    suspend fun update(transform: (MutablePreferences) -> Unit) { context.dataStore.edit(transform) }
    suspend fun clear() { context.dataStore.edit { it.clear() } }
    fun mode(value: String) = update { it[mode] = value }; fun dynamic(value: Boolean) = update { it[dynamic] = value }; fun cookies(value: Boolean) = update { it[cookies] = value }; fun apkHash(value: Boolean) = update { it[hash] = value }; fun userAgentMode(value: String) = update { it[userAgentMode] = value }; fun customUserAgent(value: String) = update { it[customUserAgent] = value }
}
