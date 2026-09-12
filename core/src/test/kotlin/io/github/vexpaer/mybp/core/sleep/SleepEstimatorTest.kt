package io.github.vexpaer.mybp.core.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepEstimatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val wakeDay: LocalDate = LocalDate.of(2026, 9, 12)

    private fun ms(day: LocalDate, hour: Int, minute: Int): Long =
        ZonedDateTime.of(day, LocalDateTime.of(day.year, 1, 1, hour, minute).toLocalTime(), zone)
            .toInstant().toEpochMilli()

    private fun ms(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private val yesterday: LocalDate get() = wakeDay.minusDays(1)

    private fun interval(y: Int, mo: Int, d: Int, sh: Int, sm: Int, eh: Int, em: Int) =
        SleepEstimator.Interval(ms(y, mo, d, sh, sm), ms(y, mo, d, eh, em))

    @Test
    fun `正常夜间区间 - 规格示例 23_47 入睡 07_31 起床`() {
        // 前一天 22:00-23:47 使用手机，当天 07:31 再次使用
        val usage = listOf(
            interval(2026, 9, 11, 22, 0, 23, 47),
            interval(2026, 9, 12, 7, 31, 7, 45),
        )
        val e = SleepEstimator.estimate(usage, wakeDay, zone)!!
        assertEquals(ms(2026, 9, 11, 23, 47), e.bedtimeEpochMillis)
        assertEquals(ms(2026, 9, 12, 7, 31), e.wakeEpochMillis)
        assertEquals(464, e.durationMinutes) // 7 h 44 min
    }

    @Test
    fun `跨午夜 - 入睡与起床分属两天`() {
        val usage = listOf(
            interval(2026, 9, 11, 23, 40, 23, 47),
            interval(2026, 9, 12, 7, 31, 7, 40),
        )
        val e = SleepEstimator.estimate(usage, wakeDay, zone)!!
        assertEquals(yesterday.atTime(23, 47), LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(e.bedtimeEpochMillis), zone))
        assertEquals(wakeDay.atTime(7, 31), LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(e.wakeEpochMillis), zone))
    }

    @Test
    fun `夜间继续使用手机 - 睡眠按最长未使用区间估算`() {
        val usage = listOf(
            interval(2026, 9, 11, 22, 0, 23, 40),
            interval(2026, 9, 12, 1, 0, 1, 40), // 起夜玩了 40 分钟（超过碎片阈值）
            interval(2026, 9, 12, 7, 30, 7, 45),
        )
        val e = SleepEstimator.estimate(usage, wakeDay, zone)!!
        assertEquals(ms(2026, 9, 12, 1, 40), e.bedtimeEpochMillis)
        assertEquals(ms(2026, 9, 12, 7, 30), e.wakeEpochMillis)
        assertEquals(350, e.durationMinutes)
    }

    @Test
    fun `夜间短暂亮屏 - 少于 1 分钟的碎片不算明显使用`() {
        val usage = listOf(
            interval(2026, 9, 11, 22, 0, 23, 47),
            interval(2026, 9, 12, 3, 10, 3, 10) // 只亮屏几秒
                .let { SleepEstimator.Interval(ms(2026, 9, 12, 3, 10), ms(2026, 9, 12, 3, 10) + 20_000L) },
            interval(2026, 9, 12, 7, 31, 7, 40),
        )
        val e = SleepEstimator.estimate(usage, wakeDay, zone)!!
        assertEquals(ms(2026, 9, 11, 23, 47), e.bedtimeEpochMillis)
        assertEquals(ms(2026, 9, 12, 7, 31), e.wakeEpochMillis)
    }

    @Test
    fun `数据不足 - 全天零散使用没有足够长的未使用区间`() {
        val usage = listOf(
            interval(2026, 9, 11, 18, 0, 20, 0),
            interval(2026, 9, 11, 21, 0, 23, 0),
            interval(2026, 9, 12, 0, 0, 2, 0),
            interval(2026, 9, 12, 3, 0, 5, 0),
            interval(2026, 9, 12, 6, 0, 8, 0),
        )
        assertNull(SleepEstimator.estimate(usage, wakeDay, zone))
    }

    @Test
    fun `数据不足 - 起床时间无法确认（窗口尾部开放）`() {
        // 只有一次凌晨前的使用记录，之后直到窗口结束都没有使用
        val usage = listOf(
            interval(2026, 9, 11, 19, 0, 19, 30),
            interval(2026, 9, 11, 20, 0, 20, 30),
        )
        assertNull(SleepEstimator.estimate(usage, wakeDay, zone))
    }

    @Test
    fun `空数据返回 null`() {
        assertNull(SleepEstimator.estimate(emptyList(), wakeDay, zone))
    }

    @Test
    fun `头部区间过短不误判 - 傍晚少量使用后早睡`() {
        val usage = listOf(
            interval(2026, 9, 11, 17, 30, 17, 45),
            interval(2026, 9, 11, 21, 30, 21, 45),
            interval(2026, 9, 12, 6, 30, 6, 50),
            interval(2026, 9, 12, 9, 0, 9, 10),
        )
        val e = SleepEstimator.estimate(usage, wakeDay, zone)!!
        // 候选：21:45→06:30（8h45m，核心夜间区间全覆盖）vs 06:50→09:00（2h10m）
        assertEquals(ms(2026, 9, 11, 21, 45), e.bedtimeEpochMillis)
        assertEquals(ms(2026, 9, 12, 6, 30), e.wakeEpochMillis)
        assertEquals(525, e.durationMinutes)
    }
}

class SleepNightMergerTest {

    private fun night(day: Long, manual: Boolean) =
        SleepNight(day, bedtimeEpochMillis = 0, wakeEpochMillis = 0, durationMinutes = 400, isManual = manual)

    @Test
    fun `手动修改优先于估算`() {
        val estimated = listOf(night(100, manual = false), night(101, manual = false))
        val manual = listOf(night(100, manual = true))
        val merged = SleepNightMerger.merge(estimated, manual)
        assertEquals(2, merged.size)
        assertEquals(101L, merged[0].wakeDayEpochDay)
        assertEquals(100L, merged[1].wakeDayEpochDay)
        assertEquals(true, merged[1].isManual)
    }

    @Test
    fun `按起床日倒序排列`() {
        val merged = SleepNightMerger.merge(
            listOf(night(98, manual = false), night(100, manual = false)),
            listOf(night(99, manual = true)),
        )
        assertEquals(listOf(100L, 99L, 98L), merged.map { it.wakeDayEpochDay })
    }
}
