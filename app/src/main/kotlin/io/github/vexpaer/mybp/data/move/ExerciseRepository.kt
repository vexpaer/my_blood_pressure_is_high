package io.github.vexpaer.mybp.data.move

import io.github.vexpaer.mybp.core.move.ExerciseMode
import io.github.vexpaer.mybp.core.move.ExerciseValues
import io.github.vexpaer.mybp.data.db.AppDatabase
import io.github.vexpaer.mybp.data.db.ExerciseDefEntity
import io.github.vexpaer.mybp.data.db.ExerciseRecordEntity
import io.github.vexpaer.mybp.data.db.RecordRow
import kotlinx.coroutines.flow.Flow

/** 运动 CRUD：所有数据只进本地 Room。 */
class ExerciseRepository(
    db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.exerciseDao()

    val defs: Flow<List<ExerciseDefEntity>> = dao.observeDefs()
    val records: Flow<List<RecordRow>> = dao.observeRecentRecords()

    suspend fun createDef(name: String, mode: ExerciseMode): Long {
        val cleaned = name.trim().take(12)
        require(cleaned.isNotEmpty()) { "运动名称不能为空" }
        return dao.upsertDef(
            ExerciseDefEntity(name = cleaned, mode = mode.name, createdAt = now()),
        )
    }

    suspend fun archiveDef(id: Long) = dao.archiveDef(id)

    suspend fun addRecord(defId: Long, dayEpochDay: Long, values: ExerciseValues) {
        dao.upsertRecord(
            ExerciseRecordEntity(
                defId = defId,
                dayEpochDay = dayEpochDay,
                sets = values.sets,
                reps = values.reps,
                seconds = values.seconds,
                meters = values.meters,
                kilograms = values.kilograms,
                createdAt = now(),
            ),
        )
    }

    suspend fun updateRecord(existing: ExerciseRecordEntity, values: ExerciseValues) {
        dao.upsertRecord(
            existing.copy(
                sets = values.sets,
                reps = values.reps,
                seconds = values.seconds,
                meters = values.meters,
                kilograms = values.kilograms,
            ),
        )
    }

    suspend fun getRecord(id: Long): ExerciseRecordEntity? = dao.getRecord(id)

    suspend fun deleteRecord(id: Long) = dao.deleteRecord(id)
}
