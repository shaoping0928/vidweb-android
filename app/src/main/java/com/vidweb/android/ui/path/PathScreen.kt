package com.vidweb.android.ui.path

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.BrowseEntry
import com.vidweb.android.data.BrowseResponse
import com.vidweb.android.data.DirItem
import com.vidweb.android.data.DriveItem
import com.vidweb.android.data.PathBody
import com.vidweb.android.data.Repo
import com.vidweb.android.data.ScanBody
import com.vidweb.android.data.ScanStatus
import com.vidweb.android.player.PlayerLauncher
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.DetailDialog
import com.vidweb.android.ui.common.EmptyHint
import com.vidweb.android.ui.common.LoadingBox
import com.vidweb.android.ui.common.MediaEntry
import com.vidweb.android.ui.common.MediaRow
import com.vidweb.android.ui.common.Pager
import com.vidweb.android.ui.common.PromptDialog
import com.vidweb.android.ui.common.toEntry
import com.vidweb.android.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PathScreen(repo: Repo) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var drives by remember { mutableStateOf<List<DriveItem>>(emptyList()) }
    var dirs by remember { mutableStateOf<List<DirItem>>(emptyList()) }
    var browse by remember { mutableStateOf<BrowseResponse?>(null) }
    var pathInput by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var onlyVideo by remember { mutableStateOf(repo.getBool("mw/onlyVideo", false)) }
    var sort by remember { mutableStateOf(repo.getInt("mw/sort", 0)) }
    var loading by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf(1) }
    var showDirs by remember { mutableStateOf(false) }
    var showAddDir by remember { mutableStateOf(false) }
    var addDirParent by remember { mutableStateOf("") }
    var scanJob by remember { mutableStateOf<ScanStatus?>(null) }
    var scanDialog by remember { mutableStateOf(false) }
    var menuEntry by remember { mutableStateOf<BrowseEntry?>(null) }
    var detailPath by remember { mutableStateOf<String?>(null) }

    suspend fun load(target: String, remember: Boolean = true) {
        loading = true
        runCatching { ApiClient.api.browse(target, filter, onlyVideo, sort) }
            .onSuccess {
                browse = it
                pathInput = it.path
                if (remember) repo.set("mw/lastPath", it.path)
            }
        loading = false
    }

    suspend fun reloadDirs() {
        runCatching { ApiClient.api.dirs() }.onSuccess { dirs = it.items }
    }

    LaunchedEffect(Unit) {
        runCatching { ApiClient.api.drives() }.onSuccess { drives = it.roots }
        reloadDirs()
        val last = repo.get("mw/lastPath")
        load(last.ifBlank { drives.firstOrNull()?.path ?: "" })
    }

    LaunchedEffect(filter, onlyVideo, sort) {
        val target = browse?.path ?: return@LaunchedEffect
        page = 1
        load(target)
    }

    LaunchedEffect(scanJob?.state) {
        val job = scanJob ?: return@LaunchedEffect
        if (job.state == "running" || job.state == "queued") {
            delay(800)
            runCatching { ApiClient.api.scanStatus(job.jobId) }.onSuccess { scanJob = it }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            var driveMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { driveMenu = true }) {
                    Text(drives.firstOrNull { it.path == browse?.path }?.name ?: "根目录", maxLines = 1)
                }
                DropdownMenu(expanded = driveMenu, onDismissRequest = { driveMenu = false }) {
                    for (drive in drives) {
                        DropdownMenuItem(
                            text = { Text(drive.name) },
                            onClick = {
                                driveMenu = false
                                scope.launch { load(drive.path) }
                            },
                        )
                    }
                }
            }
            IconButton(onClick = {
                val parent = browse?.parent
                if (!parent.isNullOrBlank() && parent != browse?.path) scope.launch { load(parent) }
            }) { Icon(Icons.Filled.ArrowUpward, contentDescription = "上一级") }
            OutlinedTextField(
                value = pathInput,
                onValueChange = { pathInput = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入路径，如 D:\\Media") },
            )
            TextButton(onClick = { scope.launch { load(pathInput) } }) { Text("跳转") }
            IconButton(onClick = { scope.launch { load(browse?.path ?: pathInput) } }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { Text("文件名过滤") },
            )
            FilterChip(
                selected = onlyVideo,
                onClick = {
                    onlyVideo = !onlyVideo
                    repo.setBool("mw/onlyVideo", onlyVideo)
                },
                label = { Text("仅视频") },
            )
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Text(listOf("名称 ↑", "名称 ↓", "大小 ↑", "大小 ↓", "时间 ↑", "时间 ↓")[sort])
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    for ((index, label) in listOf("名称 ↑", "名称 ↓", "大小 ↑", "大小 ↓", "时间 ↑", "时间 ↓").withIndex()) {
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sortMenu = false
                                sort = index
                                repo.set("mw/sort", index.toString())
                            },
                        )
                    }
                }
            }
            TextButton(onClick = { showDirs = true }) { Text("路径库(${dirs.size})") }
        }

        val response = browse
        if (loading && response == null) {
            LoadingBox()
        } else if (response == null) {
            EmptyHint("无法读取目录")
        } else {
            val folders = response.entries.filter { it.isdir }
            val files = response.entries.filter { !it.isdir }
            val videos = files.filter { it.isVideo }
            val pageSize = repo.pageSize()
            val allRows: List<BrowseEntry> = folders + files
            val totalPages = ((allRows.size + pageSize - 1) / pageSize).coerceAtLeast(1)
            val currentPage = page.coerceIn(1, totalPages)
            val rows = allRows.drop((currentPage - 1) * pageSize).take(pageSize)
            val videoPaths = videos.map { it.path }

            LazyColumn(Modifier.weight(1f)) {
                items(rows, key = { it.path }) { entry ->
                    if (entry.isdir) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { load(entry.path) } }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    } else {
                        val item = entry.toEntry()
                        MediaRow(
                            entry = item,
                            onClick = {
                                if (entry.isVideo) {
                                    val index = videoPaths.indexOf(entry.path).coerceAtLeast(0)
                                    PlayerLauncher.open(context, videoPaths, index)
                                }
                            },
                            onLongClick = { menuEntry = entry },
                        )
                    }
                }
            }
            Pager(
                page = currentPage,
                totalPages = totalPages,
                totalItems = allRows.size,
                pageSize = pageSize,
                onPage = { page = it },
                onPageSize = {
                    repo.set("mw/pageSize", it.toString())
                    page = 1
                },
            )
        }
    }

    if (showDirs) {
        ActionSheet(
            title = "路径库",
            actions = buildList {
                add("添加当前目录" to {
                    val path = browse?.path ?: return@to
                    scope.launch {
                        runCatching { ApiClient.api.addDir(PathBody(path)) }
                            .onSuccess { dirs = it.items }
                    }
                })
                for (dir in dirs) {
                    add("扫描入库：${dir.path}" to {
                        scope.launch {
                            runCatching { ApiClient.api.scan(ScanBody(dirId = dir.id)) }
                                .onSuccess {
                                    scanDialog = true
                                    scanJob = ScanStatus(jobId = it.jobId, state = "queued")
                                }
                        }
                    })
                    add("移除目录：${dir.path}" to {
                        scope.launch {
                            runCatching { ApiClient.api.deleteDir(dir.id) }
                                .onSuccess { dirs = it.items }
                        }
                    })
                }
                add("手动添加路径…" to {
                    addDirParent = ""
                    showAddDir = true
                })
            },
            onDismiss = { showDirs = false },
        )
    }

    if (showAddDir) {
        PromptDialog(
            title = "添加路径库目录",
            initial = browse?.path ?: "",
            onDismiss = { showAddDir = false },
            onConfirm = { value ->
                showAddDir = false
                if (value.isNotBlank()) {
                    scope.launch {
                        runCatching { ApiClient.api.addDir(PathBody(value)) }
                            .onSuccess { dirs = it.items }
                    }
                }
            },
        )
    }

    if (scanDialog) {
        val job = scanJob
        AlertDialog(
            onDismissRequest = {},
            title = { Text("扫描中") },
            text = {
                Column {
                    if (job != null) {
                        Text(job.path, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text("状态：${job.state}  已处理 ${job.processed}/${job.total}  新增 ${job.added}")
                        Spacer(Modifier.height(8.dp))
                        if (job.state == "running" || job.state == "queued") {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (!job.error.isNullOrBlank()) {
                            Text(job.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                val finished = job == null || job.state in listOf("done", "cancelled", "error")
                if (finished) {
                    TextButton(onClick = {
                        scanDialog = false
                        scope.launch { reloadDirs() }
                    }) { Text("完成") }
                } else {
                    TextButton(onClick = {
                        val id = job?.jobId ?: return@TextButton
                        scope.launch {
                            runCatching { ApiClient.api.scanCancel(com.vidweb.android.data.JobBody(id)) }
                        }
                    }) { Text("取消扫描") }
                }
            },
        )
    }

    menuEntry?.let { entry ->
        val item = entry.toEntry()
        ActionSheet(
            title = entry.name,
            actions = listOf(
                "播放" to {
                    val response = browse
                    val videos = response?.entries?.filter { it.isVideo }?.map { it.path } ?: emptyList()
                    val index = videos.indexOf(entry.path).coerceAtLeast(0)
                    PlayerLauncher.open(context, videos, index)
                },
                "加入播放列表" to {
                    scope.launch { runCatching { repo.addToPlaylist(listOf(entry.path)) } }
                },
                "标签 / 详情" to { detailPath = entry.path },
                "在资源管理器中显示" to {
                    scope.launch { runCatching { ApiClient.api.explore(PathBody(entry.path)) } }
                },
            ),
            onDismiss = { menuEntry = null },
        )
    }

    detailPath?.let { path ->
        DetailDialog(
            path = path,
            repo = repo,
            onDismiss = { detailPath = null },
            onPlay = { p ->
                PlayerLauncher.open(context, listOf(p), 0)
            },
        )
    }
}

private fun BrowseEntry.toEntry(): MediaEntry = MediaEntry(
    path = path,
    name = name,
    subtitle = buildString {
        if (size > 0) append(Format.bytes(size))
        if (mtime > 0) {
            if (isNotEmpty()) append(" · ")
            append(Format.relative(mtime))
        }
    },
    hasThumb = hasThumb,
    progress = 0f,
    exists = true,
)
