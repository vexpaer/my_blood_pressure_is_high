package io.github.vexpaer.mybp.core.move

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseInputTest {

    @Test
    fun `组×次 - 正常输入`() {
        val r = ExerciseInput.parse(ExerciseMode.SETS_REPS, "3", "12", null, null, null)
        assertNotNull(r.values)
        assertEquals("3 × 12", r.values!!.summary(ExerciseMode.SETS_REPS))
    }

    @Test
    fun `组×次 - 缺组数报错`() {
        val r = ExerciseInput.parse(ExerciseMode.SETS_REPS, "", "12", null, null, null)
        assertNull(r.values)
        assertEquals("组数", r.errorField)
    }

    @Test
    fun `次数模式`() {
        val r = ExerciseInput.parse(ExerciseMode.REPS, null, "20", null, null, null)
        assertEquals("20 次", r.values!!.summary(ExerciseMode.REPS))
    }

    @Test
    fun `时间模式 - 秒与分钟`() {
        assertEquals("45 s", ExerciseValues(seconds = 45).summary(ExerciseMode.TIME))
        assertEquals("17 min", ExerciseValues(seconds = 17 * 60).summary(ExerciseMode.TIME))
        assertEquals("1 h 5 min", ExerciseValues(seconds = 3900).summary(ExerciseMode.TIME))
    }

    @Test
    fun `距离模式 - 米与公里`() {
        val r = ExerciseInput.parse(ExerciseMode.DISTANCE, null, null, "1020", "2400", null)
        assertEquals("2.40 km · 17 min", r.values!!.summary(ExerciseMode.DISTANCE))
        assertEquals("800 m", ExerciseValues(meters = 800).summary(ExerciseMode.DISTANCE))
    }

    @Test
    fun `距离模式 - 时间可选`() {
        val r = ExerciseInput.parse(ExerciseMode.DISTANCE, null, null, null, "1000", null)
        assertEquals("1.00 km", r.values!!.summary(ExerciseMode.DISTANCE))
    }

    @Test
    fun `重量+组×次 - 正常与非法`() {
        val ok = ExerciseInput.parse(ExerciseMode.WEIGHT_SETS_REPS, "4", "8", null, null, "12.5")
        assertEquals("12.5 kg · 4 × 8", ok.values!!.summary(ExerciseMode.WEIGHT_SETS_REPS))
        assertEquals("12 kg", ExerciseValues.formatKg(12.0))
        val bad = ExerciseInput.parse(ExerciseMode.WEIGHT_SETS_REPS, "4", "8", null, null, "0")
        assertNull(bad.values)
        assertEquals("重量", bad.errorField)
    }

    @Test
    fun `极端输入 - 负数 科学计数 文本 不崩溃`() {
        assertNull(ExerciseInput.parse(ExerciseMode.REPS, null, "-5", null, null, null).values)
        assertNull(ExerciseInput.parse(ExerciseMode.REPS, null, "1e20", null, null, null).values)
        assertNull(ExerciseInput.parse(ExerciseMode.REPS, null, "abc", null, null, null).values)
        assertNull(ExerciseInput.parse(ExerciseMode.REPS, null, null, null, null, null).values)
    }

    @Test
    fun `小数重量保留一位`() {
        assertEquals("12.3 kg", ExerciseValues.formatKg(12.345678))
    }
}
