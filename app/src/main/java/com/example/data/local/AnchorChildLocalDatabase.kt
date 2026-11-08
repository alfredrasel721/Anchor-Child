package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_app_rules")
data class CachedAppRuleEntity(
    @PrimaryKey val ruleId: String,
    val familyId: String,
    val childId: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val dailyLimitMinutes: Int,
    val usedMinutesToday: Int,
    val status: String,
    val blocked: Boolean,
    val whitelisted: Boolean,
    val scheduleOnly: Boolean,
    val cachedAtMs: Long
)

@Entity(tableName = "cached_schedules")
data class CachedScheduleEntity(
    @PrimaryKey val scheduleId: String,
    val familyId: String,
    val childId: String,
    val title: String,
    val windowDescription: String,
    val daysSummary: String,
    val modeCategory: String,
    val allowedAppsSummary: String,
    val status: String,
    val enabled: Boolean,
    val cachedAtMs: Long
)

@Entity(
    tableName = "offline_sync_queue",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class OfflineSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idempotencyKey: String = "op_${System.nanoTime()}",
    val actionType: String, // EXTRA_TIME_REQUEST | SAFE_CHECK_IN | FAMILY_CHAT | SOS_BEACON | TASK_COMPLETE
    val title: String,
    val payloadSummary: String,
    val targetName: String = "",
    val targetCategory: String = "",
    val bonusMinutes: Int = 0,
    val createdAtLabel: String,
    val retryCount: Int = 0,
    val synced: Boolean = false
)

@Dao
interface AnchorChildLocalDao {
    @Query("SELECT * FROM cached_app_rules ORDER BY category ASC, appName ASC")
    fun observeCachedAppRules(): Flow<List<CachedAppRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRules(rules: List<CachedAppRuleEntity>)

    @Query("SELECT * FROM cached_schedules ORDER BY scheduleId ASC")
    fun observeCachedSchedules(): Flow<List<CachedScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedules(schedules: List<CachedScheduleEntity>)

    @Query("SELECT * FROM offline_sync_queue ORDER BY id DESC")
    fun observeOfflineQueue(): Flow<List<OfflineSyncQueueEntity>>

    @Query("SELECT * FROM offline_sync_queue WHERE synced = 0 ORDER BY id ASC")
    suspend fun getPendingUnsyncedItems(): List<OfflineSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueueOfflineAction(item: OfflineSyncQueueEntity)

    @Query("UPDATE offline_sync_queue SET synced = 1 WHERE id = :itemId")
    suspend fun markItemSynced(itemId: Long)

    @Query("UPDATE offline_sync_queue SET retryCount = retryCount + 1 WHERE id = :itemId")
    suspend fun incrementRetryCount(itemId: Long)

    @Query("DELETE FROM offline_sync_queue WHERE synced = 1")
    suspend fun clearSyncedItems()
}

@Database(
    entities = [
        CachedAppRuleEntity::class,
        CachedScheduleEntity::class,
        OfflineSyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AnchorChildLocalDatabase : RoomDatabase() {
    abstract fun childLocalDao(): AnchorChildLocalDao

    companion object {
        @Volatile
        private var INSTANCE: AnchorChildLocalDatabase? = null

        fun getInstance(context: Context): AnchorChildLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorChildLocalDatabase::class.java,
                    "anchor_child_offline_cache.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
