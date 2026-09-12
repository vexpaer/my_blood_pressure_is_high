package io.github.vexpaer.mybp.data.sleep

import io.github.vexpaer.mybp.core.sleep.SleepEstimator
import io.github.vexpaer.mybp.core.sleep.SleepNight
import io.github.vexpaer.mybp.data.db.AppDatabase
import io.github.vexpaer.mybp.data.db.SleepNightEntity
import io.github.vexpaer.mybp.data.usage.UsageStatsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/**
 * 睡眠数据的落库与刷新：估算数据只覆盖非手动修改的记录；
 * 手动修改永远优先，估算刷新不会覆盖用户的手工数据。
 */
class SleepRepository(
    db: AppDatabase,
    private val usage: UsageStatsSource,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.sleepNightDao()

    fun hasUsageAccess(): Boolean = usage.hasUsageAccess()

    suspend fun allNights(): List<SleepNight> = dao.getAllOnce().map { it.toDomain() }

    val nights: Flow<List<SleepNight>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    /** 进入页面时调用：估算最近 N 晚并入库（跳过手动修改与无变化的数据）。 */
    suspend fun refreshRecent(days: Int = 7, zone: ZoneId = ZoneId.systemDefault()) {
        if (!usage.hasUsageAccess()) return
        val today = LocalDate.now(zone)
        for (i in 0 until days) {
            val wakeDay = today.minusDays(i.toLong())
            val estimate = SleepEstimator.estimate(usage.nightlyUsage(wakeDay, zone), wakeDay, zone)
                ?: continue
            val existing = dao.get(wakeDay.toEpochDay())
            if (existing?.isManual == true) continue
            if (existing != null &&
                existing.bedtimeEpochMillis == estimate.bedtimeEpochMillis &&
                existing.wakeEpochMillis == estimate.wakeEpochMillis
            ) {
                continue
            }
            dao.upsert(
                SleepNightEntity(
                    wakeDayEpochDay = wakeDay.toEpochDay(),
                    bedtimeEpochMillis = estimate.bedtimeEpochMillis,
                    wakeEpochMillis = estimate.wakeEpochMillis,
                    durationMinutes = estimate.durationMinutes,
                    isManual = false,
                    updatedAtMillis = now(),
                ),
            )
        }
    }

    suspend fun saveManual(wakeDay: LocalDate, bedtimeMillis: Long, wakeMillis: Long) {
        val minutes = ((wakeMillis - bedtimeMillis).coerceAtLeast(0L) / 60_000L).toInt()
        dao.upsert(
            SleepNightEntity(
                wakeDayEpochDay = wakeDay.toEpochDay(),
                bedtimeEpochMillis = bedtimeMillis,
                wakeEpochMillis = wakeMillis,
                durationMinutes = minutes,
                isManual = true,
                updatedAtMillis = now(),
            ),
        )
    }
}

private fun SleepNightEntity.toDomain() = SleepNight(
    wakeDayEpochDay = wakeDayEpochDay,
    bedtimeEpochMillis = bedtimeEpochMillis,
    wakeEpochMillis = wakeEpochMillis,
    durationMinutes = durationMinutes,
    isManual = isManual,
)
