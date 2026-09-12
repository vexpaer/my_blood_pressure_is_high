package io.github.vexpaer.mybp.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNamesTest {

    @Test
    fun `默认名称`() {
        assertEquals(listOf("早睡早起", "少吃点盐", "抬腿跑跑", "设置"), TabNames.DEFAULTS)
        assertEquals(TabNames.DEFAULTS, TabNames.resolve(emptyList()))
        assertEquals(TabNames.DEFAULTS, TabNames.resolve(listOf(null, null, null, null)))
    }

    @Test
    fun `修改生效`() {
        val saved = listOf("早点睡", "少放盐", "动一动", "我的")
        assertEquals(saved, TabNames.resolve(saved))
    }

    @Test
    fun `空白回退默认`() {
        assertEquals(
            listOf("早睡早起", "少吃点盐", "抬腿跑跑", "设置"),
            TabNames.resolve(listOf("  ", "", "抬腿跑跑", "设置")),
        )
        assertEquals("早睡早起", TabNames.sanitize(null, "早睡早起"))
    }

    @Test
    fun `超长截断到 6 个字`() {
        assertEquals("一二三四五六", TabNames.sanitize("一二三四五六七", "设置"))
    }

    @Test
    fun `压缩连续空白并去首尾`() {
        assertEquals("早 睡", TabNames.sanitize("  早   睡  ", "设置"))
    }

    @Test
    fun `恢复默认判断`() {
        assertTrue(TabNames.areDefaults(emptyList()))
        assertTrue(TabNames.areDefaults(TabNames.DEFAULTS))
        assertFalse(TabNames.areDefaults(listOf("早点睡", "少吃点盐", "抬腿跑跑", "设置")))
    }

    @Test
    fun `缺失项自动补默认`() {
        assertEquals(
            listOf("早点睡", "少吃点盐", "抬腿跑跑", "设置"),
            TabNames.resolve(listOf("早点睡")),
        )
    }
}
