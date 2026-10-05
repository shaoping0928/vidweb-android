# VidWeb Android

Media Web 的 Android 客户端（Kotlin + Jetpack Compose + Media3/ExoPlayer），对接 `../vidweb` 的 FastAPI 后端，功能对齐 Web 前端的主要能力。后端无需改动，仅需在客户端填写服务器地址。

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
- 筛选：文件名、状态（全部/未播放/有进度/已播放/失效）、时长（4 档）、标签（任一/全部）、排序（7 种）
- 智能合集：应用筛选、保存当前筛选、管理（重命名 / 用当前筛选更新 / 删除）、数量徽标
- 卡片网格（16:9 缩略图、续播进度条、失效标记），树形模式逐层下钻（可整目录播放）
- 多选：长按进入，全选当前显示、加入/移出播放列表、批量打标签
- 「重置筛选」「从媒体库恢复」、分页

### 搜索 / 标签 / 任务 / 设置
- 搜索文件名、标签、评论；结果可播放、加入列表、查看详情
- 标签：层级树展开、新建（可选父标签）、重命名、循环换色、删除；点击标签查看该标签下视频
- 任务：列表 + 进度条 + 状态，2 秒轮询，可删除记录
- 设置：服务器地址、监听地址/端口、ffmpeg/ffprobe 路径与检测、服务端转码开关与硬件编码器、访问日志开关、播放器默认值（音量/倍速/弹幕/弹幕速度）、每页条数、关于

### 播放器（PlayerActivity）
- 直连播放（支持 Range 原生 seek），播放失败自动按配置回退实时转码
- 实时转码播放：`/api/transcode/stream?path=&start=`，seek = 以目标位置重新请求流；显示编码器/直通标签
- 坏片段容错：流中断自动从当前位置 +8s 跳过（最多 4 次），失败后保留流模式可拖动重试
- 无帧看门狗：播放 6 秒无首帧自动切实时转码或提示
- 控制：上一个/播放暂停/下一个、进度条拖动、倍速（0.5–4.0x）、音量、静音、弹幕开关、画面比例（适应/裁剪/拉伸）、截图、面板、全屏（横屏 + 沉浸）
- 续播：读取 `last_position_ms` 自动跳转并提示；播放中每 5 秒 / 暂停时上报进度；播完清除续播点
- 播放记录：开始播放上报 `played`；播放结束仅暂停（不自动下一个）
- 右侧面板（底部弹层 5 个 Tab）：
  - 详情：时长/分辨率/大小/播放次数/续播点/评论数/路径，标签编辑，加入/移出播放列表
  - 播放列表：当前队列，点击切换，↑/↓ 调整顺序，✕ 移除（删除当前项自动切下一个）
  - 评论：列表点击跳转时间点，当前位置添加、编辑、删除
  - 截图：抓取当前画面（PixelCopy）上传服务端、网格浏览、删除
  - 最近播放：列表播放（替换队列）、刷新、清空
- 弹幕：评论按时间轴从右向左滚动，开关与速度跟随设置（`mw/danmaku`、`mw/danmakuSpeed`）

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
    PlayerActivity.kt    ExoPlayer 播放页（控制、面板、截图、容错）
    PlayerLauncher.kt    播放启动入口（paths + index）
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
.\gradlew.bat assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

本仓库已在本机验证 `assembleDebug` 构建通过（Gradle 8.2 + AGP 8.1.4 + Kotlin 1.9.22）。

安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`，或直接把 APK 拷到手机安装。

## 与 Web 端的差异 / 简化

- 播放列表排序用 ↑/↓ 按钮（移动端拖拽体验差）；Web 为拖动。
- 路径库侧栏、右键菜单改为底部操作表；悬停预览改为长按。
- 树形模式逐层下钻（面包屑返回），不做一次性完整树展开。
- 截图需要 Android 8.0+（PixelCopy；8.0 以下提示不支持）。
- 进度条评论标记点、m3u 保存/加载、目录改路径等接口已封装但未接 UI，可后续补充。
- 未实现：画中画、后台音频、手势亮度/音量、投屏。

## 注意事项

- 服务端无鉴权，建议仅在同一局域网/VPN 使用；所有路径均为服务器本地路径。
- 转码流为 chunked fMP4，无总长度，ExoPlayer 播放正常但原生 seek 不可用（拖动会重新请求流）。
- 多端进度/播放次数共享同一数据库。
