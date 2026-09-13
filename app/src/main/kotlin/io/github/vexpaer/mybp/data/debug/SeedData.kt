package io.github.vexpaer.mybp.data.debug

import io.github.vexpaer.mybp.core.move.ExerciseMode
import io.github.vexpaer.mybp.data.db.AppDatabase
import io.github.vexpaer.mybp.data.db.ExerciseDefEntity
import io.github.vexpaer.mybp.data.db.ExerciseRecordEntity
import io.github.vexpaer.mybp.data.db.SleepNightEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 演示/截图用的种子数据。
 * 仅当通过 adb 显式传入 seed_demo_data="mybp-seed-2026" 且本地库为空时才会执行；
 * 不碰任何网络，只往本机 Room 写入几条示例记录。真实用户数据永远不会被覆盖。
 */
object SeedData {

    const val EXPECTED_KEY = "mybp-seed-2026"

    suspend fun seedIfNeeded(db: AppDatabase, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val sleepDao = db.sleepNightDao()
        val exerciseDao = db.exerciseDao()
        if (sleepDao.getAllOnce().isNotEmpty() || exerciseDao.getDefsOnce().isNotEmpty()) {
            return false
        }

        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)

        // 7 晚睡眠：时长在 6h20m–8h10m 之间，其中一晚手动修改
        val nights = listOf(
            Triple(6, 23, 470), Triple(5, 23, 452), Triple(4, 23, 496), Triple(3, 23, 431),
            Triple(2, 23, 448), Triple(1, 23, 380), Triple(0, 23, 465),
        ).mapIndexed { index, (daysAgo, bedHour, minutes) ->
            val wakeDay = today.minusDays(daysAgo.toLong())
            val bed = wakeDay.minusDays(1).atTime(LocalTime.of(bedHour, if (index % 2 == 0) 36 else 52))
            val wake = wakeDay.atTime(LocalTime.of(7, (15 + index * 3) % 45))
            SleepNightEntity(
                wakeDayEpochDay = wakeDay.toEpochDay(),
                bedtimeEpochMillis = bed.atZone(zone).toInstant().toEpochMilli(),
                wakeEpochMillis = wake.atZone(zone).toInstant().toEpochMilli(),
                durationMinutes = minutes,
                isManual = index == 5,
                updatedAtMillis = now,
            )
        }
        nights.forEach { sleepDao.upsert(it) }

        // 运动：深蹲 / 跑步 / 平板支撑，今天两条、最近几天各有分布，保证连续 3 天
        val squat = exerciseDao.upsertDef(
            ExerciseDefEntity(name = "深蹲", mode = ExerciseMode.SETS_REPS.name, createdAt = now - 6_000),
        )
        val run = exerciseDao.upsertDef(
            ExerciseDefEntity(name = "跑步", mode = ExerciseMode.DISTANCE.name, createdAt = now - 5_000),
        )
        val plank = exerciseDao.upsertDef(
            ExerciseDefEntity(name = "平板支撑", mode = ExerciseMode.TIME.name, createdAt = now - 4_000),
        )

        fun day(day: Long, hour: Int, minute: Int) =
            today.minusDays(day).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

        val records = listOf(
            ExerciseRecordEntity(defId = squat, dayEpochDay = today.toEpochDay() - 1, sets = 3, reps = 12, createdAt = day(1, 8, 10)),
            ExerciseRecordEntity(defId = run, dayEpochDay = today.toEpochDay() - 1, seconds = 1020, meters = 2400, createdAt = day(1, 19, 5)),
            ExerciseRecordEntity(defId = squat, dayEpochDay = today.toEpochDay() - 2, sets = 3, reps = 10, createdAt = day(2, 8, 2)),
            ExerciseRecordEntity(defId = plank, dayEpochDay = today.toEpochDay() - 2, seconds = 60, createdAt = day(2, 21, 30)),
            ExerciseRecordEntity(defId = run, dayEpochDay = today.toEpochDay() - 3, seconds = 1500, meters = 3100, createdAt = day(3, 7, 40)),
            ExerciseRecordEntity(defId = squat, dayEpochDay = today.toEpochDay(), sets = 4, reps = 12, createdAt = day(0, 8, 6)),
            ExerciseRecordEntity(defId = plank, dayEpochDay = today.toEpochDay(), seconds = 45, createdAt = day(0, 12, 30)),
        )
        records.forEach { exerciseDao.upsertRecord(it) }

        return true
    }
}
