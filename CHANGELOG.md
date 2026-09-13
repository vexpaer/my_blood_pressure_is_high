# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [0.1.1] — 2026-09-13

Polish Release：**Paper Minimal 2.0** —— 数据可视化、微动画与图形化反馈。

### Added

- **数据导出**：设置 → 数据与备份 → 导出记录，生成 ZIP
  （sleep.csv / exercise.csv / settings.json / README.txt，UTF-8，缺失留空）
- **Sleep Arc**：早睡早起页的半圆夜间时间轴视觉中心（Canvas 自绘 + 首现描画动画）
- **Score Ring**：餐厅详情的圆环分数视觉中心
- **Streak Ring / WeekBars 动画**：抬腿跑跑页圆环 + 柱条生长 + 保存成功描画反馈
- **统一 Motion System**：时长/缓动 tokens、系统"移除动画"全程尊重
- **统一 Visualization Tokens**：五类图表同一套描边/圆点/圆角/动画
- **图标系统扩展**：28 个统一 1.8dp 线性图标；运动项目按名称关键词自动配图标
- 高德地图真正接通：CI/Release 通过 GitHub Secret `AMAP_API_KEY` 注入真实 Key
- 新增 `signing-info.yml`（手动查询签名证书 SHA1/SHA256，不输出任何密钥）

### Changed

- 设置页重构为 个性化 / 数据与备份 / 关于与隐私 三段式
- 底部导航：药丸平滑滑动、图标 0.94→1.0 缩放、标签透明度过渡
- 主题切换 220ms Crossfade；深色模式持续独立校准
- 地图 Marker 升级为分数徽标+箭头，错峰淡入；详情页视觉重排
- README 更新导出说明与高德配置说明

### Fixed

- **CI / Release 未注入 `AMAP_API_KEY` 的问题**（此前构建均为无 Key 演示模式）
- **高德 SDK 隐私合规初始化**：`MapsInitializer` + `ServiceSettings` 官方
  Privacy API 在任何 MapView/PoiSearch 创建之前调用（签名按当前 SDK jar 核实）
- 地图 SDK 懒初始化：未同意隐私说明时完全不创建 SDK 对象、不请求定位
- 定位权限拒绝流程：不崩溃，提供"稍后开启 / 去系统设置"
- 无障碍：Sleep Arc / Score Ring / WeekBars 语义描述，触控目标与对比度复核

[0.1.1]: https://github.com/vexpaer/my_blood_pressure_is_high/releases/tag/v0.1.1

## [0.1.0] — 2026-09-12

首个公开版本。一个温和、自嘲、不制造焦虑的 Android 生活方式 App：
**少盐 · 早睡 · 动一动**。所有数据仅保存在手机本地。

### 新增

- **早睡早起**
  - 根据手机使用情况（`UsageStatsManager` / `UsageEvents`）估算昨晚入睡时间、
    起床时间与睡眠时长，展示为"7 h 44 min"式的估算值，并注明
    「根据手机使用情况估算，不是医疗监测」
  - 最近 7 天记录列表、平均睡眠、入睡/起床时间趋势图
  - 手动修正某一晚（TimePicker），保存后标记**手动修改**，估算永不覆盖手工数据
  - 权限按需申请：进入页面时才引导开启「使用情况访问权限」
- **少吃点盐**
  - 高德地图 + 周边餐饮 POI 搜索（500 m / 1 km / 3 km 可选）
  - 每家餐厅一个 0–10 的**低盐友好度**徽标，本地可解释评分算法
    （类型基线 + 名称修饰规则，有单元测试），并注明
    「根据餐厅类型和常见菜品估算，不代表实际钠含量」
  - 餐厅详情：评分理由、**来了可以点 / 尽量少碰**两类点餐指引、
    点餐建议「少盐、少酱油、酱汁分开、少喝汤。」
  - 未配置高德 Key 时进入演示模式（配置指引 + 示例数据），App 正常运行不崩溃
  - 定位仅在使用该页面时单次访问（系统定位），首次使用前弹出高德服务隐私说明
- **抬腿跑跑**
  - 自由创建运动（不写死种类），五种记录模式：
    次数 / 组×次 / 时间 / 距离(+时间) / 重量+组×次
  - 极速记录路径（≤3 步）、今天记录修改/删除、最近 7 天柱状条、
    连续运动天数（今天没动不清零昨天的连续链）
- **设置**
  - 四个底部 Tab 的显示名称可自定义（≤6 字，可一键恢复默认）
  - 主题：跟随系统 / 浅色 / 深色（独立校准的深色板）
  - 关于与隐私说明
- 首次打开的三页极简 Onboarding，不在引导阶段申请任何权限

### 技术

- Kotlin 2.0 + Jetpack Compose + Material 3（自定义品牌主题：暖纸色 + 松绿）
- Room（本地数据）+ DataStore（设置）；无网络上传、无账号、无统计/广告 SDK
- GitHub Actions CI：lint + 单元测试 + Debug/Release 构建；Tag 触发 Release

[0.1.0]: https://github.com/vexpaer/my_blood_pressure_is_high/releases/tag/v0.1.0
