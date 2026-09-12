package io.github.vexpaer.mybp.core.salt

/**
 * 低盐友好度评分（0–10，本地、可解释）。
 *
 * 输入是高德 POI 的餐厅名称与类型描述，输出：
 * - 分数与档位（FRIENDLY ≥ 7，NEUTRAL ≥ 4，CAUTION < 4）
 * - 命中的规则列表（可解释：每条规则有名称与加减分）
 * - 该类餐厅的「推荐 / 谨慎」点餐指引
 *
 * 不按"川菜 = 不健康"一刀切：每个类型都给出能点的选项，
 * 并根据店名/招牌里的线索（清蒸、白灼、腊味……）做增减。
 * 评分只反映餐饮类型与常见菜品的普遍规律，不代表实际钠含量。
 */
object SaltScorer {

    enum class Band { FRIENDLY, NEUTRAL, CAUTION }

    data class Rule(val id: String, val label: String, val delta: Double)

    data class Verdict(
        val score: Double,
        val band: Band,
        val categoryLabel: String,
        val reasons: List<Rule>,
        val recommend: List<String>,
        val caution: List<String>,
        val tip: String,
    )

    private data class Category(
        val id: String,
        val keywords: List<String>,
        val base: Double,
        val label: String,
        val recommend: List<String>,
        val caution: List<String>,
        val suppress: Set<String> = emptySet(),
    )

    private data class Modifier(
        val id: String,
        val keywords: List<String>,
        val delta: Double,
        val label: String,
    )

    private val GENERIC_TIP = "少盐、少酱油、酱汁分开、少喝汤。"

    // 顺序即优先级：更具体的类型在前。
    private val categories = listOf(
        Category("malatang", listOf("麻辣烫", "冒菜"), 3.4, "麻辣烫 / 冒菜",
            listOf("清汤锅底", "新鲜蔬菜", "酱料减半"),
            listOf("汤底别喝", "加工丸子", "麻酱花生酱"),
            suppress = setOf("spicy")),
        Category("hotpot", listOf("火锅"), 3.8, "火锅",
            listOf("清汤或菌汤锅底", "新鲜食材", "蘸料减半"),
            listOf("麻辣红油锅底", "加工丸滑类", "别喝汤")),
        Category("bbq", listOf("烧烤", "烤串", "串串", "烤肉"), 3.2, "烧烤",
            listOf("烤蔬菜", "原味少撒料", "配无糖茶"),
            listOf("腌制烤料", "加工肠类", "重酱刷料")),
        Category("sichuan", listOf("川菜", "四川", "川味", "湘菜", "湖南", "辣"), 4.2, "川湘菜",
            listOf("清炒时蔬", "蒸蛋", "粉蒸类"),
            listOf("水煮 / 干锅", "腊味", "泡椒剁椒")),
        Category("fastfood", listOf("快餐", "汉堡", "炸鸡", "便当"), 4.0, "快餐",
            listOf("少酱现做", "生菜多的选项"),
            listOf("薯条", "培根", "加工芝士")),
        Category("buffet", listOf("自助"), 4.6, "自助餐",
            listOf("清蒸 / 白灼档口", "新鲜食材"),
            listOf("重酱汁", "腌制凉菜", "汤底")),
        Category("noodle", listOf("面馆", "拉面", "米线", "米粉", "粉面", "汤面", "面庄"), 4.8, "粉面馆",
            listOf("烫青菜", "加个蛋", "汤留一半"),
            listOf("喝完汤底", "卤味浇头", "酸菜辣油")),
        Category("western", listOf("西餐", "意大利", "牛排", "披萨", "意面"), 5.4, "西餐",
            listOf("烤蔬菜", "清烤 / 香煎主菜"),
            listOf("培根火腿", "芝士酱汁", "浓汤")),
        Category("dongbei", listOf("东北"), 5.0, "东北菜",
            listOf("清炖", "大拌菜"),
            listOf("酱烧类", "腌菜")),
        Category("jiangzhe", listOf("江浙", "本帮", "杭帮", "淮扬"), 5.8, "江浙菜",
            listOf("清蒸鱼", "白灼", "清炒时蔬"),
            listOf("红烧", "酱鸭酱肉", "腌笃鲜")),
        Category("seafood", listOf("海鲜", "渔", "海味"), 6.4, "海鲜",
            listOf("清蒸", "白灼"),
            listOf("蒜蓉粉丝", "重酱蘸料", "咸鱼")),
        Category("dimsum", listOf("饺子", "包子", "面点", "小吃", "馄饨"), 6.2, "面点小吃",
            listOf("蒸制主食", "不喝汤", "配烫菜"),
            listOf("酱菜小菜", "重卤蘸料")),
        Category("congee", listOf("粥"), 7.6, "粥品",
            listOf("白粥", "蒸点心", "白灼青菜"),
            listOf("腌菜酱瓜", "咸鸭蛋", "卤味")),
        Category("cantonese", listOf("粤菜", "粤式", "茶餐厅", "港式", "烧腊"), 7.2, "粤菜 / 茶餐厅",
            listOf("清蒸鱼", "白灼类", "蒸蛋"),
            listOf("豉油皇", "腊味", "烧腊酱汁")),
        Category("steamer", listOf("蒸菜", "蒸品"), 7.8, "蒸菜",
            listOf("原味蒸制", "少蘸料"),
            listOf("豉油汁", "腌腊配料")),
        Category("japanese", listOf("日料", "日本", "寿司", "刺身", "日式", "鳗"), 7.0, "日料",
            listOf("刺身少蘸酱油", "蒸蛋", "烤鱼"),
            listOf("酱油", "味噌汤", "腌制小菜")),
        Category("cafe", listOf("咖啡", "甜品", "蛋糕", "面包", "烘焙", "奶茶", "饮品"), 7.4, "咖啡甜品",
            listOf("美式 / 无糖茶", "原味烘焙"),
            listOf("起酥咸点", "加工芝士")),
        Category("salad", listOf("轻食", "沙拉", "减脂"), 8.6, "轻食沙拉",
            listOf("油醋汁分开", "烤鸡胸", "新鲜蔬菜"),
            listOf("凯撒酱", "加工肉")),
        Category("chinese", listOf("中餐", "中式", "家常", "饭店", "酒家", "餐厅", "食堂"), 5.8, "家常中餐",
            listOf("清蒸白灼", "清炒时蔬", "米饭"),
            listOf("重酱汁", "腌腊", "汤泡饭")),
    )

    private val fallback = Category(
        "unknown", emptyList(), 5.5, "未识别餐厅类型",
        listOf("清蒸白灼", "清炒时蔬"),
        listOf("重酱汁", "腌制食品", "汤底"),
    )

    private val modifiers = listOf(
        Modifier("steam", listOf("清蒸"), +0.8, "有清蒸类可选"),
        Modifier("blanch", listOf("白灼", "白切"), +0.7, "有白灼类可选"),
        Modifier("stirfry", listOf("清炒", "时蔬"), +0.5, "有清炒时蔬可选"),
        Modifier("porridge", listOf("粥"), +0.3, "有粥类可选"),
        Modifier("spicy", listOf("麻辣", "水煮", "香锅", "冒菜"), -0.8, "重辣重油做法多"),
        Modifier("cured", listOf("腊", "腌", "咸鱼", "咸菜", "酸菜", "卤", "酱鸭", "酱骨", "梅菜"), -0.7, "腌制腊味类多"),
        Modifier("pickled-pepper", listOf("泡椒", "剁椒", "椒盐", "盐焗", "咸蛋", "咸肉"), -0.6, "盐渍做法多"),
        Modifier("drypot", listOf("烤鱼", "干锅", "孜然"), -0.5, "重口干锅类多"),
        Modifier("braised", listOf("红烧", "盖浇", "油焖"), -0.4, "红烧类酱汁多"),
    )

    fun score(name: String, category: String): Verdict {
        val picked = categories.firstOrNull { it.keywords.any { k -> category.contains(k) } }
            ?: categories.firstOrNull { it.keywords.any { k -> name.contains(k) } }
            ?: fallback

        val rules = ArrayList<Rule>()
        rules += Rule("base:${picked.id}", "类型：${picked.label}", 0.0)
        for (m in modifiers) {
            if (m.id in picked.suppress) continue
            val hit = m.keywords.any { name.contains(it) }
            if (hit) rules += Rule(m.id, m.label, m.delta)
        }

        val raw = picked.base + rules.sumOf { it.delta }
        val rounded = kotlin.math.round(raw * 10) / 10.0
        val final = rounded.coerceIn(1.0, 9.8)

        return Verdict(
            score = final,
            band = when {
                final >= 7.0 -> Band.FRIENDLY
                final >= 4.0 -> Band.NEUTRAL
                else -> Band.CAUTION
            },
            categoryLabel = picked.label,
            reasons = rules,
            recommend = picked.recommend,
            caution = picked.caution,
            tip = GENERIC_TIP,
        )
    }
}
