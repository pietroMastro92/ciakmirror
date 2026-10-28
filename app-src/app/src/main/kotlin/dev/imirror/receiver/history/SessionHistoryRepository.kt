package dev.imirror.receiver.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.imirror.receiver.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// Separate DataStore file from settings: clearing history never touches user settings.
private val Context.historyDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "imirror_history")

/**
 * SessionHistoryRepository — local-only log of the last [MAX_RECORDS] AirPlay sessions.
 *
 * Stored in DataStore as a small JSON array. Never leaves the device and contains no
 * IP addresses, media URLs or content: only sender name, kind, start time and duration.
 */
class SessionHistoryRepository(context: Context) {

    private val appContext = context.applicationContext

    /** Newest first. */
    val history: Flow<List<SessionRecord>> = appContext.historyDataStore.data
        .catch { e ->
            Logger.e("Failed to read session history — using empty list", e)
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs -> decode(prefs[KEY_HISTORY]) }

    /** Prepends [record], keeping only the newest [MAX_RECORDS]. */
    suspend fun add(record: SessionRecord) {
        runCatching {
            appContext.historyDataStore.edit { prefs ->
                val current = decode(prefs[KEY_HISTORY])
                val updated = (listOf(record) + current).take(MAX_RECORDS)
                prefs[KEY_HISTORY] = encode(updated)
            }
        }.onFailure { Logger.e("Failed to save session history", it) }
    }

    suspend fun clear() {
        runCatching {
            appContext.historyDataStore.edit { prefs -> prefs.remove(KEY_HISTORY) }
        }.onFailure { Logger.e("Failed to clear session history", it) }
    }

    // ─── JSON (org.json is part of the Android framework: no extra dependency) ──

    private fun encode(records: List<SessionRecord>): String {
        val array = JSONArray()
        records.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("name", r.deviceName)
                    put("kind", r.kind.name)
                    put("started", r.startedAt)
                    put("duration", r.durationMs)
                    put("outcome", r.outcome.name)
                }
            )
        }
        return array.toString()
    }

    private fun decode(json: String?): List<SessionRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    add(
                        SessionRecord(
                            id = o.optLong("id"),
                            deviceName = o.optString("name", "AirPlay"),
                            kind = runCatching { SessionKind.valueOf(o.optString("kind")) }
                                .getOrDefault(SessionKind.MIRRORING),
                            startedAt = o.optLong("started"),
                            durationMs = o.optLong("duration"),
                            outcome = runCatching { SessionOutcome.valueOf(o.optString("outcome")) }
                                .getOrDefault(SessionOutcome.COMPLETED),
                        )
                    )
                }
            }
        }.getOrElse {
            Logger.e("Corrupt session history JSON — ignoring", it)
            emptyList()
        }
    }

    private companion object {
        const val MAX_RECORDS = 20
        val KEY_HISTORY = stringPreferencesKey("session_history_json")
    }
}
