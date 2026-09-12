package io.github.vexpaer.mybp.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 展示格式：时钟、时长、日期。全部随系统本地时区。 */
object TimeFormats {

    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthDayFormatter = DateTimeFormatter.ofPattern("M.d")

    fun clock(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(clockFormatter)

    /** 464 分钟 → "7 h 44 min" */
    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "$h h $m min"
    }

    /** 入睡时间折算为"小时数"，用于趋势图 y 轴（18 → 次日 36）。 */
    fun hourOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Float {
        val t = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val h = t.hour + t.minute / 60f
        return if (h < 12f) h + 24f else h
    }

    fun dateLabel(epochDay: Long, todayEpochDay: Long): String = when (epochDay) {
        todayEpochDay -> "今天"
        todayEpochDay - 1 -> "昨天"
        else -> LocalDate.ofEpochDay(epochDay).format(monthDayFormatter)
    }
}
