package com.vidweb.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.Repo
import com.vidweb.android.data.SettingsData
import com.vidweb.android.data.SettingsUpdateBody
import com.vidweb.android.data.TestToolBody
import com.vidweb.android.player.GesturePrefs
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.Danger
import com.vidweb.android.ui.theme.TextDim
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

private val ENCODER_LABELS = mapOf(
    "auto" to "自动（优先硬件）",
    "nvenc" to "NVIDIA NVENC",
    "qsv" to "Intel QuickSync",
    "amf" to "AMD AMF",
    "cpu" to "CPU (libx264)",
)

@Composable
fun SettingsScreen(repo: Repo, onDisconnect: () -> Unit) {
    val scope = rememberCoroutineScope()
    val stateMap by repo.state.collectAsState()
    val savedUrl by repo.prefs.serverUrl.collectAsState(initial = "")
    var urlInput by remember { mutableStateOf("") }
    var urlMessage by remember { mutableStateOf("") }

    var settings by remember { mutableStateOf<SettingsData?>(null) }
    var ffmpeg by remember { mutableStateOf("") }
    var ffprobe by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var transcode by remember { mutableStateOf(false) }
    var encoder by remember { mutableStateOf("auto") }
    var encoderMenu by remember { mutableStateOf(false) }
    var accessLog by remember { mutableStateOf(false) }
    var toolMessage by remember { mutableStateOf("") }

    var volume by remember { mutableStateOf(80f) }
    var speed by remember { mutableStateOf(10f) }
    var danmaku by remember { mutableStateOf(true) }
    var danmakuSpeed by remember { mutableStateOf(8000f) }
    var pageSize by remember { mutableStateOf(30) }
    var pageSizeMenu by remember { mutableStateOf(false) }

    var gSeek by remember { mutableStateOf(true) }
    var gLongPress by remember { mutableStateOf(true) }
    var gDoubleTap by remember { mutableStateOf(true) }
    var gBrightness by remember { mutableStateOf(true) }
    var gVolume by remember { mutableStateOf(true) }
    var gZoom by remember { mutableStateOf(true) }
    var gLongPressSpeed by remember { mutableStateOf(2f) }
    var gDoubleTapMs by remember { mutableStateOf(10000f) }
    var gSeekSensitivity by remember { mutableStateOf(1f) }

    fun saveGestures() {
        repo.setJson(
            "mw/gestures",
            buildJsonObject {
                put("seek", gSeek)
                put("longPress", gLongPress)
                put("doubleTap", gDoubleTap)
                put("brightness", gBrightness)
                put("volume", gVolume)
                put("zoom", gZoom)
                put("longPressSpeed", JsonPrimitive(gLongPressSpeed))
                put("doubleTapMs", JsonPrimitive(gDoubleTapMs.toLong()))
                put("seekSensitivity", JsonPrimitive(gSeekSensitivity))
            },
        )
    }

    LaunchedEffect(savedUrl) { if (urlInput.isBlank()) urlInput = savedUrl }

    LaunchedEffect(Unit) {
        runCatching { ApiClient.api.settings() }.onSuccess {
            settings = it
            ffmpeg = it.ffmpeg
            ffprobe = it.ffprobe
            host = it.host
            port = it.port.toString()
            transcode = it.transcode
            encoder = it.transcodeEncoder
            accessLog = it.accessLog
        }
    }

    LaunchedEffect(stateMap) {
        if (stateMap.isEmpty()) return@LaunchedEffect
        volume = repo.getInt("mw/volume", 80).toFloat()
        speed = repo.getInt("mw/speed", 10).toFloat()
        danmaku = repo.getBool("mw/danmaku", true)
        danmakuSpeed = repo.getInt("mw/danmakuSpeed", 8000).toFloat()
        pageSize = repo.pageSize()
        val gestures = repo.getJsonObject("mw/gestures")?.let { obj ->
            runCatching {
                Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    .decodeFromJsonElement<GesturePrefs>(obj)
            }.getOrNull()
        } ?: GesturePrefs()
        gSeek = gestures.seek
        gLongPress = gestures.longPress
        gDoubleTap = gestures.doubleTap
        gBrightness = gestures.brightness
        gVolume = gestures.volume
        gZoom = gestures.zoom
        gLongPressSpeed = gestures.longPressSpeed
        gDoubleTapMs = gestures.doubleTapMs.toFloat()
        gSeekSensitivity = gestures.seekSensitivity
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("服务器连接", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("服务器地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val normalized = ApiClient.normalize(urlInput)
                    ApiClient.setBaseUrl(normalized)
                    val ok = runCatching { ApiClient.api.config() }.isSuccess
                    urlMessage = if (ok) "连接成功：${runCatching { ApiClient.api.config().version }.getOrDefault("")}" else "连接失败"
                    if (ok) {
                        repo.prefs.setServerUrl(normalized)
                        repo.loadState()
                        runCatching { ApiClient.api.settings() }.onSuccess { settings = it }
                    }
                }
            }) { Text("测试并保存") }
            TextButton(onClick = onDisconnect) { Text("切换服务器") }
        }
        if (urlMessage.isNotEmpty()) {
            Text(urlMessage, color = if (urlMessage.startsWith("连接成功")) Accent else Danger, style = MaterialTheme.typography.labelSmall)
        }

        Divider()
        Text("服务端设置", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("监听地址") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("端口") },
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ffmpeg,
                onValueChange = { ffmpeg = it },
                label = { Text("ffmpeg 路径") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                scope.launch {
                    toolMessage = runCatching { ApiClient.api.testTool(TestToolBody(ffmpeg, "ffmpeg")).version }
                        .getOrElse { "检测失败" }
                }
            }) { Text("检测") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ffprobe,
                onValueChange = { ffprobe = it },
                label = { Text("ffprobe 路径") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                scope.launch {
                    toolMessage = runCatching { ApiClient.api.testTool(TestToolBody(ffprobe, "ffprobe")).version }
                        .getOrElse { "检测失败" }
                }
            }) { Text("检测") }
        }
        if (toolMessage.isNotEmpty()) {
            Text(toolMessage, color = TextDim, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("服务端转码", modifier = Modifier.weight(1f))
            Switch(checked = transcode, onCheckedChange = { transcode = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("转码硬件", modifier = Modifier.weight(1f))
            TextButton(onClick = { encoderMenu = true }) { Text(ENCODER_LABELS[encoder] ?: encoder) }
            DropdownMenu(expanded = encoderMenu, onDismissRequest = { encoderMenu = false }) {
                for ((key, label) in ENCODER_LABELS) {
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        encoderMenu = false
                        encoder = key
                    })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("控制台访问日志", modifier = Modifier.weight(1f))
            Switch(checked = accessLog, onCheckedChange = { accessLog = it })
        }
        Button(onClick = {
            scope.launch {
                runCatching {
                    ApiClient.api.updateSettings(
                        SettingsUpdateBody(
                            host = host,
                            port = port.toIntOrNull(),
                            ffmpeg = ffmpeg,
                            ffprobe = ffprobe,
                            transcode = transcode,
                            transcodeEncoder = encoder,
                            accessLog = accessLog,
                        )
                    )
                }.onSuccess {
                    toolMessage = if (it.restartRequired) "已保存，监听地址/端口需重启服务生效" else "已保存"
                }.onFailure { toolMessage = "保存失败：${it.message}" }
            }
        }) { Text("保存服务端设置") }
        if (settings != null) {
            Text(
                "数据目录：${settings?.dataDir}",
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Divider()
        Text("播放器默认值", style = MaterialTheme.typography.titleMedium)
        Text("默认音量：${volume.toInt()}", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..100f, onValueChangeFinished = {
            repo.set("mw/volume", volume.toInt().toString())
        })
        Text("默认倍速：${String.format(java.util.Locale.US, "%.1f", speed / 10f)}x", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 5f..40f, onValueChangeFinished = {
            repo.set("mw/speed", speed.toInt().toString())
        })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("弹幕", modifier = Modifier.weight(1f))
            Switch(checked = danmaku, onCheckedChange = {
                danmaku = it
                repo.setBool("mw/danmaku", it)
            })
        }
        Text("弹幕横穿时间：${(danmakuSpeed / 1000).toInt()} 秒", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = danmakuSpeed, onValueChange = { danmakuSpeed = it }, valueRange = 3000f..15000f, onValueChangeFinished = {
            repo.set("mw/danmakuSpeed", danmakuSpeed.toInt().toString())
        })

        Divider()
        Text("播放器手势", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("左右滑动调进度", modifier = Modifier.weight(1f))
            Switch(checked = gSeek, onCheckedChange = {
                gSeek = it
                saveGestures()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("长按加速播放", modifier = Modifier.weight(1f))
            Switch(checked = gLongPress, onCheckedChange = {
                gLongPress = it
                saveGestures()
            })
        }
        Text("长按倍速：${String.format(java.util.Locale.US, "%.1f", gLongPressSpeed)}x", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = gLongPressSpeed, onValueChange = { gLongPressSpeed = it }, valueRange = 1.5f..4f, onValueChangeFinished = { saveGestures() })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("双击快进/快退", modifier = Modifier.weight(1f))
            Switch(checked = gDoubleTap, onCheckedChange = {
                gDoubleTap = it
                saveGestures()
            })
        }
        Text("双击步长：${(gDoubleTapMs / 1000).toInt()} 秒", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = gDoubleTapMs, onValueChange = { gDoubleTapMs = it }, valueRange = 5000f..30000f, onValueChangeFinished = { saveGestures() })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("左半屏滑动调亮度", modifier = Modifier.weight(1f))
            Switch(checked = gBrightness, onCheckedChange = {
                gBrightness = it
                saveGestures()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("右半屏滑动调音量", modifier = Modifier.weight(1f))
            Switch(checked = gVolume, onCheckedChange = {
                gVolume = it
                saveGestures()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("双指缩放/平移画面", modifier = Modifier.weight(1f))
            Switch(checked = gZoom, onCheckedChange = {
                gZoom = it
                saveGestures()
            })
        }
        Text("进度拖动灵敏度：${String.format(java.util.Locale.US, "%.1f", gSeekSensitivity)}x", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(value = gSeekSensitivity, onValueChange = { gSeekSensitivity = it }, valueRange = 0.3f..3f, onValueChangeFinished = { saveGestures() })

        Divider()
        Text("列表", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("每页条数", modifier = Modifier.weight(1f))
            TextButton(onClick = { pageSizeMenu = true }) { Text("$pageSize / 页") }
            DropdownMenu(expanded = pageSizeMenu, onDismissRequest = { pageSizeMenu = false }) {
                for (value in listOf(10, 20, 30, 50, 100)) {
                    DropdownMenuItem(text = { Text("$value / 页") }, onClick = {
                        pageSizeMenu = false
                        pageSize = value
                        repo.set("mw/pageSize", value.toString())
                    })
                }
            }
        }

        Divider()
        Text("关于", style = MaterialTheme.typography.titleMedium)
        val version = remember { mutableStateOf("") }
        LaunchedEffect(Unit) { runCatching { ApiClient.api.config().version }.onSuccess { version.value = it } }
        Text("Media Web 服务端版本：${version.value}", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Text("Android 客户端 1.1.3", color = TextDim, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(20.dp))
    }
}
