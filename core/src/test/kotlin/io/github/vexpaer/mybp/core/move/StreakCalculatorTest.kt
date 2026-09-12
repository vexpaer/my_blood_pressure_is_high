package io.github.vexpaer.mybp.core.move

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = 20_000L

    @Test
    fun `空记录为 0`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `今天动了 - 连续一天`() {
        assertEquals(1, StreakCalculator.currentStreak(setOf(today), today))
    }

    @Test
    fun `今天还没动 - 到昨天为止的连续链保留`() {
        assertEquals(1, StreakCalculator.currentStreak(setOf(today - 1), today))
        assertEquals(3, StreakCalculator.currentStreak(setOf(today - 3, today - 2, today - 1), today))
    }

    @Test
    fun `链条断了归零`() {
        assertEquals(0, StreakCalculator.currentStreak(setOf(today - 2), today))
        assertEquals(0, StreakCalculator.currentStreak(setOf(today - 5, today - 3), today))
    }

    @Test
    fun `久远的记录不影响最近的连续链`() {
        // 今天没动，昨天与前天连续 → 2 天；上周的记录不算进去
        assertEquals(2, StreakCalculator.currentStreak(setOf(today - 5, today - 2, today - 1), today))
    }

    @Test
    fun `今天接上 - 完整链条计数`() {
        assertEquals(4, StreakCalculator.currentStreak(setOf(today - 3, today - 2, today - 1, today), today))
    }

    @Test
    fun `中间有洞只算最近一段`() {
        assertEquals(2, StreakCalculator.currentStreak(setOf(today - 5, today - 1, today), today))
    }

    @Test
    fun `最近 7 天计数`() {
        val days = setOf(today - 6, today - 3, today, today + 1) // 今天+1 不算
        assertEquals(3, StreakCalculator.last7DaysCount(days, today))
    }
}
