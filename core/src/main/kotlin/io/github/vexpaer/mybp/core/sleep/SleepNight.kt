package io.github.vexpaer.mybp.core.sleep

/**
 * 一晚睡眠数据（按"起床日"为键，例如 9 月 12 日早上醒来 = 9 月 12 日这一晚）。
 */
data class SleepNight(
    val wakeDayEpochDay: Long,
    val bedtimeEpochMillis: Long,
    val wakeEpochMillis: Long,
    val durationMinutes: Int,
    val isManual: Boolean,
)

/**
 * 合并估算与手动修正：同一晚手动修改的数据永远优先于估算。
 */
object SleepNightMerger {

    fun merge(estimated: List<SleepNight>, manual: List<SleepNight>): List<SleepNight> {
        val manualDays = manual.mapTo(HashSet()) { it.wakeDayEpochDay }
        return (manual + estimated.filter { it.wakeDayEpochDay !in manualDays })
            .sortedByDescending { it.wakeDayEpochDay }
    }
}
