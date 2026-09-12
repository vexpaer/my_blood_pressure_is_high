package io.github.vexpaer.mybp.core.move

/** 运动记录模式：次数 / 组×次 / 时间 / 距离 / 重量+组×次。不写死运动种类。 */
enum class ExerciseMode(val label: String) {
    REPS("次数"),
    SETS_REPS("组 × 次"),
    TIME("时间"),
    DISTANCE("距离"),
    WEIGHT_SETS_REPS("重量 + 组 × 次"),
}

/** 一次运动记录的数值字段（按 mode 选用，未用的为 null）。 */
data class ExerciseValues(
    val sets: Int? = null,
    val reps: Int? = null,
    val seconds: Long? = null,
    val meters: Int? = null,
    val kilograms: Double? = null,
) {
    /** 一行可读摘要，如 "3 × 12"、"2.4 km · 17 min"、"3 × 45 s"。 */
    fun summary(mode: ExerciseMode): String = when (mode) {
        ExerciseMode.REPS -> "${reps ?: 0} 次"
        ExerciseMode.SETS_REPS -> "${sets ?: 0} × ${reps ?: 0}"
        ExerciseMode.TIME -> formatSeconds(seconds ?: 0)
        ExerciseMode.DISTANCE -> buildString {
            append(formatMeters(meters ?: 0))
            seconds?.takeIf { it > 0 }?.let { append(" · "); append(formatSeconds(it)) }
        }
        ExerciseMode.WEIGHT_SETS_REPS -> buildString {
            kilograms?.takeIf { it > 0 }?.let { append(formatKg(it)); append(" · ") }
            append("${sets ?: 0} × ${reps ?: 0}")
        }
    }

    companion object {
        fun formatSeconds(total: Long): String = when {
            total >= 3600 -> {
                val h = total / 3600
                val m = (total % 3600) / 60
                if (m > 0) "$h h $m min" else "$h h"
            }
            total >= 60 -> "${total / 60} min"
            else -> "$total s"
        }

        fun formatMeters(meters: Int): String =
            if (meters >= 1000) String.format("%.2f km", meters / 1000.0) else "$meters m"

        fun formatKg(kg: Double): String =
            if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else String.format("%.1f kg", kg)
    }
}

/**
 * 输入校验：所有需要字段必须为正数。
 * 返回解析后的值；错误时返回 null 并给出错误字段名。
 */
object ExerciseInput {

    data class Result(val values: ExerciseValues?, val errorField: String?)

    fun parse(
        mode: ExerciseMode,
        setsText: String?,
        repsText: String?,
        secondsText: String?,
        metersText: String?,
        kilogramsText: String?,
    ): Result {
        val sets = intOf(setsText)
        val reps = intOf(repsText)
        val seconds = longOf(secondsText)
        val meters = intOf(metersText)
        val kilograms = doubleOf(kilogramsText)

        fun fail(field: String) = Result(null, field)

        return when (mode) {
            ExerciseMode.REPS -> {
                if (reps == null || reps <= 0) fail("次数") else Result(ExerciseValues(reps = reps), null)
            }
            ExerciseMode.SETS_REPS -> when {
                sets == null || sets <= 0 -> fail("组数")
                reps == null || reps <= 0 -> fail("次数")
                else -> Result(ExerciseValues(sets = sets, reps = reps), null)
            }
            ExerciseMode.TIME -> {
                if (seconds == null || seconds <= 0) fail("时间") else Result(ExerciseValues(seconds = seconds), null)
            }
            ExerciseMode.DISTANCE -> when {
                meters == null || meters <= 0 -> fail("距离")
                seconds != null && seconds <= 0 -> fail("时间")
                else -> Result(ExerciseValues(seconds = seconds, meters = meters), null)
            }
            ExerciseMode.WEIGHT_SETS_REPS -> when {
                kilograms == null || kilograms <= 0 -> fail("重量")
                sets == null || sets <= 0 -> fail("组数")
                reps == null || reps <= 0 -> fail("次数")
                else -> Result(ExerciseValues(sets = sets, reps = reps, kilograms = kilograms), null)
            }
        }
    }

    private fun intOf(text: String?): Int? =
        text?.trim()?.toDoubleOrNull()?.takeIf { it > 0 && it <= Int.MAX_VALUE }?.toInt()

    private fun longOf(text: String?): Long? =
        text?.trim()?.toDoubleOrNull()?.takeIf { it > 0 && it <= Long.MAX_VALUE }?.toLong()

    private fun doubleOf(text: String?): Double? =
        text?.trim()?.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
}
