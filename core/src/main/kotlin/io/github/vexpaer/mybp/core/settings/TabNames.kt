package io.github.vexpaer.mybp.core.settings

/**
 * 四个底部 Tab 的显示名称规则：可改、可恢复默认。
 * 名称去首尾空白、压缩连续空白、最长 6 个字；空白回退到默认。
 */
object TabNames {
    const val MAX_LENGTH = 6
    val DEFAULTS = listOf("早睡早起", "少吃点盐", "抬腿跑跑", "设置")

    fun sanitize(raw: String?, fallback: String): String {
        val cleaned = raw?.trim()?.replace(Regex("\\s+"), " ")?.take(MAX_LENGTH).orEmpty()
        return cleaned.ifEmpty { fallback }
    }

    /** saved 与 DEFAULTS 一一对应，缺项/空串回退默认。 */
    fun resolve(saved: List<String?>): List<String> =
        DEFAULTS.indices.map { i -> sanitize(saved.getOrNull(i), DEFAULTS[i]) }

    fun areDefaults(saved: List<String?>): Boolean = resolve(saved) == DEFAULTS
}
