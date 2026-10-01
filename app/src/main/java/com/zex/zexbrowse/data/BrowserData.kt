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

@Dao
interface BrowserDao {
    @Insert
    suspend fun addHistory(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBookmark(item: BookmarkEntity)

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun history(): Flow<List<HistoryEntity>>
}

@Database(
    entities = [HistoryEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun dao(): BrowserDao

    companion object {
        fun create(context: Context): BrowserDatabase =
            Room.databaseBuilder(context, BrowserDatabase::class.java, "zexbrowse.db").build()
    }
}

data class BrowserSettings(
    val darkMode: String = "system",
    val dynamicColor: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val apkHashEnabled: Boolean = true,
    val userAgentMode: String = "geckoview",
    val customUserAgent: String = ""
)

class SettingsStore(private val context: Context) {
    private val modeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val cookiesKey = booleanPreferencesKey("cookies")
    private val apkHashKey = booleanPreferencesKey("apk_hash")
    private val userAgentModeKey = stringPreferencesKey("user_agent_mode")
    private val customUserAgentKey = stringPreferencesKey("custom_user_agent")

    val settings = context.dataStore.data.map { preferences ->
        BrowserSettings(
            darkMode = preferences[modeKey] ?: "system",
            dynamicColor = preferences[dynamicColorKey] ?: true,
            cookiesEnabled = preferences[cookiesKey] ?: true,
            apkHashEnabled = preferences[apkHashKey] ?: true,
            userAgentMode = preferences[userAgentModeKey] ?: "geckoview",
            customUserAgent = preferences[customUserAgentKey] ?: ""
        )
    }

    private suspend fun update(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit(transform)
    }

    suspend fun mode(value: String) = update { it[modeKey] = value }
    suspend fun dynamic(value: Boolean) = update { it[dynamicColorKey] = value }
    suspend fun cookies(value: Boolean) = update { it[cookiesKey] = value }
    suspend fun apkHash(value: Boolean) = update { it[apkHashKey] = value }
    suspend fun userAgentMode(value: String) = update { it[userAgentModeKey] = value }
    suspend fun customUserAgent(value: String) = update { it[customUserAgentKey] = value }
}
