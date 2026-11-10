package com.cex.insightflowstore.simulator

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SimulationRunStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class SimulationRunRecord(
    val runId: String,
    val scenarioCode: String,
    val targetClassIndex: Int,
    val seed: Long,
    val requestedSessions: Int,
    val completedSessions: Int,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val status: SimulationRunStatus
) {
    val formattedStartTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startTimestamp))

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("runId", runId)
            put("scenarioCode", scenarioCode)
            put("targetClassIndex", targetClassIndex)
            put("seed", seed)
            put("requestedSessions", requestedSessions)
            put("completedSessions", completedSessions)
            put("startTimestamp", startTimestamp)
            put("endTimestamp", endTimestamp ?: JSONObject.NULL)
            put("status", status.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): SimulationRunRecord? {
            return try {
                val runId = json.getString("runId")
                val scenarioCode = json.getString("scenarioCode")
                val targetClassIndex = json.optInt("targetClassIndex", 0)
                val seed = json.getLong("seed")
                val requestedSessions = json.getInt("requestedSessions")
                val completedSessions = json.getInt("completedSessions")
                val startTimestamp = json.getLong("startTimestamp")
                val endTimestamp = if (json.isNull("endTimestamp")) null else json.optLong("endTimestamp")
                val statusStr = json.optString("status", SimulationRunStatus.COMPLETED.name)
                val status = try {
                    SimulationRunStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    SimulationRunStatus.COMPLETED
                }
                SimulationRunRecord(
                    runId = runId,
                    scenarioCode = scenarioCode,
                    targetClassIndex = targetClassIndex,
                    seed = seed,
                    requestedSessions = requestedSessions,
                    completedSessions = completedSessions,
                    startTimestamp = startTimestamp,
                    endTimestamp = if (endTimestamp != null && endTimestamp > 0) endTimestamp else null,
                    status = status
                )
            } catch (e: Exception) {
                Log.w("SimulationManifest", "Failed to parse manifest entry: ${e.message}")
                null
            }
        }
    }
}

class SimulationManifestManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _runs = MutableStateFlow<List<SimulationRunRecord>>(emptyList())
    val runs: StateFlow<List<SimulationRunRecord>> = _runs.asStateFlow()

    init {
        loadRuns()
    }

    @Synchronized
    fun loadRuns(): List<SimulationRunRecord> {
        val rawJson = prefs.getString(KEY_RUN_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<SimulationRunRecord>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val record = SimulationRunRecord.fromJson(obj)
                if (record != null) {
                    list.add(record)
                }
            }
        } catch (e: Exception) {
            Log.e("SimulationManifest", "Failed to parse simulation manifest history: ${e.message}")
        }
        _runs.value = list
        return list
    }

    @Synchronized
    fun saveRun(record: SimulationRunRecord) {
        val current = loadRuns().toMutableList()
        val index = current.indexOfFirst { it.runId == record.runId }
        if (index >= 0) {
            current[index] = record
        } else {
            current.add(0, record)
        }

        val trimmed = current.take(MAX_HISTORY_COUNT)
        _runs.value = trimmed

        val jsonArray = JSONArray()
        trimmed.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_RUN_HISTORY, jsonArray.toString()).apply()
    }

    @Synchronized
    fun clearHistory() {
        _runs.value = emptyList()
        prefs.edit().remove(KEY_RUN_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "simulation_run_manifest"
        private const val KEY_RUN_HISTORY = "run_history"
        private const val MAX_HISTORY_COUNT = 100
    }
}
