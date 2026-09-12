package io.github.vexpaer.mybp.core.export

import io.github.vexpaer.mybp.core.sleep.SleepNight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream

class ExportBuilderTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()

    private fun night(day: Long, manual: Boolean) = SleepNight(
        wakeDayEpochDay = day,
        bedtimeEpochMillis = millis(2026, 9, 10, 23, 48),
        wakeEpochMillis = millis(2026, 9, 11, 7, 31),
        durationMinutes = 463,
        isManual = manual,
    )

    @Test
    fun `sleep csv - 表头与示例行`() {
        val csv = ExportBuilder.sleepCsv(listOf(night(LocalDate.of(2026, 9, 11).toEpochDay(), manual = false)), zone)
        val lines = csv.removePrefix("\uFEFF").trim().lines()
        assertEquals("date,bedtime,wake_time,duration_minutes,source", lines[0])
        assertEquals("2026-09-11,23:48,07:31,463,estimated", lines[1])
    }

    @Test
    fun `sleep csv - manual 标记与 UTF-8 BOM`() {
        val csv = ExportBuilder.sleepCsv(listOf(night(1, manual = true)), zone)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.lines()[1].endsWith(",manual"))
    }

    @Test
    fun `exercise csv - 五种模式与空字段留空`() {
        val d11 = LocalDate.of(2026, 9, 11).toEpochDay()
        val d12 = LocalDate.of(2026, 9, 12).toEpochDay()
        val rows = listOf(
            ExportBuilder.ExerciseRow("深蹲", "SETS_REPS", d11, millis(2026, 9, 11, 8, 5), sets = 3, reps = 12),
            ExportBuilder.ExerciseRow("跑步", "DISTANCE", d11, millis(2026, 9, 11, 19, 30), seconds = 1020, meters = 2400),
            ExportBuilder.ExerciseRow("平板支撑", "TIME", d12, millis(2026, 9, 12, 7, 0), seconds = 45),
            ExportBuilder.ExerciseRow("俯卧撑", "REPS", d12, millis(2026, 9, 12, 7, 10), reps = 20),
            ExportBuilder.ExerciseRow("卧推", "WEIGHT_SETS_REPS", d12, millis(2026, 9, 12, 7, 20), sets = 4, reps = 8, kilograms = 52.5),
        )
        val lines = ExportBuilder.exerciseCsv(rows, zone).removePrefix("\uFEFF").trim().lines()
        assertEquals("date,time,exercise,mode,sets,reps,weight_kg,distance_m,duration_s", lines[0])
        assertEquals("2026-09-11,08:05,深蹲,SETS_REPS,3,12,,,", lines[1])
        assertEquals("2026-09-11,19:30,跑步,DISTANCE,,,,2400,1020", lines[2])
        assertEquals("2026-09-12,07:00,平板支撑,TIME,,,,,45", lines[3])
        assertEquals("2026-09-12,07:10,俯卧撑,REPS,,20,,,,", lines[4])
        assertEquals("2026-09-12,07:20,卧推,WEIGHT_SETS_REPS,4,8,52.5,,", lines[5])
    }

    @Test
    fun `csv escaping - 逗号 引号 换行`() {
        assertEquals("plain", ExportBuilder.escape("plain"))
        assertEquals("\"a,b\"", ExportBuilder.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ExportBuilder.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", ExportBuilder.escape("line1\nline2"))
        val row = ExportBuilder.ExerciseRow("波比跳, 进阶", "REPS", 0, 0, reps = 10)
        assertTrue(ExportBuilder.exerciseCsv(listOf(row), zone).contains("\"波比跳, 进阶\",REPS"))
    }

    @Test
    fun `zip - 四个文件齐全且内容可读`() {
        val bytes = ExportBuilder.buildZip(
            sleep = listOf(night(0, manual = false)),
            exercise = listOf(ExportBuilder.ExerciseRow("深蹲", "SETS_REPS", 0, 0, sets = 3, reps = 12)),
            settings = ExportBuilder.SettingsSnapshot(
                tabNames = listOf("早睡早起", "少吃点盐", "抬腿跑跑", "设置"),
                theme = "SYSTEM",
                appVersion = "0.1.1",
                exportedAt = Instant.parse("2026-09-12T15:00:00Z"),
                zone = zone,
            ),
        )
        val names = ArrayList<String>()
        val contents = HashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                names += entry.name
                contents[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }
        assertEquals(listOf("sleep.csv", "exercise.csv", "settings.json", "README.txt"), names)
        assertTrue(contents["sleep.csv"]!!.contains("estimated"))
        assertTrue(contents["exercise.csv"]!!.contains("深蹲"))
        assertTrue(contents["README.txt"]!!.contains("estimated"))
    }

    @Test
    fun `settings json - schema version 与中文名正常`() {
        val json = ExportBuilder.settingsJson(
            ExportBuilder.SettingsSnapshot(
                tabNames = listOf("早点睡", "少放盐", "动一动", "我的"),
                theme = "DARK",
                appVersion = "0.1.1",
                exportedAt = Instant.parse("2026-09-12T15:00:00Z"),
                zone = zone,
            ),
        )
        assertTrue(json.contains("\"schema_version\": 1"))
        assertTrue(json.contains("\"sleep\": \"早点睡\""))
        assertTrue(json.contains("\"theme\": \"DARK\""))
        assertTrue(json.contains("\"exported_at\": \"2026-09-12T15:00:00Z\""))
    }

    @Test
    fun `settings json - 引号转义`() {
        val json = ExportBuilder.settingsJson(
            ExportBuilder.SettingsSnapshot(
                tabNames = listOf("说\"早安\"", "少吃点盐", "抬腿跑跑", "设置"),
                theme = "SYSTEM",
                appVersion = "0.1.1",
                exportedAt = Instant.EPOCH,
                zone = zone,
            ),
        )
        assertTrue(json.contains("\"sleep\": \"说\\\"早安\\\"\""))
    }

    @Test
    fun `导出内容不包含任何 API Key 痕迹`() {
        val all = ExportBuilder.buildZip(
            sleep = emptyList(),
            exercise = emptyList(),
            settings = ExportBuilder.SettingsSnapshot(listOf("a", "b", "c", "d"), "SYSTEM", "0.1.1", Instant.EPOCH, zone),
        ).toString(Charsets.UTF_8)
        assertFalse(all.contains("AMAP", ignoreCase = true))
        assertFalse(all.contains("apikey", ignoreCase = true))
        assertFalse(all.contains("key\"", ignoreCase = true))
    }
}
