package io.github.vexpaer.mybp.core.move

/**
 * 运动项目的自动图标分类：纯字符串规则，不依赖 AI，识别失败回落 GENERIC。
 */
enum class ExerciseIcon { RUNNING, WALKING, CYCLING, DUMBBELL, TIMER, BODYWEIGHT, LEGS, GENERIC }

object ExerciseIconMapper {

    // 顺序即优先级：更具体的动作在前，宽泛的（走/骑）在后
    private val rules: List<Pair<List<String>, ExerciseIcon>> = listOf(
        listOf("跑", "jog", "run", " sprint") to ExerciseIcon.RUNNING,
        listOf("哑铃", "杠铃", "壶铃", "划船", "弯举", "卧推", "硬拉", "推举", "飞鸟", "dumbbell", "barbell", "row", "curl", "press") to ExerciseIcon.DUMBBELL,
        listOf("平板", "支撑", "卷腹", "仰卧起坐", "核心", "腹肌", "plank", "core", "situp", "crunch") to ExerciseIcon.TIMER,
        listOf("俯卧撑", "引体", "双杠", "波比", "push", "pull", "burpee", "俯撑") to ExerciseIcon.BODYWEIGHT,
        listOf("深蹲", "蹲", "弓步", "箭步", "腿举", "提踵", "squat", "lunge", "calf") to ExerciseIcon.LEGS,
        listOf("骑", "单车", "自行车", "bike", "cycl", "骑行") to ExerciseIcon.CYCLING,
        listOf("走", "步行", "散步", "远足", "walk", "hike") to ExerciseIcon.WALKING,
    )

    fun map(rawName: String): ExerciseIcon {
        val name = rawName.lowercase()
        return rules.firstOrNull { (keywords, icon) -> keywords.any { name.contains(it) } }?.second
            ?: ExerciseIcon.GENERIC
    }
}
