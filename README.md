# 我有高血压

> 少盐 · 早睡 · 动一动。
> 一个温和、自嘲、不制造焦虑的 Android 生活方式 App。

![CI](https://github.com/vexpaer/my_blood_pressure_is_high/actions/workflows/ci.yml/badge.svg)

**这不是医疗诊断 App。** 名字只是提醒你少盐，别慌，先把觉睡好。

它只做三件小事，并把它们做顺手：

| | 功能 | 一句话 |
|---|---|---|
| 🌙 | **早睡早起** | 根据手机使用情况，估算昨晚睡了多久（不是医疗监测） |
| 🥣 | **少吃点盐** | 看看附近有什么相对适合少吃点盐的（低盐友好度 + 点餐建议） |
| 🏃 | **抬腿跑跑** | 记一下今天有没有动 |

四个底部 Tab 的名字都可以在设置里改。数据全部只保存在手机本地：
无登录、无注册、无统计 SDK、无广告、无上传。

## 下载

前往 [Releases](https://github.com/vexpaer/my_blood_pressure_is_high/releases)
下载 `my-blood-pressure-is-high-<版本>.apk` 安装（需允许安装未知来源应用）。

- 最低支持 Android 8.0（API 26）
- APK 包含 arm64-v8a 与 x86_64

## 功能说明

### 早睡早起

- 通过系统的「使用情况访问权限」（`UsageStatsManager`）读取一天里
  「什么时候在用手机」，在夜间寻找最长的未使用区间来估算睡眠。
- 估算规则完全本地、可解释：观察窗口为前一天 17:00 → 当天 15:00；
  短于 1 分钟的亮屏碎片不算「明显使用」；候选区间需落在核心夜间时段；
  数据不足就诚实地说数据不足，而不是编一个数。
- 可以手动修正任意一晚（标记**手动修改**，估算不会覆盖它）。

### 少吃点盐

- 使用[高德开放平台](https://lbs.amap.com/)的地图与周边搜索 SDK
  查找附近餐厅（500 m / 1 km / 3 km），每个 Marker 直接显示
  **低盐友好度 0–10**。
- 评分算法在本地运行、完全可解释：餐饮类型给基线分，店名/招牌里的
  线索（清蒸、白灼、腊味、麻辣……）做加减，每个分数都能列出依据。
  不搞「川菜 = 不健康」的一刀切，每类餐厅都告诉你**来了可以点什么**。
- 没有配置高德 Key 时，此页面进入演示模式（展示配置指引与示例数据），
  App 其余功能完全正常。

### 抬腿跑跑

- 运动种类随便建（深蹲、跑步、平板支撑、锤式弯举……都行），
  记录方式五选一：次数 / 组×次 / 时间 / 距离 / 重量+组×次。
- 打开 → 记一笔 → 输数字 → 保存，三步以内。
- 连续天数今天没动不清零，昨天的努力还在。

## 构建

```bash
./gradlew :core:test          # 纯 JVM 单元测试（估算/评分/校验/持久化）
./gradlew :app:assembleDebug  # Debug APK
./gradlew :app:lintDebug      # Android Lint
./gradlew :app:assembleRelease
```

要求：JDK 17，Android SDK 35。CI（`.github/workflows/ci.yml`）在每次
push / PR 上自动执行以上全部检查。

### 配置高德 Key

不配置也能用（地图页为演示模式）。要启用真实地图与附近餐厅：

1. 在[高德开放平台](https://console.amap.com/)创建 **Android Key**，
   包名填 `io.github.vexpaer.mybp`，SHA1 填应用签名证书的 SHA1
   （可运行仓库的手动工作流 **Signing info** 查看，或本地
   `keytool -list -v -keystore your.keystore`）
2. 在仓库 Settings → Secrets and variables → Actions → Repository secrets
   新建 **`AMAP_API_KEY`**
3. CI / Release 构建会自动注入（`ci.yml` / `release.yml` / `screenshots.yml`）；
   本地构建可用 `./gradlew :app:assembleDebug -PAMAP_API_KEY=你的Key`

> 真实 Key 与应用签名绑定，属个人凭证，不入库；Release APK 由 CI
> 用仓库 Secrets 签名并注入 Key，安装即可用真实地图。

## 数据导出

设置 → 数据与备份 → **导出记录**：通过系统文件面板选择位置，
生成 `my-blood-pressure-is-high-YYYY-MM-DD.zip`：

| 文件 | 内容 |
|---|---|
| `sleep.csv` | 睡眠估算结果（date / bedtime / wake_time / duration_minutes / source） |
| `exercise.csv` | 运动记录（date / time / exercise / mode / sets / reps / weight_kg / distance_m / duration_s） |
| `settings.json` | 设置快照，含 `schema_version`（为未来导入预留） |
| `README.txt` | 字段说明 |

CSV 为 UTF-8（带 BOM，Excel 可直接打开中文）；缺失数据留空，不用 0 冒充。
导出是你主动触发的唯一"数据出口"，全程不联网。

### 发布签名

- CI 从 GitHub Secrets 读取 `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` /
  `KEY_ALIAS` / `KEY_PASSWORD` 解码签名，密钥文件本身**不提交**到仓库。
- 推送 `v*` Tag 触发 `.github/workflows/release.yml`：
  测试 → lint → Release 构建 → 创建 GitHub Release 并上传
  `my-blood-pressure-is-high-<版本>.apk`。

## 隐私

- 睡眠估算数据、手机使用分析结果、运动记录、设置：**只保存在这台手机上**
  （Room + DataStore），没有账号，没有上传。
- 「少吃点盐」只在你使用它的时候访问一次定位，用于查询附近餐厅；
  位置不保存、不上传。地图服务由高德提供，首次使用前会向你说明并征得同意。
- 不收集、不上传、不跟踪。源码即文档，欢迎审计。

## 设计

视觉方向见 [docs/design/DIRECTION.md](docs/design/DIRECTION.md)：
「纸感极简」——暖纸色底、松绿品牌色、1.8dp 线性自绘图标、
克制的动效；深色模式独立校准而非简单反转。

## 许可

[MIT](LICENSE)

## 免责声明

本应用不提供医疗建议，不能替代专业诊疗。如有健康问题请咨询医生。
