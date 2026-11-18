package fr.julie.mariosprint.data.local

import android.content.Context
import androidx.room.withTransaction
import fr.julie.mariosprint.data.local.model.*
import fr.julie.mariosprint.domain.game.Level
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Import atomique et rejouable après interruption. Les préférences v1 restent intactes. */
internal class LegacyRecordsMigration(private val context: Context, private val database: SprintDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val mutex = Mutex()
    private var initialized = false
    suspend fun initialize() = mutex.withLock {
        if (initialized) return@withLock
        withContext(ioDispatcher) {
            database.withTransaction {
                val dao = database.recordsDao()
                if (dao.settings() == null) {
                    val legacy = context.getSharedPreferences("prairie-v1", Context.MODE_PRIVATE)
                    dao.saveStats(StatsEntity(Level.LEGACY_ID, legacy.getLong("bestMs", 0).takeIf { it > 0 },
                        legacy.getInt("attempts", 0), legacy.getInt("finishes", 0), legacy.getInt("coins", 0)))
                    legacy.all.keys.filter { it.startsWith("pending:") && it.endsWith(":time") }.forEach { key ->
                        val uid = key.removePrefix("pending:").removeSuffix(":time")
                        val time = legacy.getLong(key, 0)
                        if (uid.isNotBlank() && time > 0) dao.savePending(PendingRecordEntity(Level.LEGACY_ID, uid, time,
                            legacy.getInt("pending:$uid:coins", 0)))
                    }
                    dao.saveSettings(SettingsEntity(sound = legacy.getBoolean("sound", true)))
                }
            }
        }
        initialized = true
    }
}
