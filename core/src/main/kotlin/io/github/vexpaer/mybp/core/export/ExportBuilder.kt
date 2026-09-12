package io.github.vexpaer.mybp.core.export

import io.github.vexpaer.mybp.core.sleep.SleepNight
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 本地数据导出（Paper Minimal 2.0「数据与备份」）：
 * sleep.csv / exercise.csv / settings.json / README.txt 打成一个 ZIP。
 * 纯 JVM、可测试；CSV 全部 UTF-8（带 BOM，方便 Excel 直接打开中文），
 * 缺失字段留空而不是用 0 冒充。
 */
object ExportBuilder {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

    data class ExerciseRow(
        val name: String,
        val mode: String,
        val dayEpochDay: Long,
        val createdAt: Long,
        val sets: Int? = null,
        val reps: Int? = null,
        val seconds: Long? = null,
        val meters: Int? = null,
        val kilograms: Double? = null,
    )

    data class SettingsSnapshot(
        val tabNames: List<String>,
        val theme: String,
        val appVersion: String,
        val exportedAt: Instant,
        val zone: ZoneId,
    )

    fun buildZip(sleep: List<SleepNight>, exercise: List<ExerciseRow>, settings: SettingsSnapshot): ByteArray {
        val entries = linkedMapOf(
            "sleep.csv" to sleepCsv(sleep, settings.zone).toByteArray(Charsets.UTF_8),
            "exercise.csv" to exerciseCsv(exercise, settings.zone).toByteArray(Charsets.UTF_8),
            "settings.json" to settingsJson(settings).toByteArray(Charsets.UTF_8),
            "README.txt" to readmeText().toByteArray(Charsets.UTF_8),
        )
        return zip(entries)
    }

    fun sleepCsv(nights: List<SleepNight>, zone: ZoneId): String = buildString {
        append(BOM)
        appendLine("date,bedtime,wake_time,duration_minutes,source")
        nights
            .sortedBy { it.wakeDayEpochDay }
            .forEach { night ->
                val date = LocalDate.ofEpochDay(night.wakeDayEpochDay).format(dateFormat)
                val bed = clock(night.bedtimeEpochMillis, zone)
                val wake = clock(night.wakeEpochMillis, zone)
                val source = if (night.isManual) "manual" else "estimated"
                appendLine("$date,$bed,$wake,${night.durationMinutes},$source")
            }
    }

    fun exerciseCsv(rows: List<ExerciseRow>, zone: ZoneId): String = buildString {
        append(BOM)
        appendLine("date,time,exercise,mode,sets,reps,weight_kg,distance_m,duration_s")
        rows
            .sortedWith(compareBy({ it.dayEpochDay }, { it.createdAt }))
            .forEach { row ->
                val date = LocalDate.ofEpochDay(row.dayEpochDay).format(dateFormat)
                val time = clock(row.createdAt, zone)
                appendLine(
                    listOf(
                        date,
                        time,
                        escape(row.name),
                        row.mode,
                        row.sets?.toString().orEmpty(),
                        row.reps?.toString().orEmpty(),
                        row.kilograms?.toString().orEmpty(),
                        row.meters?.toString().orEmpty(),
                        row.seconds?.toString().orEmpty(),
                    ).joinToString(","),
                )
            }
    }

    fun settingsJson(settings: SettingsSnapshot): String = buildString {
        append("{\n")
        append("  \"schema_version\": 1,\n")
        append("  \"app_version\": \"${jsonEscape(settings.appVersion)}\",\n")
        append("  \"exported_at\": \"${jsonEscape(settings.exportedAt.toString())}\",\n")
        append("  \"tab_names\": {\n")
        val keys = listOf("sleep", "salt", "move", "settings")
        keys.forEachIndexed { i, key ->
            val value = settings.tabNames.getOrElse(i) { "" }
            append("    \"$key\": \"${jsonEscape(value)}\"")
            append(if (i == keys.lastIndex) "\n" else ",\n")
        }
        append("  },\n")
        append("  \"theme\": \"${jsonEscape(settings.theme)}\"\n")
        append("}")
    }

    fun readmeText(): String = """
        |这是「我有高血压」App 的本地数据导出。
        |
        |包含：
        |- sleep.csv    睡眠估算结果（不是完整的手机使用记录）
        |- exercise.csv 运动记录
        |- settings.json App 设置（含 schema_version，便于未来导入）
        |
        |CSV 为 UTF-8 编码（带 BOM，Excel 可直接打开中文）。
        |source 字段：estimated = 根据手机使用情况估算；manual = 用户手动修改。
        |缺失的数据留空，不用 0 冒充。
        |
        |这些数据只属于你。App 本身没有任何上传功能。
    """.trimMargin()

    /** CSV 字段转义：包含逗号/引号/换行时加引号，引号翻倍。 */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    private fun jsonEscape(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    private fun clock(epochMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(clockFormat)

    private const val BOM = "\uFEFF"

    fun zip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
