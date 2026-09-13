# Design Direction — 我有高血压

> 本文档是 v0.1.0 全部 UI 决策的唯一事实来源（Source of Truth）。
> 由 ui-ux-pro-max（design-system 检索 + pro-rules 交付清单）与 modern-jetpack-compose 规范共同推导。
> UI Review 2 / 3 以此为基准逐条核对。

## 0. 品牌与语气

「我有高血压」不是疾病标签，是一个**带一点自嘲的生活方式品牌**。

- Slogan：**少盐 · 早睡 · 动一动**
- 语气：温和、克制、有生活气息、略带幽默；**绝不制造疾病焦虑**
- 禁止出现：医疗诊断式措辞、恐吓性红色警示、"监测/治疗"类词汇
- 一切数据都标注"估算"属性，诚实而非精确

## 1. 风格定位

**"纸感极简"（Paper Minimal）**：瑞士式网格与字阶 + 暖纸色底 + 单一松绿品牌色。
来自 ui-ux-pro-max 的风格检索（Minimalism & Swiss Style），拒绝其中的紫色系配色（命中用户禁令），拒绝着陆页模式（不适用于原生四 Tab 结构）。

- 现代但不冰冷：暖白纸感底色替代纯白/纯灰
- 克制：每屏**至多一个 hero 数据区块**，其余用字阶与留白组织
- 有生活气息：文案像朋友说话，图标手绘感线性风格

## 2. 设计 Tokens

### 2.1 色彩 — Light

| Token | 值 | 用途 |
|---|---|---|
| `bg` | `#F7F5F0` | 页面背景（暖纸色） |
| `surface` | `#FFFFFF` | 卡片 / 浮层 |
| `surfaceVariant` | `#F0EDE5` | 次级填充、chip、输入框底 |
| `hairline` | `#E5E0D6` | 分隔线、描边 |
| `ink` | `#23281F` | 主文本 |
| `inkSecondary` | `#6E736C` | 次文本（≥4.5:1） |
| `inkFaint` | `#9A9E93` | 弱化说明（仅大号文字用） |
| `primary` | `#31684E` | 松绿：主操作、选中态、品牌 |
| `onPrimary` | `#FFFFFF` | |
| `primaryContainer` | `#DFEDE4` | 选中底、soft pill |
| `onPrimaryContainer` | `#1D4634` | |
| `sleep` | `#4F6486` | 早睡早起 功能色（夜蓝，非医院蓝） |
| `sleepContainer` | `#E4E9F2` | |
| `saltGood` | `#3E7C59` | 低盐友好 ≥7 |
| `saltMid` | `#9A7222` | 4–7（琥珀文字色，保证对比） |
| `saltBad` | `#BB4A32` | <4（陶土色，**不是医院红**） |
| `move` | `#C96A3B` | 抬腿跑跑 功能色（柿子色） |
| `moveContainer` | `#F6E4D8` | |
| `scrim` | `#1A1C18` @ 45% | 浮层遮罩 |

### 2.2 色彩 — Dark（独立校准，非简单反转）

| Token | 值 |
|---|---|
| `bg` | `#171A17` |
| `surface` | `#1F2320` |
| `surfaceVariant` | `#262B27` |
| `hairline` | `#2E332F` |
| `ink` | `#E8E6DF` |
| `inkSecondary` | `#A6ABA1` |
| `primary` | `#8FC9A8` |
| `onPrimary` | `#14261C` |
| `primaryContainer` | `#234737` |
| `onPrimaryContainer` | `#C4E5D2` |
| `sleep` | `#9AAECF` / `sleepContainer` `#26303F` |
| `saltGood` `#85C29B` / `saltMid` `#D4AC5C` / `saltBad` `#E08367` |
| `move` `#E09672` / `moveContainer` `#3A2A20` |

### 2.3 字阶（sp，系统默认中文无衬线，数字用 tabular）

| 级别 | 规格 | 用途 |
|---|---|---|
| Display | 34/40 · w700 · tnum | hero 数字（7 h 44 min） |
| Title L | 22/28 · w600 | 屏标题 |
| Title M | 17/24 · w600 | 区块标题、卡片标题 |
| Body | 15/22 · w400 | 正文 |
| Label | 12.5/16 · w500 | 区块眉标（配 inkSecondary） |
| Caption | 11.5/14 · w400 | 免责、估算标注 |

### 2.4 间距 / 圆角 / 图标

- 间距 4dp 网格：`4 · 8 · 12 · 16 · 20 · 24 · 32 · 48`；屏幕水平边距 **20dp**；区块间 28dp
- 圆角：chip 10 / card 18 / hero 22 / sheet 顶部 24 / pill 全圆；
  **按钮采用 M3 默认全圆药丸**（Review 2 定案：与 chip/pill 家族一致，不另设 14dp）
- 图标：**自绘线性图标族**，24dp 网格、1.8dp 描边、圆头圆角、单色继承文本色；
  统一 outline 一套（同层级禁止混用 filled）；**禁止 emoji 当图标**
- 触控目标 ≥48dp（图标视觉可以小，命中区必须扩够）

### 2.5 表面层级

1. 页面底 `bg`，区块眉标（Label 级，inkSecondary）+ 内容，**不加框**
2. 需要承载的数据块用 `surface` 卡（每屏 ≤2 张），不嵌套卡片
3. hero 区块可用 `primaryContainer`/功能色 container 做大面积 tint
4. 列表项之间用 `hairline` 分隔线，不用卡片堆叠

### 2.6 动效语言

- 时长 token：fast 150ms / base 220ms / slow 320ms；缓动 `CubicBezier(0.2, 0, 0, 1)`
- 底部导航选中 pill：spring(dampingRatio 0.85, stiffness 380)
- 页面切换：淡入 + 6dp 上移（180ms），退出不动画（fade-through）
- 数字不做滚动动画（克制）；系统"移除动画"开启时全部跳过
- 触觉：仅导航切换与保存成功时轻触反馈，不做连环震动

### 2.7 底部导航（品牌自定义，不用默认 NavigationBar）

- 高 68dp + 导航条 inset，`surface` 底 + 顶部 hairline
- 每项：24dp 图标 + 11sp 标签；选中态为 primaryContainer 药丸底 + 主色图标 + w600 标签
- 未选中：inkSecondary；四个 Tab 各自可用功能色做选中图标 tint（统一层级）

### 2.8 空状态

线性图标（56dp 圆形 tinted 底）+ Title M 一句话 + Body 一句补充 + 可选 CTA。
示例文案：抬腿跑跑空态「今天还没动。从两个深蹲开始也行。」

### 2.9 文案库（v0.1.0 定稿）

- 睡眠估算标注：**根据手机使用情况估算，不是医疗监测**
- 手动数据标记：**手动修改**
- 低盐免责：**根据餐厅类型和常见菜品估算，不代表实际钠含量**
- 点餐建议通用句：**少盐、少酱油、酱汁分开、少喝汤。**
- 关于页：「我有高血压」不是诊断，只是提醒你：少盐、早睡、动一动。

## 3. 无障碍承诺（对照 pro-rules 清单）

- 双主题正文对比 ≥4.5:1；分隔线两主题均可见
- 颜色不是唯一信息载体（盐分徽标带数字、连续天数带文案）
- 有含义图标带 contentDescription，装饰性图标传 null
- 全部交互目标 ≥48dp；按压有视觉反馈（80–150ms 内）
- 表单有标签与错误提示；图表提供语义摘要

## 4. UI Review 1 结论（编码前定案）

- [x] 视觉风格：纸感极简 + 松绿品牌色
- [x] Design tokens / 色板 / 字阶 / 间距 / 圆角：如上
- [x] 表面层级：眉标 + 少卡片 + hairline
- [x] 图标语言：自绘 1.8dp 线性族
- [x] 动效语言：淡入为主，克制
- [x] 品牌底部导航方案：药丸选中态
- [x] 空状态与文案基调：定稿

## 5. UI Review 2 记录（四页面完成后）

审查方式：modern-jetpack-compose（state / accessibility 规则）代码审查
+ ui-ux-pro-max 交付清单逐条核对 + 模拟器截图（光/暗）视觉验证。

修复的行为问题：

1. 设置页「恢复默认名称」后输入框不同步（remember 键错误）
2. 「我的运动」点击未预选对应运动，多一步操作
3. 隐私弹窗「先不用」点了没反应（补 declined 状态）
4. 已授权定位的用户重进页面不会触发定位（入口时同步权限状态）
5. 主题选择行补 RadioButton role 语义

定案偏差：底部导航标签由 11sp 调整为 12.5sp（labelMedium），
可读性优先，中文小字号 11sp 过于纤弱；其余 token 不变。

## 6. UI Review 3 记录（Release 前终审）

- [x] 双主题截图核对（四 Tab + Onboarding）：对比度、层级、对齐
- [x] 触控目标 ≥48dp；按压反馈齐全
- [x] 图标同族（1.8dp 描边）；无 emoji 图标
- [x] 免责/估算标注在每个数据出口出现
- [x] 空状态文案与品牌语气一致
- [x] 图表语义描述齐全；颜色不单独承载信息（徽标带数字、柱状带文案）

---

# Paper Minimal 2.0（v0.1.1）

在保留「纸感极简 + 松绿品牌」的前提下，v0.1.1 补上了**数据可视化、微动画与图形化反馈**：
静态高级感之外，让人愿意每天打开。参考了 GymMane 的"数据即 UI / 强视觉中心"思路，
全部用自己的 Compose Canvas / Vector 实现，未复制其任何代码、素材或布局。

## Motion Tokens（ui/theme/Motion.kt）

| Token | 值 | 用途 |
|---|---|---|
| Easing | CubicBezier(0.2, 0, 0, 1) | 一切过渡 |
| Fast | 150ms | 图标切换、icon 交叉淡化 |
| Normal | 220ms | 主题 Crossfade、页面淡入 |
| Emphasized | 320ms | 半径圈过渡 |
| Draw | 500ms | Arc / Ring 首次描画 |
| Grow | 380ms | 柱条生长 |
| Stagger | 30ms | Marker 错峰 |

原则：**只有状态变化才动画**。禁止无限漂浮/pulse/渐变、粒子、巨型 Lottie。
系统「移除动画」（ANIMATOR_DURATION_SCALE = 0）→ `LocalReducedMotion`，
所有进场/描画动画直接呈现最终状态。

## Visualization Tokens（ui/theme/Chart.kt）

stroke 2dp · point 4dp · bar 16dp/6dp 圆角 · RingStroke 10dp · RingStrokeSmall 7dp ·
动画时长取 Motion tokens。统一用于：Sleep Arc、Sleep Trend、Score Ring、Streak Ring、
WeekBars——四页像同一个 App。

## 四个页面的视觉中心

- **早睡早起** = Sleep Arc：半圆夜间时间轴（左入睡右起床，中央 7 h 44 min），
  首现描画动画；朗读语义「估算入睡 23 点 47 分…共 7 小时 44 分」
- **少吃点盐** = 地图 + 低盐友好度：徽标 Marker（数字+箭头）错峰淡入（前 15 个）、
  首次定位一次 pulse、搜索一次雷达波、半径圈平滑变化；详情页 Score Ring
- **抬腿跑跑** = Streak Ring（柿子橙，7 天满圈）+ WeekBars 生长动画 +
  保存成功时圆环+对勾描画反馈（Canvas 自绘，非 Lottie）
- **设置** = 干净的偏好行 + 数据出口（导出记录，SAF ZIP）

## Icon System

28 个自绘线性图标：24dp 网格、1.8dp 圆头描边、统一 outline。
运动项目按名称关键词自动映射（跑→跑步、蹲→腿部、平板→计时…纯字符串规则，
识别失败回落 generic，core 有测试）。

## 数据出口（导出）

设置 → 数据与备份 → 导出记录：SAF CreateDocument 生成
`my-blood-pressure-is-high-YYYY-MM-DD.zip`
（sleep.csv · exercise.csv · settings.json · README.txt，UTF-8 带 BOM，
缺失字段留空）。成功后下载图标短暂变为对勾 + 轻触觉。

## 地图与隐私（v0.1.1 修复）

- 进入「少吃点盐」→ 隐私说明 → 同意后才初始化高德 SDK（Lazy init）
- 官方 Privacy API：`MapsInitializer` 与 `ServiceSettings` 的
  updatePrivacyShow/updatePrivacyAgree（签名按 SDK 11.2.100/9.8.1 jar 核实）
- API Key 仅经 GitHub Secret `AMAP_API_KEY` 注入 CI/Release，永不入库
- 定位权限拒绝：不崩溃，可稍后再开或去系统设置

## UI Review 4 记录（v0.1.1 Polish）

- [x] Motion/Visualization tokens 全图表统一（SleepArc/趋势/ScoreRing/StreakRing/WeekBars）
- [x] Reduced Motion 全组件接通
- [x] 触觉仅限：Tab 切换 / 保存运动 / 导出成功
- [x] 图表语义描述（Arc/ScoreRing/WeekBars/Trend）全覆盖
- [x] 无 InfiniteTransition；全部动画 finite
- [x] 截图审查（光/暗 × 数据态/空态/隐私态）逐张核对后修正


