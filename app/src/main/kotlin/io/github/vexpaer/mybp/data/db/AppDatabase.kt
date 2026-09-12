package io.github.vexpaer.mybp.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
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

/** 用户自建的运动（不写死种类）。归档=软删除。 */
@Entity(tableName = "exercise_defs")
data class ExerciseDefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

/** 一次运动记录，数值字段按模式选用。 */
@Entity(tableName = "exercise_records")
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val defId: Long,
    val dayEpochDay: Long,
    val sets: Int? = null,
    val reps: Int? = null,
    val seconds: Long? = null,
    val meters: Int? = null,
    val kilograms: Double? = null,
    val createdAt: Long,
)

/** 列表展示用行：记录 + 运动名/模式。 */
data class RecordRow(
    val id: Long,
    val defId: Long,
    val dayEpochDay: Long,
    val sets: Int?,
    val reps: Int?,
    val seconds: Long?,
    val meters: Int?,
    val kilograms: Double?,
    val createdAt: Long,
    val defName: String,
    val defMode: String,
)

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise_defs WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeDefs(): Flow<List<ExerciseDefEntity>>

    @Query("SELECT r.id, r.defId, r.dayEpochDay, r.sets, r.reps, r.seconds, r.meters, r.kilograms, r.createdAt, d.name AS defName, d.mode AS defMode FROM exercise_records r INNER JOIN exercise_defs d ON r.defId = d.id WHERE d.archived = 0 ORDER BY r.dayEpochDay DESC, r.createdAt DESC LIMIT 300")
    fun observeRecentRecords(): Flow<List<RecordRow>>

    @Query("SELECT * FROM exercise_records WHERE id = :id")
    suspend fun getRecord(id: Long): ExerciseRecordEntity?

    @Upsert
    suspend fun upsertDef(def: ExerciseDefEntity): Long

    @Upsert
    suspend fun upsertRecord(record: ExerciseRecordEntity)

    @Query("UPDATE exercise_defs SET archived = 1 WHERE id = :id")
    suspend fun archiveDef(id: Long)

    @Query("DELETE FROM exercise_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)
}

@Database(
    entities = [SleepNightEntity::class, ExerciseDefEntity::class, ExerciseRecordEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepNightDao(): SleepNightDao
    abstract fun exerciseDao(): ExerciseDao
}
