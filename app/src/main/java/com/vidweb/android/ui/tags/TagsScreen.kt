package com.vidweb.android.ui.tags

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.MediaItem
import com.vidweb.android.data.PathsBody
import com.vidweb.android.data.Repo
import com.vidweb.android.data.Tag
import com.vidweb.android.data.TagCreateBody
import com.vidweb.android.data.TagUpdateBody
import com.vidweb.android.player.PlayerLauncher
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.EmptyHint
import com.vidweb.android.ui.common.LoadingBox
import com.vidweb.android.ui.common.MediaRow
import com.vidweb.android.ui.common.PromptDialog
import com.vidweb.android.ui.common.toEntry
import com.vidweb.android.ui.theme.TextDim
import kotlinx.coroutines.launch

private val TAG_COLORS = listOf("#00a4dc", "#e5534b", "#f0a020", "#4caf50", "#9c27b0", "#607d8b")

@Composable
fun TagsScreen(repo: Repo, onOpenTag: (Int, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var newName by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Int?>(null) }
    var parentMenu by remember { mutableStateOf(false) }
    var menuTag by remember { mutableStateOf<Tag?>(null) }
    var renameTag by remember { mutableStateOf<Tag?>(null) }

    suspend fun load() {
        loading = true
        runCatching { ApiClient.api.tags().items }.onSuccess { tags = it }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    val children = remember(tags) { tags.groupBy { it.parentId ?: 0 } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("新标签名称") },
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { parentMenu = true }) {
                        Text(tags.firstOrNull { it.id == parentId }?.name ?: "无父标签")
                    }
                    DropdownMenu(expanded = parentMenu, onDismissRequest = { parentMenu = false }) {
                        DropdownMenuItem(text = { Text("无父标签") }, onClick = {
                            parentMenu = false
                            parentId = null
                        })
                        for (tag in tags) {
                            DropdownMenuItem(text = { Text(tag.name) }, onClick = {
                                parentMenu = false
                                parentId = tag.id
                            })
                        }
                    }
                }
                Button(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            runCatching { ApiClient.api.createTag(TagCreateBody(name, parentId)) }
                            newName = ""
                            load()
                        }
                    }
                }) { Text("新建") }
            }
        }

        Text(
            "共 ${tags.size} 个标签",
            color = TextDim,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        )

        when {
            loading -> LoadingBox()
            tags.isEmpty() -> EmptyHint("暂无标签，可在上方创建")
            else -> LazyColumn(Modifier.weight(1f)) {
                val rows = buildList {
                    fun walk(tag: Tag, depth: Int) {
                        add(tag to depth)
                        if (expanded.contains(tag.id)) {
                            for (child in children[tag.id].orEmpty()) walk(child, depth + 1)
                        }
                    }
                    for (root in children[0].orEmpty()) walk(root, 0)
                }
                items(rows, key = { it.first.id }) { (tag, depth) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTag(tag.id, tag.name) }
                            .padding(start = (12 + depth * 20).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val hasChildren = children[tag.id].orEmpty().isNotEmpty()
                        if (hasChildren) {
                            IconButton(
                                onClick = {
                                    expanded = if (expanded.contains(tag.id)) expanded - tag.id else expanded + tag.id
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    if (expanded.contains(tag.id)) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            Spacer(Modifier.width(24.dp))
                        }
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(parseColor(tag.color)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(tag.name, modifier = Modifier.weight(1f))
                        Text(
                            "${tag.subtreeCount}",
                            color = TextDim,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        TextButton(onClick = { menuTag = tag }) { Text("⋯") }
                    }
                }
            }
        }
    }

    menuTag?.let { tag ->
        ActionSheet(
            title = tag.name,
            actions = buildList {
                add("重命名" to { renameTag = tag })
                add("设置颜色" to {
                    scope.launch {
                        val index = TAG_COLORS.indexOfFirst { it.equals(tag.color, ignoreCase = true) }
                        val color = TAG_COLORS[(index + 1) % TAG_COLORS.size]
                        runCatching { ApiClient.api.updateTag(tag.id, TagUpdateBody(color = color)) }
                        load()
                    }
                })
                add("删除标签" to {
                    scope.launch {
                        runCatching { ApiClient.api.deleteTag(tag.id) }
                        load()
                    }
                })
            },
            onDismiss = { menuTag = null },
        )
    }

    renameTag?.let { tag ->
        PromptDialog(
            title = "重命名标签",
            initial = tag.name,
            onDismiss = { renameTag = null },
            onConfirm = { name ->
                renameTag = null
                if (name.isNotBlank()) {
                    scope.launch {
                        runCatching { ApiClient.api.updateTag(tag.id, TagUpdateBody(name = name)) }
                        load()
                    }
                }
            },
        )
    }
}

@Composable
fun TagFilesScreen(repo: Repo, tagId: Int, tagName: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tagId) {
        loading = true
        runCatching {
            val paths = ApiClient.api.tagFiles(tagId).paths
            if (paths.isEmpty()) emptyList() else ApiClient.api.filesInfo(PathsBody(paths)).items
        }.onSuccess { items = it }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
            Text(tagName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${items.size} 个", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        when {
            loading -> LoadingBox()
            items.isEmpty() -> EmptyHint("该标签下暂无视频")
            else -> LazyColumn(Modifier.weight(1f)) {
                items(items, key = { it.path }) { item ->
                    MediaRow(
                        entry = item.toEntry(),
                        onClick = {
                            val paths = items.map { it.path }
                            PlayerLauncher.open(context, paths, paths.indexOf(item.path).coerceAtLeast(0))
                        },
                    )
                }
            }
        }
    }
}

private fun parseColor(value: String?): Color {
    if (value.isNullOrBlank()) return Color(0xFF607D8B)
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color(0xFF607D8B))
}
