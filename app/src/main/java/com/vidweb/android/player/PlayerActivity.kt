package com.vidweb.android.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.CommentAddBody
import com.vidweb.android.data.CommentItem
import com.vidweb.android.data.CommentUpdateBody
import com.vidweb.android.data.FileDetail
import com.vidweb.android.data.FileTagsBody
import com.vidweb.android.data.PathBody
import com.vidweb.android.data.ProgressBody
import com.vidweb.android.data.RecentItem
import com.vidweb.android.data.Repo
import com.vidweb.android.data.Screenshot
import com.vidweb.android.data.Tag
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.TagPickerDialog
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.Danger
import com.vidweb.android.ui.theme.TextDim
import com.vidweb.android.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private val NETWORK_ERRORS = setOf(
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
)

class PlayerActivity : ComponentActivity() {

    private var exo: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val paths = intent.getStringArrayListExtra("paths")?.toList().orEmpty()
        val index = intent.getIntExtra("index", 0)
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)
        val player = ExoPlayer.Builder(this, renderers).build()
        exo = player
        setContent {
            com.vidweb.android.ui.theme.VidWebTheme {
                PlayerScreen(this, player, paths, index)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exo?.pause()
    }

    override fun onDestroy() {
        exo?.release()
        exo = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    activity: PlayerActivity,
    player: ExoPlayer,
    initialPaths: List<String>,
    initialIndex: Int,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { (activity.application as com.vidweb.android.VidWebApp).repo }
    val exoPlayer = player
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val gesturePrefs = remember {
        val obj = repo.getJsonObject("mw/gestures")
        if (obj == null) GesturePrefs()
        else runCatching {
            Json { ignoreUnknownKeys = true; coerceInputValues = true }
                .decodeFromJsonElement<GesturePrefs>(obj)
        }.getOrDefault(GesturePrefs())
    }

    val queue = remember { mutableStateListOf<String>().apply { addAll(initialPaths) } }
    var index by remember { mutableStateOf(initialIndex.coerceIn(0, (initialPaths.size - 1).coerceAtLeast(0))) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var streamMode by remember { mutableStateOf(false) }
    var streamOffset by remember { mutableStateOf(0L) }
    var streamRetries by remember { mutableStateOf(0) }
    var networkRetries by remember { mutableStateOf(0) }
    var streamLabel by remember { mutableStateOf("") }
    var frameSeen by remember { mutableStateOf(false) }
    var pendingResume by remember { mutableStateOf(0L) }
    var resumeApplied by remember { mutableStateOf(false) }
    var transcodeOn by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<FileDetail?>(null) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var shots by remember { mutableStateOf<List<Screenshot>>(emptyList()) }
    var recent by remember { mutableStateOf<List<RecentItem>>(emptyList()) }
    var allTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showControls by remember { mutableStateOf(true) }
    var showPanel by remember { mutableStateOf(false) }
    var panelTab by remember { mutableStateOf(0) }
    var osd by remember { mutableStateOf("") }
    var fullscreen by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var speedDialog by remember { mutableStateOf(false) }
    var volumeDialog by remember { mutableStateOf(false) }
    var danmakuOn by remember { mutableStateOf(repo.getBool("mw/danmaku", true)) }
    var danmakuSpeed by remember { mutableStateOf(repo.getInt("mw/danmakuSpeed", 8000).toLong()) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var addCommentDialog by remember { mutableStateOf(false) }
    var editComment by remember { mutableStateOf<CommentItem?>(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var menuShot by remember { mutableStateOf<Screenshot?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    var gestureSeekTarget by remember { mutableStateOf<Long?>(null) }
    var gestureLabel by remember { mutableStateOf("") }
    var gestureActive by remember { mutableStateOf(false) }
    var longPressActive by remember { mutableStateOf(false) }
    var normalSpeed by remember { mutableStateOf(1f) }
    var brightnessValue by remember { mutableStateOf(0.5f) }
    var zoom by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var dragPreviewMs by remember { mutableStateOf<Long?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    val path: String = queue.getOrNull(index) ?: ""

    fun showOsd(text: String) {
        osd = text
    }

    fun absolutePosition(): Long =
        if (streamMode) streamOffset + player.currentPosition else player.currentPosition

    fun totalDuration(): Long {
        if (streamMode && durationMs > 0) return durationMs
        if (durationMs > 0) return durationMs
        val d = player.duration
        return if (d > 0) d else 0
    }

    fun saveProgress() {
        val currentPath = queue.getOrNull(index) ?: return
        val total = totalDuration()
        if (total <= 0) return
        val pos = absolutePosition()
        val value = when {
            pos > total - 5000 -> 0L
            pos > 5000 -> pos
            else -> return
        }
        scope.launch { runCatching { ApiClient.api.saveProgress(ProgressBody(currentPath, value)) } }
    }

    fun loadComments() {
        val currentPath = queue.getOrNull(index) ?: return
        scope.launch { runCatching { ApiClient.api.comments(currentPath) }.onSuccess { comments = it.items } }
    }

    fun loadShots() {
        val currentPath = queue.getOrNull(index) ?: return
        scope.launch { runCatching { ApiClient.api.screenshots(currentPath) }.onSuccess { shots = it.items } }
    }

    fun loadRecent() {
        scope.launch { runCatching { ApiClient.api.recent().items }.onSuccess { recent = it } }
    }

    fun fetchDetail() {
        val currentPath = queue.getOrNull(index) ?: return
        scope.launch {
            runCatching { ApiClient.api.fileDetail(currentPath) }.onSuccess {
                detail = it
                durationMs = it.record?.durationMs ?: 0
                pendingResume = it.record?.lastPositionMs ?: 0
                resumeApplied = false
            }
        }
    }

    fun startStream(target: String, startMs: Long, keepRetries: Boolean = false, restoring: Boolean = false) {
        if (!keepRetries) streamRetries = 0
        streamMode = true
        streamOffset = startMs.coerceAtLeast(0)
        frameSeen = false
        player.setMediaItem(MediaItem.fromUri(ApiClient.transcodeStreamUrl(target, streamOffset)))
        player.prepare()
        player.play()
        showOsd(if (restoring) "已恢复实时转码播放" else "已切换实时转码播放（不占用磁盘）")
        scope.launch {
            runCatching { ApiClient.api.transcodePlan(target) }.onSuccess { plan ->
                streamLabel = plan.encoderLabel
                if (plan.mode == "remux") showOsd("直通播放（仅重封装，不转码）")
                else if (plan.encoderLabel.isNotBlank()) showOsd("实时转码播放：${plan.encoderLabel}")
            }
        }
    }

    fun trySkipBadSegment(playedMs: Long): Boolean {
        if (streamRetries >= 4) return false
        if (playedMs < 500 && streamOffset <= 0) return false
        val total = durationMs
        val target = streamOffset + playedMs + 8000
        if (total > 0 && target >= total - 1000) return false
        streamRetries += 1
        showOsd("跳过损坏片段，继续播放…")
        startStream(path, target, keepRetries = true)
        return true
    }

    fun playAt(i: Int) {
        val count = queue.size
        if (count == 0) return
        saveProgress()
        index = ((i % count) + count) % count
        val target = queue[index]
        streamMode = false
        streamOffset = 0
        streamRetries = 0
        networkRetries = 0
        streamLabel = ""
        frameSeen = false
        pendingResume = 0
        resumeApplied = false
        durationMs = 0
        detail = null
        comments = emptyList()
        shots = emptyList()
        zoom = 1f
        panX = 0f
        panY = 0f
        player.setMediaItem(MediaItem.fromUri(ApiClient.streamUrl(target)))
        player.prepare()
        player.play()
        scope.launch { runCatching { ApiClient.api.played(PathBody(target)) } }
        fetchDetail()
        loadComments()
        loadShots()
    }

    fun handleError(error: PlaybackException) {
        val currentPath = queue.getOrNull(index) ?: return
        val network = error.errorCode in NETWORK_ERRORS
        if (network && networkRetries < 5) {
            networkRetries += 1
            val resumeAt = if (streamMode) streamOffset + player.currentPosition else position
            showOsd("网络中断，正在重连（$networkRetries/5）…")
            scope.launch {
                delay(800L * networkRetries)
                if (streamMode) {
                    startStream(currentPath, resumeAt, keepRetries = true, restoring = true)
                } else {
                    player.seekTo(resumeAt.coerceAtLeast(0))
                    player.prepare()
                    player.play()
                }
            }
            return
        }
        networkRetries = 0
        if (streamMode) {
            if (trySkipBadSegment(player.currentPosition)) return
            showOsd("播放失败：文件可能损坏，可拖动进度条跳过")
            return
        }
        scope.launch {
            val cfg = runCatching { ApiClient.api.config() }.getOrNull()
            transcodeOn = cfg?.transcode ?: false
            if (transcodeOn) {
                startStream(currentPath, player.currentPosition)
            } else {
                val probe = runCatching { ApiClient.api.probe(currentPath) }.getOrNull()
                if (probe?.playable == false) {
                    showOsd("浏览器不支持该编码（${probe.video.uppercase()}），可在设置中开启服务端转码")
                } else {
                    showOsd("播放失败，文件可能损坏或不完整")
                }
            }
        }
    }

    fun toggleFullscreen() {
        fullscreen = !fullscreen
        activity.requestedOrientation = if (fullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun doSeek(ms: Long) {
        if (streamMode) startStream(path, ms)
        else player.seekTo(ms.coerceAtLeast(0))
    }

    fun uploadScreenshot(bitmap: Bitmap) {
        val currentPath = queue.getOrNull(index) ?: return
        val positionMs = absolutePosition()
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.toByteArray()
                }
            }
            val part = MultipartBody.Part.createFormData(
                "file",
                "shot_${System.currentTimeMillis()}.png",
                bytes.toRequestBody("image/png".toMediaType()),
            )
            val result = runCatching {
                ApiClient.api.uploadScreenshot(
                    part,
                    currentPath.toRequestBody("text/plain".toMediaType()),
                    positionMs.toString().toRequestBody("text/plain".toMediaType()),
                )
            }
            if (result.isSuccess) {
                showOsd("截图已保存")
                loadShots()
            } else {
                showOsd("截图上传失败")
            }
        }
    }

    fun captureScreenshot() {
        val view = playerView ?: return
        if (view.width <= 0 || view.height <= 0) return
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())
        val listener = PixelCopy.OnPixelCopyFinishedListener { result ->
            if (result == PixelCopy.SUCCESS) uploadScreenshot(bitmap) else showOsd("截图失败")
        }
        try {
            val surface = view.videoSurfaceView as? SurfaceView
            if (surface != null) {
                PixelCopy.request(surface, bitmap, listener, handler)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(activity.window, bitmap, listener, handler)
            } else {
                showOsd("当前系统版本不支持截图")
            }
        } catch (e: Exception) {
            showOsd("截图失败：${e.message}")
        }
    }

    fun applyBrightness(delta: Float) {
        brightnessValue = (brightnessValue + delta).coerceIn(0.02f, 1f)
        val attrs = activity.window.attributes
        attrs.screenBrightness = brightnessValue
        activity.window.attributes = attrs
        gestureLabel = "亮度 ${(brightnessValue * 100).toInt()}%"
    }

    fun applyVolume(delta: Float) {
        val value = (player.volume + delta).coerceIn(0f, 1f)
        player.volume = value
        repo.set("mw/volume", (value * 100).toInt().toString())
        gestureLabel = "音量 ${(value * 100).toInt()}%"
    }

    fun gestureSeekBy(deltaFraction: Float) {
        val total = totalDuration()
        if (total <= 0) return
        if (gestureSeekTarget == null) gestureSeekTarget = absolutePosition()
        val step = deltaFraction * total * gesturePrefs.seekSensitivity
        val target = ((gestureSeekTarget ?: 0L) + step.toLong()).coerceIn(0L, total - 1000)
        gestureSeekTarget = target
        dragPreviewMs = target
        gestureLabel = "跳到 ${Format.time(target)} / ${Format.time(total)}"
        if (!showControls) showControls = true
    }

    fun gestureSeekFinish() {
        val target = gestureSeekTarget
        gestureSeekTarget = null
        dragPreviewMs = null
        gestureActive = false
        gestureLabel = ""
        if (target != null) doSeek(target)
    }

    fun longPressStart() {
        if (longPressActive) return
        normalSpeed = player.playbackParameters.speed
        player.setPlaybackSpeed(gesturePrefs.longPressSpeed)
        longPressActive = true
    }

    fun longPressEnd() {
        if (!longPressActive) return
        longPressActive = false
        player.setPlaybackSpeed(normalSpeed)
    }

    fun doubleTapSeek(deltaMs: Long) {
        val total = totalDuration()
        if (total <= 0) return
        val target = (absolutePosition() + deltaMs).coerceIn(0L, total - 1000)
        showOsd(if (deltaMs > 0) "快进 ${deltaMs / 1000} 秒" else "快退 ${-deltaMs / 1000} 秒")
        doSeek(target)
    }

    /* ------------------------------------------------------------ lifecycle */

    DisposableEffect(player, path) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) saveProgress()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (streamMode) {
                        val total = durationMs
                        val pos = absolutePosition()
                        if (total > 0 && pos < total - 2000 && trySkipBadSegment(player.currentPosition)) {
                            return
                        }
                    }
                    showOsd("播放结束")
                    return
                }
                if (playbackState == Player.STATE_READY) {
                    if (!resumeApplied && pendingResume > 5000) {
                        val total = totalDuration()
                        if (total <= 0 || pendingResume < total - 5000) {
                            player.seekTo(pendingResume)
                            showOsd("已从 ${Format.time(pendingResume)} 继续播放")
                        }
                        resumeApplied = true
                    }
                    val d = player.duration
                    if (durationMs <= 0 && d > 0) {
                        durationMs = d
                        val currentPath = queue.getOrNull(index) ?: return
                        scope.launch {
                            runCatching {
                                ApiClient.api.mediaInfo(
                                    com.vidweb.android.data.MediaInfoBody(
                                        path = currentPath,
                                        durationMs = d,
                                        width = 0,
                                        height = 0,
                                    )
                                )
                            }
                        }
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                frameSeen = true
                networkRetries = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                handleError(error)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            saveProgress()
        }
    }

    LaunchedEffect(Unit) {
        if (queue.isEmpty()) {
            activity.finish()
            return@LaunchedEffect
        }
        runCatching { ApiClient.api.config() }.onSuccess { transcodeOn = it.transcode }
        runCatching { ApiClient.api.tags().items }.onSuccess { allTags = it }
        player.volume = repo.getInt("mw/volume", 80) / 100f
        player.setPlaybackSpeed(repo.getInt("mw/speed", 10) / 10f)
        val attr = activity.window.attributes.screenBrightness
        brightnessValue = if (attr in 0.02f..1f) attr else 0.5f
        playAt(index)
        loadRecent()
    }

    LaunchedEffect(path) {
        while (true) {
            delay(500)
            position = absolutePosition()
        }
    }

    LaunchedEffect(path) {
        while (true) {
            delay(5000)
            if (playing) saveProgress()
        }
    }

    LaunchedEffect(path, streamMode, playing, frameSeen) {
        if (streamMode || !playing || frameSeen) return@LaunchedEffect
        delay(6000)
        if (!streamMode && playing && !frameSeen) {
            if (transcodeOn) {
                showOsd("画面无法解码（可能是 HEVC/10bit），切换实时转码…")
                startStream(path, player.currentPosition)
            } else {
                showOsd("画面无法解码（可能是 HEVC/10bit），可在设置中开启服务端转码")
            }
        }
    }

    LaunchedEffect(osd) {
        if (osd.isNotEmpty()) {
            delay(2200)
            osd = ""
        }
    }

    LaunchedEffect(gestureLabel, gestureActive) {
        if (gestureLabel.isNotEmpty() && !gestureActive) {
            delay(900)
            gestureLabel = ""
        }
    }

    LaunchedEffect(dragPreviewMs) {
        val ms = dragPreviewMs
        if (ms == null || path.isBlank()) {
            previewUrl = null
            return@LaunchedEffect
        }
        delay(140)
        previewUrl = ApiClient.previewUrl(path, ms / 1000.0)
    }

    LaunchedEffect(showControls, playing, locked) {
        if (showControls && playing && !locked) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler { activity.finish() }

    /* ------------------------------------------------------------ ui */

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
            .playerGestures(
                enabled = !locked && !showPanel,
                prefs = gesturePrefs,
                controlsVisible = showControls && !locked,
                onTap = { showControls = !showControls },
                onDoubleTapLeft = { doubleTapSeek(-gesturePrefs.doubleTapMs) },
                onDoubleTapCenter = { if (player.isPlaying) player.pause() else player.play() },
                onDoubleTapRight = { doubleTapSeek(gesturePrefs.doubleTapMs) },
                onLongPressStart = { longPressStart() },
                onLongPressEnd = { longPressEnd() },
                onSeek = { gestureSeekBy(it) },
                onSeekEnd = { gestureSeekFinish() },
                onBrightness = { applyBrightness(it) },
                onVolume = { applyVolume(it) },
                onTransform = { z, px, py ->
                    zoom = (zoom * z).coerceIn(1f, 4f)
                    if (zoom <= 1.01f) {
                        zoom = 1f
                        panX = 0f
                        panY = 0f
                    } else {
                        val maxX = (zoom - 1f) * viewSize.width / 2f
                        val maxY = (zoom - 1f) * viewSize.height / 2f
                        panX = (panX + px).coerceIn(-maxX, maxX)
                        panY = (panY + py).coerceIn(-maxY, maxY)
                    }
                },
                onActive = { active ->
                    gestureActive = active
                    if (!active) {
                        gestureLabel = ""
                    }
                },
            ),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    this.player = exoPlayer
                    playerView = this
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = panX,
                    translationY = panY,
                ),
        )

        DanmakuOverlay(
            comments = comments,
            positionProvider = { absolutePosition() },
            enabled = danmakuOn && playing,
            speedMs = danmakuSpeed,
            modifier = Modifier.fillMaxSize(),
        )

        if (osd.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            ) {
                Text(osd, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White)
            }
        }

        if (gestureSeekTarget != null && previewUrl != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.width(200.dp).aspectRatio(16f / 9f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        gestureLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White,
                    )
                }
            }
        } else if (gestureLabel.isNotEmpty() && gestureActive) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(gestureLabel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White)
            }
        }

        if (longPressActive) {
            Surface(
                color = Accent.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            ) {
                Text(
                    String.format(java.util.Locale.US, "%.1fx", gesturePrefs.longPressSpeed),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = Color.White,
                )
            }
        }

        if (locked) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clickable {
                        locked = false
                        showControls = true
                    },
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "解锁",
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                )
            }
        }

        if (showControls && !locked) {
            PlayerControls(
                title = if (path.isBlank()) "" else "${index + 1}/${queue.size} · ${path.substringAfterLast('\\').substringAfterLast('/')}",
                playing = playing,
                position = if (dragging) dragValue.toLong() else position,
                duration = totalDuration(),
                streamMode = streamMode,
                streamLabel = streamLabel,
                speed = player.playbackParameters.speed,
                volume = player.volume,
                muted = player.volume <= 0.001f,
                danmakuOn = danmakuOn,
                fullscreen = fullscreen,
                comments = comments,
                previewUrl = if (dragging) previewUrl else null,
                onBack = { activity.finish() },
                onPrev = { playAt(index - 1) },
                onNext = { playAt(index + 1) },
                onToggle = { if (player.isPlaying) player.pause() else player.play() },
                onSeekStart = { value ->
                    dragging = true
                    dragValue = value
                },
                onSeekChange = { value ->
                    dragValue = value
                    dragPreviewMs = value.toLong()
                },
                onSeekEnd = {
                    dragging = false
                    doSeek(dragValue.toLong())
                    dragPreviewMs = null
                },
                onMarkerClick = { doSeek(it) },
                onSpeed = { speedDialog = true },
                onVolume = { volumeDialog = true },
                onDanmaku = {
                    danmakuOn = !danmakuOn
                    repo.setBool("mw/danmaku", danmakuOn)
                },
                onAspect = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                onSnapshot = { captureScreenshot() },
                onPanel = { showPanel = true },
                onFullscreen = { toggleFullscreen() },
                onLock = {
                    locked = true
                    showControls = false
                },
            )
        }
    }

    if (speedDialog) {
        var value by remember { mutableStateOf(player.playbackParameters.speed) }
        AlertDialog(
            onDismissRequest = { speedDialog = false },
            title = { Text("播放速度") },
            text = {
                Column {
                    Text(String.format(java.util.Locale.US, "%.2fx", value))
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0.5f..4f)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    player.setPlaybackSpeed(value)
                    repo.set("mw/speed", (value * 10).toInt().toString())
                    speedDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { speedDialog = false }) { Text("取消") } },
        )
    }

    if (volumeDialog) {
        var value by remember { mutableStateOf(player.volume) }
        AlertDialog(
            onDismissRequest = { volumeDialog = false },
            title = { Text("音量") },
            text = {
                Column {
                    Text("${(value * 100).toInt()}")
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    player.volume = value
                    repo.set("mw/volume", (value * 100).toInt().toString())
                    volumeDialog = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { volumeDialog = false }) { Text("取消") } },
        )
    }

    val panelContent: @Composable () -> Unit = {
        PlayerPanel(
            panelTab = panelTab,
            onTabChange = { panelTab = it },
            detail = detail,
            path = path,
            repo = repo,
            onEditTags = { showTagPicker = true },
            queue = queue,
            currentIndex = index,
            onPlayIndex = { playAt(it) },
            onRemoveQueue = { i ->
                if (i in queue.indices) {
                    val wasCurrent = i == index
                    queue.removeAt(i)
                    if (queue.isEmpty()) {
                        activity.finish()
                    } else if (wasCurrent) {
                        index = i.coerceAtMost(queue.size - 1)
                        playAt(index)
                    } else if (i < index) {
                        index -= 1
                    }
                }
            },
            onMoveQueue = { from, to ->
                if (from in queue.indices && to in queue.indices && from != to) {
                    val item = queue.removeAt(from)
                    queue.add(to, item)
                    if (index == from) index = to
                    else if (from < index && to >= index) index -= 1
                    else if (from > index && to <= index) index += 1
                }
            },
            comments = comments,
            onSeekTo = { doSeek(it) },
            onAddComment = { addCommentDialog = true },
            onEditComment = { editComment = it },
            onDeleteComment = { comment ->
                scope.launch {
                    runCatching { ApiClient.api.deleteComment(comment.id) }
                    loadComments()
                }
            },
            shots = shots,
            onCapture = { captureScreenshot() },
            onShotMenu = { menuShot = it },
            recent = recent,
            onPlayRecent = { item ->
                val paths = recent.map { it.path }
                queue.clear()
                queue.addAll(paths)
                playAt(paths.indexOf(item.path).coerceAtLeast(0))
                showPanel = false
            },
            onRefreshRecent = { loadRecent() },
            onClearRecent = {
                scope.launch {
                    runCatching { ApiClient.api.clearRecent() }
                    loadRecent()
                }
            },
        )
    }

    if (showPanel) {
        if (landscape) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showPanel = false },
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(400.dp),
                ) {
                    panelContent()
                }
            }
        } else {
            ModalBottomSheet(onDismissRequest = { showPanel = false }) {
                Box(Modifier.fillMaxWidth().height(520.dp)) {
                    panelContent()
                }
            }
        }
    }

    if (addCommentDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addCommentDialog = false },
            title = { Text("在 ${Format.time(absolutePosition())} 添加评论") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = text.trim()
                    addCommentDialog = false
                    if (value.isNotEmpty()) {
                        scope.launch {
                            runCatching {
                                ApiClient.api.addComment(
                                    CommentAddBody(path, absolutePosition(), value)
                                )
                            }
                            loadComments()
                        }
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { addCommentDialog = false }) { Text("取消") } },
        )
    }

    editComment?.let { comment ->
        var text by remember(comment.id) { mutableStateOf(comment.text) }
        AlertDialog(
            onDismissRequest = { editComment = null },
            title = { Text("编辑评论") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = text.trim()
                    editComment = null
                    if (value.isNotEmpty()) {
                        scope.launch {
                            runCatching { ApiClient.api.updateComment(comment.id, CommentUpdateBody(value)) }
                            loadComments()
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editComment = null }) { Text("取消") } },
        )
    }

    menuShot?.let { shot ->
        ActionSheet(
            title = Format.dateTime(shot.createdAt),
            actions = listOf(
                "删除截图" to {
                    scope.launch {
                        runCatching { ApiClient.api.deleteScreenshot(shot.id) }
                        loadShots()
                    }
                },
            ),
            onDismiss = { menuShot = null },
        )
    }

    if (showTagPicker) {
        val initial = detail?.tags?.map { it.name }.orEmpty()
        TagPickerDialog(
            tags = allTags,
            initialSelected = initial,
            onDismiss = { showTagPicker = false },
            onConfirm = { names ->
                showTagPicker = false
                scope.launch {
                    runCatching { ApiClient.api.setFileTags(FileTagsBody(path, names)) }
                        .onSuccess { detail = detail?.copy(tags = it.tags) }
                }
            },
        )
    }
}

/* ---------------------------------------------------------------- controls */

@Composable
private fun PlayerControls(
    title: String,
    playing: Boolean,
    position: Long,
    duration: Long,
    streamMode: Boolean,
    streamLabel: String,
    speed: Float,
    volume: Float,
    muted: Boolean,
    danmakuOn: Boolean,
    fullscreen: Boolean,
    comments: List<CommentItem>,
    previewUrl: String?,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onSeekStart: (Float) -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    onMarkerClick: (Long) -> Unit,
    onSpeed: () -> Unit,
    onVolume: () -> Unit,
    onDanmaku: () -> Unit,
    onAspect: () -> Unit,
    onSnapshot: () -> Unit,
    onPanel: () -> Unit,
    onFullscreen: () -> Unit,
    onLock: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (streamMode) {
                Text(
                    streamLabel.ifBlank { "转码" },
                    color = Accent,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.Lock, contentDescription = "锁定", tint = Color.White)
            }
        }

        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一个", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(72.dp)) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "下一个", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(10.dp)) {
            if (previewUrl != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                        AsyncImage(
                            model = previewUrl,
                            contentDescription = null,
                            modifier = Modifier.width(200.dp).aspectRatio(16f / 9f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.time(position), color = Color.White, style = MaterialTheme.typography.labelSmall)
                BoxWithConstraints(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    val trackWidth = maxWidth
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Slider(
                            value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                            onValueChange = {
                                onSeekStart(it)
                                onSeekChange(it)
                            },
                            onValueChangeFinished = onSeekEnd,
                            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (duration > 0) {
                            for (comment in comments) {
                                val fraction = (comment.positionMs.toFloat() / duration).coerceIn(0f, 1f)
                                Box(
                                    Modifier
                                        .offset(x = trackWidth * fraction - 3.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Accent)
                                        .clickable { onMarkerClick(comment.positionMs) },
                                )
                            }
                        }
                    }
                }
                Text(Format.time(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onSpeed) { Text("${String.format(java.util.Locale.US, "%.1f", speed)}x", color = Color.White) }
                TextButton(onClick = onVolume) { Text(if (muted) "静音" else "${(volume * 100).toInt()}", color = Color.White) }
                TextButton(onClick = onDanmaku) { Text(if (danmakuOn) "弹幕开" else "弹幕关", color = if (danmakuOn) Accent else Color.White) }
                TextButton(onClick = onAspect) { Text("画面", color = Color.White) }
                TextButton(onClick = onSnapshot) { Text("截图", color = Color.White) }
                TextButton(onClick = onPanel) { Text("面板", color = Color.White) }
                IconButton(onClick = onFullscreen) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "全屏", tint = Color.White)
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- panel */

@Composable
private fun PlayerPanel(
    panelTab: Int,
    onTabChange: (Int) -> Unit,
    detail: FileDetail?,
    path: String,
    repo: Repo,
    onEditTags: () -> Unit,
    queue: List<String>,
    currentIndex: Int,
    onPlayIndex: (Int) -> Unit,
    onRemoveQueue: (Int) -> Unit,
    onMoveQueue: (Int, Int) -> Unit,
    comments: List<CommentItem>,
    onSeekTo: (Long) -> Unit,
    onAddComment: () -> Unit,
    onEditComment: (CommentItem) -> Unit,
    onDeleteComment: (CommentItem) -> Unit,
    shots: List<Screenshot>,
    onCapture: () -> Unit,
    onShotMenu: (Screenshot) -> Unit,
    recent: List<RecentItem>,
    onPlayRecent: (RecentItem) -> Unit,
    onRefreshRecent: () -> Unit,
    onClearRecent: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = panelTab) {
            for ((tabIndex, label) in listOf("详情", "播放列表", "评论", "截图", "最近").withIndex()) {
                Tab(
                    selected = panelTab == tabIndex,
                    onClick = { onTabChange(tabIndex) },
                    text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        when (panelTab) {
            0 -> DetailTab(detail = detail, path = path, repo = repo, onEditTags = onEditTags)
            1 -> QueueTab(
                queue = queue,
                currentIndex = currentIndex,
                onPlay = onPlayIndex,
                onRemove = onRemoveQueue,
                onMove = onMoveQueue,
            )
            2 -> CommentsTab(
                comments = comments,
                onSeek = onSeekTo,
                onAdd = onAddComment,
                onEdit = onEditComment,
                onDelete = onDeleteComment,
            )
            3 -> ShotsTab(shots = shots, onCapture = onCapture, onDelete = onShotMenu)
            4 -> RecentTab(
                items = recent,
                onPlay = onPlayRecent,
                onRefresh = onRefreshRecent,
                onClear = onClearRecent,
            )
        }
    }
}

@Composable
private fun DetailTab(
    detail: FileDetail?,
    path: String,
    repo: Repo,
    onEditTags: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var inPlaylist by remember { mutableStateOf(false) }
    LaunchedEffect(path) {
        runCatching { repo.playlistPaths() }.onSuccess { inPlaylist = it.contains(path) }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            detail?.name ?: path.substringAfterLast('\\').substringAfterLast('/'),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        val record = detail?.record
        val lines = buildList {
            if (record != null) {
                if (record.durationMs > 0) add("时长：${Format.time(record.durationMs)}")
                if (record.width > 0) add("分辨率：${record.width}×${record.height}")
                if (record.size > 0) add("大小：${Format.bytes(record.size)}")
                add("播放：${record.playCount} 次 · 上次 ${Format.relative(record.lastPlayedAt)}")
            }
            add("评论：${detail?.commentCount ?: 0} 条")
            add("路径：$path")
        }
        for (line in lines) {
            Text(line, color = TextDim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("标签", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEditTags) { Text("编辑") }
        }
        Text(
            detail?.tags?.joinToString("、") { it.name }?.ifBlank { "无" } ?: "无",
            color = TextDim,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            scope.launch {
                if (inPlaylist) repo.removeFromPlaylist(listOf(path)) else repo.addToPlaylist(listOf(path))
                inPlaylist = !inPlaylist
            }
        }) { Text(if (inPlaylist) "移出播放列表" else "加入播放列表") }
    }
}

@Composable
private fun QueueTab(
    queue: List<String>,
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(queue.size) { i ->
            val name = queue[i].substringAfterLast('\\').substringAfterLast('/')
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(i) }
                    .background(if (i == currentIndex) Accent.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(28.dp),
                )
                Text(
                    name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (i == currentIndex) Accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { if (i > 0) onMove(i, i - 1) }) { Text("↑") }
                TextButton(onClick = { if (i < queue.size - 1) onMove(i, i + 1) }) { Text("↓") }
                TextButton(onClick = { onRemove(i) }) { Text("✕", color = Danger) }
            }
        }
    }
}

@Composable
private fun CommentsTab(
    comments: List<CommentItem>,
    onSeek: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (CommentItem) -> Unit,
    onDelete: (CommentItem) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onAdd) { Text("添加评论（当前位置）") }
            Spacer(Modifier.weight(1f))
            Text("${comments.size} 条", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        if (comments.isEmpty()) {
            Text("暂无评论", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(comments, key = { it.id }) { comment ->
                    var menu by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSeek(comment.positionMs) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Format.time(comment.positionMs),
                            color = Accent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(70.dp),
                        )
                        Text(comment.text, modifier = Modifier.weight(1f))
                        TextButton(onClick = { menu = true }) { Text("⋯") }
                        if (menu) {
                            ActionSheet(
                                title = "评论",
                                actions = listOf(
                                    "编辑" to { onEdit(comment) },
                                    "删除" to { onDelete(comment) },
                                ),
                                onDismiss = { menu = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotsTab(
    shots: List<Screenshot>,
    onCapture: () -> Unit,
    onDelete: (Screenshot) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onCapture) { Text("截取当前画面") }
            Spacer(Modifier.weight(1f))
            Text("${shots.size} 张", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        if (shots.isEmpty()) {
            Text("暂无截图", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                items(shots, key = { it.id }) { shot ->
                    AsyncImage(
                        model = ApiClient.screenshotImageUrl(shot.id),
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .clickable { onDelete(shot) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTab(
    items: List<RecentItem>,
    onPlay: (RecentItem) -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRefresh) { Text("刷新") }
            TextButton(onClick = onClear) { Text("清空记录") }
            Spacer(Modifier.weight(1f))
            Text("${items.size} 条", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        if (items.isEmpty()) {
            Text("暂无记录", color = TextDim, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.path }) { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(item) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            buildString {
                                if (item.durationMs > 0) append(Format.time(item.durationMs))
                                if (item.lastPositionMs > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("续播 ${Format.time(item.lastPositionMs)}")
                                }
                                if (item.lastPlayedAt > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append(Format.relative(item.lastPlayedAt))
                                }
                            },
                            color = TextDim,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
