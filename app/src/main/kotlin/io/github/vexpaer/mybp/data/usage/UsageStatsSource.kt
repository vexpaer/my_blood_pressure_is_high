package io.github.vexpaer.mybp.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import io.github.vexpaer.mybp.core.sleep.SleepEstimator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 读取系统「使用情况」事件，还原一天里的前台使用区间。
 * 权限（PACKAGE_USAGE_STATS）由「早睡早起」页面按需引导开启，这里只做只读查询。
 */
class UsageStatsSource(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        // OP_GET_USAGE_STATS 常量是隐藏 API，这里用稳定的 op 名称字符串
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(OP_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(OP_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private companion object {
        const val OP_GET_USAGE_STATS = "android:get_usage_stats"
    }

    /**
     * 某个「起床日」对应的观察窗口（前一天 17:00 → 当天 15:00）内的前台使用区间。
     * 没有权限或读取失败时返回空列表（= 数据不足，不猜测）。
     */
    fun nightlyUsage(wakeDay: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<SleepEstimator.Interval> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val start = at(wakeDay.minusDays(1), LocalTime.of(17, 0), zone)
        val end = at(wakeDay, LocalTime.of(15, 0), zone)
        if (end <= start) return emptyList()

        val events = usm.queryEvents(start, end)
        val intervals = ArrayList<SleepEstimator.Interval>()
        var openStart: Long? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (openStart == null) openStart = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    openStart?.let { s ->
                        if (event.timeStamp > s) intervals += SleepEstimator.Interval(s, event.timeStamp)
                    }
                    openStart = null
                }
            }
        }
        // 窗口结束时仍在前台（比如用户此刻正拿着手机）：补一个到窗口尾的区间
        openStart?.let { if (end > it) intervals += SleepEstimator.Interval(it, end) }
        return intervals
    }

    private fun at(day: LocalDate, time: LocalTime, zone: ZoneId): Long =
        ZonedDateTime.of(day, time, zone).toInstant().toEpochMilli()
}
