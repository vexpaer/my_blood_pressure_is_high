package io.github.vexpaer.mybp.core.sleep

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 根据夜间手机使用情况估算睡眠时间。
 *
 * 算法（v0.1.0，本地、可解释）：
 * 1. 观察窗口 = 起床日前一天 17:00 至起床日 15:00（本地时间）。
 * 2. 使用区间先按 ≤2 分钟的间隔合并；丢弃短于 1 分钟的碎片
 *    （几秒钟的亮屏不算"明显使用"），再合并一次。
 * 3. 只在**相邻两个使用块之间**找"未使用区间"作为睡眠候选；
 *    窗口头尾的开放区间不参与（睡过头/手机没开的情况无法确认起床时间）。
 * 4. 候选需满足：时长 [2h, 13h]，且与核心夜间区间 [前一天 21:00, 当天 10:00]
 *    至少重叠 1 小时。
 * 5. 取时长最长者：起点为估算入睡时间，终点为估算起床时间。
 * 6. 没有候选 → 返回 null，UI 显示"数据不足"，引导手动修正。
 *
 * 结果一律按"估算"呈现，不是医疗监测。
 */
object SleepEstimator {

    /** 半开区间 [start, end)。 */
    data class Interval(val start: Long, val end: Long)

    data class Estimate(
        val bedtimeEpochMillis: Long,
        val wakeEpochMillis: Long,
        val durationMinutes: Int,
    )

    private const val MIN_BLOCK_MS = 60_000L
    private const val MERGE_GAP_MS = 2 * 60_000L
    private const val MIN_SLEEP_MS = 2 * 60 * 60_000L
    private const val MAX_SLEEP_MS = 13 * 60 * 60_000L
    private const val MIN_CORE_OVERLAP_MS = 60 * 60_000L

    fun estimate(usage: List<Interval>, wakeDay: LocalDate, zone: ZoneId): Estimate? {
        if (usage.isEmpty()) return null
        val windowStart = at(wakeDay.minusDays(1), 17, 0, zone)
        val windowEnd = at(wakeDay, 15, 0, zone)
        val coreStart = at(wakeDay.minusDays(1), 21, 0, zone)
        val coreEnd = at(wakeDay, 10, 0, zone)

        val blocks = usage
            .mapNotNull { clip(it, windowStart, windowEnd) }
            .let(::merge)
            .filter { it.end - it.start >= MIN_BLOCK_MS }
            .let(::merge)
        if (blocks.size < 2) return null

        var cursor = blocks.first().end
        val gaps = ArrayList<Interval>(blocks.size - 1)
        for (i in 1 until blocks.size) {
            val block = blocks[i]
            gaps += Interval(cursor, block.start)
            cursor = block.end
        }

        val best = gaps
            .filter { it.end - it.start in MIN_SLEEP_MS..MAX_SLEEP_MS }
            .filter { overlap(it, coreStart, coreEnd) >= MIN_CORE_OVERLAP_MS }
            .maxByOrNull { it.end - it.start } ?: return null

        return Estimate(
            bedtimeEpochMillis = best.start,
            wakeEpochMillis = best.end,
            durationMinutes = ((best.end - best.start) / 60_000L).toInt(),
        )
    }

    private fun merge(intervals: List<Interval>): List<Interval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.start }
        val out = ArrayList<Interval>(sorted.size)
        var cur = sorted.first()
        for (next in sorted.drop(1)) {
            if (next.start <= cur.end + MERGE_GAP_MS) {
                cur = Interval(cur.start, maxOf(cur.end, next.end))
            } else {
                out += cur
                cur = next
            }
        }
        out += cur
        return out
    }

    private fun clip(i: Interval, start: Long, end: Long): Interval? {
        val s = maxOf(i.start, start)
        val e = minOf(i.end, end)
        return if (e > s) Interval(s, e) else null
    }

    private fun overlap(i: Interval, start: Long, end: Long): Long =
        (minOf(i.end, end) - maxOf(i.start, start)).coerceAtLeast(0L)

    private fun at(day: LocalDate, hour: Int, minute: Int, zone: ZoneId): Long =
        ZonedDateTime.of(day, java.time.LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()
}
