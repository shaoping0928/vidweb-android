# VidWeb Android

Media Web 的 Android 客户端（Kotlin + Jetpack Compose + Media3/ExoPlayer），对接 `../vidweb` 的 FastAPI 后端，功能对齐 Web 前端的主要能力并增加移动端手势等增强。后端无需改动，仅需在客户端填写服务器地址。当前版本 **1.1.9**。

## 已实现功能

### 服务器连接
- 首次启动进入连接页：填写服务器地址（如 `http://192.168.1.10:8765`），测试并保存
- 地址存 DataStore；设置页可随时切换服务器（"切换服务器"回到连接页）
- 明文 HTTP 已通过 `network_security_config` 放行（局域网使用）

### 路径库
- 驱动器下拉、路径输入跳转、上一级、刷新
- 文件名过滤、仅视频、排序（名称/大小/时间，升降序）
- 路径库目录管理（底部操作表）：添加当前目录、手动添加、扫描入库（进度弹窗 + 取消）、移除
- 文件列表分页（每页条数在设置中调整）
- 长按视频：播放 / 加入播放列表 / 标签与详情 / 在资源管理器中显示
- 记住上次路径（`mw/lastPath`）

### 视频库
- 筛选：文件名、状态（可多选并集，含失效/损坏）、时长（4 档，可多选并集）、标签（任一/全部）、排序（7 种）
- 树形模式：树结构与数量不受筛选影响（文件夹始终显示）；打开文件夹时按当前筛选过滤其中的视频，筛选面板可开关「打开文件夹时应用筛选」
- 损坏检测：筛选面板「检测损坏」触发服务端 ffprobe 全库检测（任务页看进度），损坏文件卡片显示红色「损坏」徽标；缩略图生成失败也会被动标记
- 智能合集：应用筛选、保存当前筛选、管理（重命名 / 用当前筛选更新 / 删除）、数量徽标
- 卡片网格（16:9 缩略图、续播进度条、失效标记）；树形模式在列表内原地展开/收起目录（可整目录播放）
- 多选：长按进入，全选当前显示、加入/移出播放列表、批量打标签
- 「重置筛选」「从媒体库恢复」、分页

### 搜索 / 标签 / 任务 / 设置
- 搜索文件名、标签、评论；结果可播放、加入列表、查看详情
- 标签：层级树，默认根标签「全部标签」（首次加载自动展开）；每个标签右侧 ⋯ 菜单：新建子标签（弹窗输入名称）、重命名、设置颜色、删除；点击标签查看该标签及子标签下视频
- 任务：列表 + 进度条 + 状态，2 秒轮询，可删除记录（含损坏检测任务）
- 设置：服务器地址、监听地址/端口、ffmpeg/ffprobe 路径与检测、服务端转码开关与硬件编码器、访问日志开关、播放器默认值（音量/倍速/弹幕/弹幕速度）、每页条数、关于

### 播放器（PlayerActivity）
- 直连播放（支持 Range 原生 seek），播放失败自动按配置回退实时转码
- 音频编码预检：直连前用 `/api/files/probe` + `MediaCodecList` 检查设备能否解码音频，AC3/DTS/TrueHD 等不支持时直接走转码流（服务端统一输出 AAC），避免「有画面没声音」
- 实时转码播放：`/api/transcode/stream?path=&start=`，seek = 以目标位置重新请求流；显示编码器/直通标签
- 坏片段容错：流中断自动从当前位置 +8s 跳过（最多 4 次），失败后保留流模式可拖动重试
- 无帧看门狗：播放 6 秒无首帧自动切实时转码或提示
- 竖屏页面（B 站式）：上方 16:9 播放器，下方内容区（5 个 Tab：详情 / 播放列表 / 评论 / 截图 / 最近）随页面滚动；手势仅在播放器区域生效
- 控制栏参考主流播放器（自动隐藏，点按画面呼出；显示时顶/底区域手势自动让位）：
  - 竖屏（紧凑）：顶栏 返回 + 标题 + 弹幕/倍速；底栏 上一个/播放暂停/下一个 + 进度条（评论标记点）+ 时间 + 全屏
  - 横屏：弹幕、音量、倍速、画面比例、更多、全屏，画面右侧竖排「截图 / 锁定」
  - 横屏「更多」打开右侧面板（同一套 5 个 Tab）
- 返回逻辑：全屏（横屏）时按返回键或左上角返回先退出全屏回竖屏，再按才退出播放页；横屏面板打开时返回先关面板
- 进度条：拖动显示预览缩略图与时间气泡；叠加评论标记点，点击跳转
- 手势（可在设置「播放器手势」逐项开关）：
  - 左右滑动调进度（灵敏度 0.3–3.0x，画面中央显示目标时间）
  - 长按加速播放（默认 2.0x，可调 1.5–4.0x，松手恢复）
  - 双击：左 35% 快退 / 右 35% 快进（默认 10 秒，可调 5–30 秒），中间播放/暂停；单击呼出控制栏
  - 左半屏上下滑动调亮度、右半屏上下滑动调音量（实时生效并显示 OSD）
  - 双指缩放/平移画面
  - 锁定屏幕：屏蔽手势与控制，点左上角锁图标解锁
- 续播：读取 `last_position_ms` 自动跳转并提示；播放中每 5 秒 / 暂停时上报进度；播完清除续播点
- 播放记录：开始播放上报 `played`；播放结束仅暂停（不自动下一个）
- 右侧面板（竖屏内容区 / 横屏右侧弹层，5 个 Tab）：
  - 详情：时长/分辨率/大小/播放次数/续播点/评论数/路径，标签编辑，加入/移出播放列表
  - 播放列表：当前队列，点击切换，↑/↓ 调整顺序，✕ 移除（删除当前项自动切下一个）
  - 评论：列表点击跳转时间点，当前位置添加、编辑、删除
  - 截图：抓取当前画面（PixelCopy）上传服务端、网格浏览、删除
  - 最近播放：列表播放（替换队列）、刷新、清空
- 弹幕：评论按时间轴从右向左滚动，开关与速度跟随设置（`mw/danmaku`、`mw/danmakuSpeed`）

### 性能优化
- Release 构建开启 R8 代码压缩 + 资源压缩，APK 从约 18 MB 降到约 2.8 MB
- 缩略图仅在条目可见时轮询重试（4 秒 × 30 次）；`MediaEntry` 标注 `@Immutable` 并用 `remember` 缓存，Coil `ImageRequest` 复用
- 播放器启用解码器自动回退（`setEnableDecoderFallback`）与无帧看门狗，减少黑屏/硬解失败

### 界面适配
- 手机：底部导航栏；竖屏播放器 + 下方内容区
- 平板 / 宽屏（宽度 ≥ 600dp）：左侧导航栏（NavigationRail）、视频库网格按宽度自适应列数、连接页限宽居中
- 平板播放器允许自由旋转：竖屏为「播放器 + 内容区」，横屏为全屏播放；全屏按钮与返回键逻辑同步适配

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Kotlin / Compose Compiler | 1.9.22 / 1.5.8 |
| AGP / Gradle | 8.1.4 / 8.2 |
| Compose BOM / Material3 | 2023.06.01 |
| Media3 ExoPlayer | 1.1.1 |
| Retrofit / OkHttp / kotlinx.serialization | 2.9.0 / 4.12.0 / 1.6.0 |
| Coil | 2.4.0 |
| compileSdk / minSdk / targetSdk | 33 / 24 / 33 |

## 目录结构

```
app/src/main/java/com/vidweb/android/
  VidWebApp.kt           Application：Repo 单例、应用级协程
  MainActivity.kt        连接页 + 底部导航壳（路径库/视频库/搜索/标签/任务/设置）
  data/
    Models.kt            全部接口 DTO 与请求体
    Api.kt               Retrofit 接口 + ApiClient（动态 baseUrl、流地址拼接）
    Prefs.kt             DataStore 服务器地址
    Repo.kt              app_state 偏好缓存与常用操作
  player/
    PlayerActivity.kt    ExoPlayer 播放页（控制栏、面板、截图、容错、锁屏、预览）
    PlayerLauncher.kt    播放启动入口（paths + index）
    Gestures.kt          手势系统（GesturePrefs + Modifier.playerGestures）
    Danmaku.kt           弹幕 Canvas 覆盖层
  ui/
    theme/Theme.kt       Material3 深色主题
    common/Components.kt 列表行/卡片/分页/对话框/标签选择/详情/操作表
    path|lib|search|tags|tasks|settings/  各页面
  util/Format.kt         时间/大小/相对时间格式化
```

## 构建

要求：JDK 17+（本机使用 `E:/jdk/jdk-19.0.2`）、Android SDK（platform 33、build-tools 33+）。`local.properties` 指向 SDK：

```
sdk.dir=E\:\\jdk\\android
```

```powershell
$env:JAVA_HOME="E:\jdk\jdk-19.0.2"
.\gradlew.bat assembleDebug assembleRelease
# debug：app/build/outputs/apk/debug/app-debug.apk
# release：app/build/outputs/apk/release/app-release.apk（R8 + 资源压缩）
```

本仓库已在本机验证构建通过（Gradle 8.2 + AGP 8.1.4 + Kotlin 1.9.22）。Release 使用 debug 签名配置（`signingConfig = signingConfigs.getByName("debug")`），可直接安装；当前产物为 `vidweb-1.1.9-release.apk`（约 2.8 MB）与 `vidweb-1.1.9-debug.apk`。

安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`，或直接把 APK 拷到手机安装。

## 与 Web 端的差异 / 简化

- 播放列表排序用 ↑/↓ 按钮（移动端拖拽体验差）；Web 为拖动。
- 路径库侧栏、右键菜单改为底部操作表；悬停预览改为长按。
- 截图需要 Android 8.0+（PixelCopy；8.0 以下提示不支持）。
- 未接 UI 的接口：m3u 保存/加载、目录改路径等，可后续补充。
- 未实现：画中画、后台音频、投屏。
- 移动端增强（Web 没有）：手势调进度/亮度/音量/缩放、长按倍速、双击快进、锁屏、横屏侧边栏。

## 注意事项

- 服务端无鉴权，建议仅在同一局域网/VPN 使用；所有路径均为服务器本地路径。
- 转码流为 chunked fMP4，无总长度，ExoPlayer 播放正常但原生 seek 不可用（拖动会重新请求流）。
- 多端进度/播放次数共享同一数据库。
