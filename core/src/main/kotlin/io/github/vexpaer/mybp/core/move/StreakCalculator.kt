package io.github.vexpaer.mybp.core.move

/**
 * 连续运动天数：优先以今天结尾；今天还没动时，到昨天为止的连续链仍然保留
 * （今天动了会接上，不动也不清零昨天以前的努力）。
 */
object StreakCalculator {

    fun currentStreak(daysWithRecords: Set<Long>, today: Long): Int {
        var cursor = if (today in daysWithRecords) today else today - 1
        var streak = 0
        while (cursor in daysWithRecords) {
            streak++
            cursor--
        }
        return streak
    }

    fun last7DaysCount(daysWithRecords: Set<Long>, today: Long): Int =
        (today - 6..today).count { it in daysWithRecords }
}
