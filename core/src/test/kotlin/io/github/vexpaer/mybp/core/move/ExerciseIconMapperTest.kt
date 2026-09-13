package io.github.vexpaer.mybp.core.move

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseIconMapperTest {

    @Test
    fun `中文关键词映射`() {
        assertEquals(ExerciseIcon.RUNNING, ExerciseIconMapper.map("晨跑"))
        assertEquals(ExerciseIcon.RUNNING, ExerciseIconMapper.map("跑步 3km"))
        assertEquals(ExerciseIcon.WALKING, ExerciseIconMapper.map("晚饭后散步"))
        assertEquals(ExerciseIcon.CYCLING, ExerciseIconMapper.map("骑行通勤"))
        assertEquals(ExerciseIcon.DUMBBELL, ExerciseIconMapper.map("哑铃划船"))
        assertEquals(ExerciseIcon.DUMBBELL, ExerciseIconMapper.map("锤式弯举"))
        assertEquals(ExerciseIcon.TIMER, ExerciseIconMapper.map("平板支撑"))
        assertEquals(ExerciseIcon.BODYWEIGHT, ExerciseIconMapper.map("俯卧撑"))
        assertEquals(ExerciseIcon.LEGS, ExerciseIconMapper.map("深蹲"))
        assertEquals(ExerciseIcon.LEGS, ExerciseIconMapper.map("保加利亚箭步蹲"))
    }

    @Test
    fun `英文关键词不区分大小写`() {
        assertEquals(ExerciseIcon.RUNNING, ExerciseIconMapper.map("Morning Jog"))
        assertEquals(ExerciseIcon.DUMBBELL, ExerciseIconMapper.map("Dumbbell Row"))
        assertEquals(ExerciseIcon.BODYWEIGHT, ExerciseIconMapper.map("Push-ups"))
    }

    @Test
    fun `识别失败回落 GENERIC 且不报错`() {
        assertEquals(ExerciseIcon.GENERIC, ExerciseIconMapper.map("奇怪的操"))
        assertEquals(ExerciseIcon.GENERIC, ExerciseIconMapper.map(""))
        assertEquals(ExerciseIcon.GENERIC, ExerciseIconMapper.map("🎉🎉🎉"))
    }

    @Test
    fun `更具体的类别优先 - 弓步蹲归腿部而非跑`() {
        assertEquals(ExerciseIcon.LEGS, ExerciseIconMapper.map("弓步走"))
    }
}
