package io.github.vexpaer.mybp.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** 一晚睡眠（按起床日为键）。 */
@Entity(tableName = "sleep_nights")
data class SleepNightEntity(
    @PrimaryKey val wakeDayEpochDay: Long,
    val bedtimeEpochMillis: Long,
    val wakeEpochMillis: Long,
    val durationMinutes: Int,
    val isManual: Boolean,
    val updatedAtMillis: Long,
)

@Dao
interface SleepNightDao {

    @Query("SELECT * FROM sleep_nights ORDER BY wakeDayEpochDay DESC")
    fun observeAll(): Flow<List<SleepNightEntity>>

    @Query("SELECT * FROM sleep_nights WHERE wakeDayEpochDay = :wakeDay")
    suspend fun get(wakeDay: Long): SleepNightEntity?

    @Upsert
    suspend fun upsert(night: SleepNightEntity)

    @Query("DELETE FROM sleep_nights WHERE wakeDayEpochDay = :wakeDay")
    suspend fun delete(wakeDay: Long)
}

@Database(entities = [SleepNightEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepNightDao(): SleepNightDao
}
