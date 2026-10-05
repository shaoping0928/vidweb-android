package com.vidweb.android.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.FileDetail
import com.vidweb.android.data.MediaItem
import com.vidweb.android.data.RecentItem
import com.vidweb.android.data.Repo
import com.vidweb.android.data.Tag
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.Border
import com.vidweb.android.ui.theme.Danger
import com.vidweb.android.ui.theme.Surface2
import com.vidweb.android.ui.theme.TextDim
import com.vidweb.android.util.Format
import kotlinx.coroutines.launch

/* ---------------------------------------------------------------- models */

@Immutable
data class MediaEntry(
    val path: String,
    val name: String,
    val subtitle: String,
    val hasThumb: Boolean,
    val progress: Float,
    val exists: Boolean,
    val tags: List<String> = emptyList(),
)

fun MediaItem.toEntry(): MediaEntry = MediaEntry(
    path = path,
    name = name,
    subtitle = buildString {
        if (durationMs > 0) append(Format.time(durationMs))
        if (durationMs > 0 && size > 0) append(" · ")
        if (size > 0) append(Format.bytes(size))
        if (!exists) append(if (isEmpty()) "失效" else " · 失效")
    },
    hasThumb = hasThumb,
    progress = Format.percent(lastPositionMs, durationMs),
    exists = exists,
    tags = tags,
)

fun RecentItem.toEntry(): MediaEntry = MediaEntry(
    path = path,
    name = name,
    subtitle = buildString {
        if (durationMs > 0) append(Format.time(durationMs))
        if (playCount > 0) {
            if (isNotEmpty()) append(" · ")
            append("播放 $playCount 次")
        }
        if (lastPlayedAt > 0) {
            if (isNotEmpty()) append(" · ")
            append(Format.relative(lastPlayedAt))
        }
    },
    hasThumb = hasThumb,
    progress = Format.percent(lastPositionMs, durationMs),
    exists = exists,
)

/* ---------------------------------------------------------------- basics */

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextDim, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Pager(
    page: Int,
    totalPages: Int,
    totalItems: Int,
    pageSize: Int,
    itemLabel: String = "条",
    onPage: (Int) -> Unit,
    onPageSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sizeMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("共 $totalItems $itemLabel", color = TextDim, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { if (page > 1) onPage(page - 1) }, enabled = page > 1) {
            Text("上一页")
        }
        Text("$page / $totalPages", color = TextDim, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { if (page < totalPages) onPage(page + 1) }, enabled = page < totalPages) {
            Text("下一页")
        }
        Box {
            TextButton(onClick = { sizeMenu = true }) { Text("$pageSize / 页") }
            DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                for (value in listOf(10, 20, 30, 50, 100)) {
                    DropdownMenuItem(
                        text = { Text("$value / 页") },
                        onClick = {
                            sizeMenu = false
                            onPageSize(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun Thumbnail(
    path: String,
    hasThumb: Boolean,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
) {
    var attempt by remember(path) { androidx.compose.runtime.mutableStateOf(0) }
    var loaded by remember(path) { androidx.compose.runtime.mutableStateOf(hasThumb) }
    val context = LocalContext.current

    LaunchedEffect(path, attempt, loaded) {
        if (!loaded && attempt < 30) {
            kotlinx.coroutines.delay(4000)
            attempt += 1
        }
    }

    val request = remember(path, attempt) {
        coil.request.ImageRequest.Builder(context)
            .data(ApiClient.thumbUrl(path, attempt.toLong()))
            .crossfade(false)
            .build()
    }

    Box(modifier.background(Surface2, RoundedCornerShape(6.dp))) {
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = TextDim)
            }
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            onSuccess = { loaded = true },
            onError = { loaded = false },
            modifier = Modifier.fillMaxSize(),
        )
        if (progress > 0.01f) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = Accent,
                trackColor = Color.Transparent,
            )
        }
    }
}

/* ---------------------------------------------------------------- rows */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaRow(
    entry: MediaEntry,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) Accent.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (selectionMode) {
            SelectionCircle(selected)
        }
        Thumbnail(
            path = entry.path,
            hasThumb = entry.hasThumb,
            progress = entry.progress,
            modifier = Modifier.width(72.dp).height(41.dp).clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (entry.exists) MaterialTheme.colorScheme.onSurface else TextDim,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (entry.subtitle.isNotEmpty()) {
                Text(
                    entry.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun SelectionCircle(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) Accent else Color.Transparent)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        if (!selected) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(0.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                ) {}
            }
        } else {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThumbCard(
    entry: MediaEntry,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))) {
            Thumbnail(
                path = entry.path,
                hasThumb = entry.hasThumb,
                progress = entry.progress,
                modifier = Modifier.fillMaxSize(),
            )
            if (selectionMode) {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    SelectionCircle(selected)
                }
            }
        }
        Text(
            entry.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (entry.exists) MaterialTheme.colorScheme.onSurface else TextDim,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        if (entry.subtitle.isNotEmpty()) {
            Text(
                entry.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp),
            )
        }
    }
}

/* ---------------------------------------------------------------- dialogs */

@Composable
fun PromptDialog(
    title: String,
    initial: String = "",
    confirmLabel: String = "确定",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String = "",
    confirmLabel: String = "确定",
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { if (text.isNotEmpty()) Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) Danger else Accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun ItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<Pair<String, () -> Unit>>,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        for ((label, action) in actions) {
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onDismiss()
                    action()
                },
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    title: String,
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            for ((label, action) in actions) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            action()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- tag picker */

@Composable
fun TagPickerDialog(
    tags: List<Tag>,
    initialSelected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateOf(initialSelected.toSet()) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("新标签") },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            selected.value = selected.value + name
                            newName = ""
                        }
                    }) { Text("添加") }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(240.dp)) {
                    items(tags) { tag ->
                        val checked = selected.value.contains(tag.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected.value = if (checked) selected.value - tag.name
                                    else selected.value + tag.name
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SelectionCircle(checked)
                            Spacer(Modifier.width(10.dp))
                            Text(tag.name, modifier = Modifier.weight(1f))
                            Text("${tag.subtreeCount}", color = TextDim, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.value.toList()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/* ---------------------------------------------------------------- detail sheet */

@Composable
fun DetailDialog(
    path: String,
    repo: Repo,
    onDismiss: () -> Unit,
    onPlay: (String) -> Unit,
) {
    var detail by remember { mutableStateOf<FileDetail?>(null) }
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var inPlaylist by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(path) {
        runCatching { ApiClient.api.fileDetail(path) }.onSuccess { detail = it }
        runCatching { ApiClient.api.tags().items }.onSuccess { tags = it }
        runCatching { repo.playlistPaths() }.onSuccess { inPlaylist = it.contains(path) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail?.name ?: "详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                val record = detail?.record
                val lines = buildList {
                    add("路径：$path")
                    if (record != null) {
                        if (record.durationMs > 0) add("时长：${Format.time(record.durationMs)}")
                        if (record.width > 0) add("分辨率：${record.width}×${record.height}")
                        if (record.size > 0) add("大小：${Format.bytes(record.size)}")
                        add("播放：${record.playCount} 次 · 上次 ${Format.relative(record.lastPlayedAt)}")
                        if (record.lastPositionMs > 0) add("续播点：${Format.time(record.lastPositionMs)}")
                    }
                    add("评论：${detail?.commentCount ?: 0} 条")
                }
                for (line in lines) {
                    Text(line, style = MaterialTheme.typography.bodySmall, color = TextDim, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("标签", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (tag in detail?.tags.orEmpty()) {
                        Surface(color = Surface2, shape = RoundedCornerShape(12.dp)) {
                            Text(tag.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (detail?.tags.isNullOrEmpty()) {
                        Text("无", color = TextDim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onPlay(path) }) { Text("播放") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showTagPicker = true }) { Text("标签") }
                TextButton(onClick = {
                    scope.launch {
                        if (inPlaylist) repo.removeFromPlaylist(listOf(path))
                        else repo.addToPlaylist(listOf(path))
                        inPlaylist = !inPlaylist
                    }
                }) { Text(if (inPlaylist) "移出列表" else "加入列表") }
            }
        },
    )

    if (showTagPicker) {
        TagPickerDialog(
            tags = tags,
            initialSelected = detail?.tags?.map { it.name }.orEmpty(),
            onDismiss = { showTagPicker = false },
            onConfirm = { names ->
                showTagPicker = false
                scope.launch {
                    runCatching { ApiClient.api.setFileTags(com.vidweb.android.data.FileTagsBody(path, names)) }
                        .onSuccess { detail = detail?.copy(tags = it.tags) }
                }
            },
        )
    }
}
