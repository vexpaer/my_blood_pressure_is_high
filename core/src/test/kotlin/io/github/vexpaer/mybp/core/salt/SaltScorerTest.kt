package io.github.vexpaer.mybp.core.salt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaltScorerTest {

    @Test
    fun `川菜馆按类型给分 - 不一刀切但分档为谨慎`() {
        val v = SaltScorer.score("", "餐饮服务;中餐厅;四川菜馆")
        assertEquals(4.2, v.score, 1e-9)
        assertEquals(SaltScorer.Band.NEUTRAL, v.band)
        assertEquals("川湘菜", v.categoryLabel)
        assertTrue(v.recommend.isNotEmpty()) // 川菜也有能点的
        assertTrue(v.caution.isNotEmpty())
    }

    @Test
    fun `类型优先于店名 - 中餐厅里的清蒸海鲜坊得到加分`() {
        val v = SaltScorer.score("清蒸海鲜坊", "餐饮服务;中餐厅")
        assertEquals(5.8 + 0.8, v.score, 1e-9) // 中餐基线 + 清蒸加成
        assertTrue(v.reasons.any { it.id == "steam" })
    }

    @Test
    fun `没有类型时按店名识别 - 轻食沙拉`() {
        val v = SaltScorer.score("沙绿轻食", "")
        assertEquals(8.6, v.score, 1e-9)
        assertEquals(SaltScorer.Band.FRIENDLY, v.band)
        assertEquals("轻食沙拉", v.categoryLabel)
    }

    @Test
    fun `麻辣烫给出谨慎档`() {
        val v = SaltScorer.score("张亮麻辣烫", "餐饮服务;中餐厅;麻辣烫")
        assertEquals(3.4, v.score, 1e-9)
        assertEquals(SaltScorer.Band.CAUTION, v.band)
    }

    @Test
    fun `名称加成可以叠加并封顶 9_8`() {
        val v = SaltScorer.score("清蒸白灼轻食", "轻食")
        // 8.6 + 0.8 + 0.7 = 10.1 → 封顶 9.8
        assertEquals(9.8, v.score, 1e-9)
    }

    @Test
    fun `相同关键词只计一次`() {
        val v1 = SaltScorer.score("清蒸清蒸白灼", "")
        val v2 = SaltScorer.score("清蒸白灼", "")
        assertEquals(v2.score, v1.score, 1e-9)
    }

    @Test
    fun `减分规则生效 - 腊味`() {
        val v = SaltScorer.score("老腊味饭店", "中餐")
        assertEquals(5.8 - 0.7, v.score, 1e-9)
        assertTrue(v.reasons.any { it.id == "cured" })
    }

    @Test
    fun `没有 POI 信息 - 回退到常见中餐估算`() {
        val v = SaltScorer.score("", "")
        assertEquals(5.5, v.score, 1e-9)
        assertEquals(SaltScorer.Band.NEUTRAL, v.band)
        assertEquals("未识别餐厅类型", v.categoryLabel)
        assertTrue(v.reasons.any { it.id == "base:unknown" })
    }

    @Test
    fun `极端输入不崩溃 - 超长与特殊字符`() {
        val v = SaltScorer.score("丨".repeat(500) + "火锅锅锅锅", ";;;!!!")
        assertEquals(SaltScorer.Band.CAUTION, v.band)
        assertTrue(v.score in 1.0..9.8)
    }

    @Test
    fun `分数保留一位小数并落在 1_0 到 9_8`() {
        for (base in listOf("粥", "轻食", "火锅", "烧烤", "日料")) {
            val v = SaltScorer.score(base, base)
            assertEquals(v.score, kotlin.math.round(v.score * 10) / 10.0, 1e-9)
            assertTrue(v.score in 1.0..9.8)
        }
    }

    @Test
    fun `每条结论都带点餐建议`() {
        val v = SaltScorer.score("", "火锅")
        assertEquals("少盐、少酱油、酱汁分开、少喝汤。", v.tip)
    }
}
