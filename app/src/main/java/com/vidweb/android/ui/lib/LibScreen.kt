package com.vidweb.android.ui.lib

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.vidweb.android.data.CollectionBody
import com.vidweb.android.data.CollectionItem
import com.vidweb.android.data.CollectionUpdateBody
import com.vidweb.android.data.MediaItem
import com.vidweb.android.data.PathsBody
import com.vidweb.android.data.PlaylistQueryRequest
import com.vidweb.android.data.Repo
import com.vidweb.android.data.Tag
import com.vidweb.android.data.TagsBatchBody
import com.vidweb.android.player.PlayerLauncher
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.DetailDialog
import com.vidweb.android.ui.common.EmptyHint
import com.vidweb.android.ui.common.LoadingBox
import com.vidweb.android.ui.common.MediaRow
import com.vidweb.android.ui.common.Pager
import com.vidweb.android.ui.common.PromptDialog
import com.vidweb.android.ui.common.TagPickerDialog
import com.vidweb.android.ui.common.ThumbCard
import com.vidweb.android.ui.common.toEntry
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.TextDim
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val STATUS_OPTIONS = listOf(
    "all" to "全部", "new" to "未播放", "progress" to "有进度", "played" to "已播放",
    "missing" to "失效", "broken" to "损坏",
)
private val DURATION_OPTIONS = listOf(
    "all" to "全部", "short" to "10 分钟内", "medium" to "10–30 分钟", "long" to "30 分钟以上", "unknown" to "未知时长",
)
private val SORT_OPTIONS = listOf(
    "name" to "名称 ↑", "name_desc" to "名称 ↓", "recent" to "最近播放", "plays" to "播放次数",
    "duration" to "时长", "size" to "文件大小", "mtime" to "修改时间",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibScreen(repo: Repo) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var search by remember { mutableStateOf("") }
    var statuses by remember { mutableStateOf<Set<String>>(emptySet()) }
    var duration by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("name") }
    var tagMatchAll by remember { mutableStateOf(false) }
    var tagIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var treeMode by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var tree by remember { mutableStateOf<List<com.vidweb.android.data.TreeNode>>(emptyList()) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    val treeFiles = remember { mutableStateMapOf<String, List<MediaItem>>() }
    var treeLoading by remember { mutableStateOf<Set<String>>(emptySet()) }
    var treeMenuNode by remember { mutableStateOf<com.vidweb.android.data.TreeNode?>(null) }
    var total by remember { mutableStateOf(0) }
    var filtered by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var showFilters by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var detailPath by remember { mutableStateOf<String?>(null) }
    var menuEntry by remember { mutableStateOf<MediaItem?>(null) }
    var collectionMenu by remember { mutableStateOf<CollectionItem?>(null) }
    var showSaveCollection by remember { mutableStateOf(false) }
    var renameCollection by remember { mutableStateOf<CollectionItem?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var allTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val pageSize = repo.pageSize()

    fun buildRequest(folder: String? = null, offset: Int = 0, limit: Int = pageSize, tree: Boolean = false): PlaylistQueryRequest =
        PlaylistQueryRequest(
            search = search,
            status = "all",
            statuses = statuses.toList().ifEmpty { null },
            duration = duration,
            tagIds = tagIds.toList(),
            tagMatchAll = tagMatchAll,
            sort = sort,
            tree = tree,
            folder = folder,
            offset = offset,
            limit = limit,
        )

    suspend fun load() {
        loading = true
        val response = runCatching {
            ApiClient.api.queryPlaylist(
                buildRequest(
                    offset = (page - 1) * pageSize,
                    tree = treeMode,
                )
            )
        }.getOrNull()
        if (response != null) {
            tree = response.tree
            items = response.items
            total = response.total
            filtered = response.filtered
            if (treeMode) {
                expanded = emptySet()
                treeFiles.clear()
                treeLoading = emptySet()
            }
        }
        loading = false
    }

    fun toggleTreeNode(node: com.vidweb.android.data.TreeNode) {
        val isExpanded = expanded.contains(node.path)
        expanded = if (isExpanded) expanded - node.path else expanded + node.path
        if (!isExpanded && !treeFiles.containsKey(node.path)) {
            treeLoading = treeLoading + node.path
            scope.launch {
                val response = runCatching {
                    ApiClient.api.queryPlaylist(buildRequest(folder = node.path, limit = 500))
                }.getOrNull()
                treeFiles[node.path] = response?.items ?: emptyList()
                treeLoading = treeLoading - node.path
            }
        }
    }

    suspend fun playFolder(node: com.vidweb.android.data.TreeNode) {
        val files = treeFiles[node.path] ?: runCatching {
            ApiClient.api.queryPlaylist(buildRequest(folder = node.path, limit = 500))
        }.getOrNull()?.items.orEmpty()
        treeFiles[node.path] = files
        val paths = files.map { it.path }
        if (paths.isNotEmpty()) PlayerLauncher.open(context, paths, 0)
    }

    suspend fun loadAux() {
        runCatching { ApiClient.api.tags().items }.onSuccess { allTags = it }
        runCatching { ApiClient.api.collections().items }.onSuccess { collections = it }
        runCatching { ApiClient.api.collectionCounts().counts }.onSuccess { counts = it }
    }

    LaunchedEffect(Unit) { loadAux() }
    LaunchedEffect(search, statuses, duration, sort, tagMatchAll, tagIds, treeMode, page) {
        load()
    }

    fun currentFiltersJson(): kotlinx.serialization.json.JsonObject {
        val names = allTags.filter { tagIds.contains(it.id) }.map { it.name }
        return buildJsonObject {
            put("search", search)
            put("status", statuses.singleOrNull() ?: "all")
            put("statuses", JsonArray(statuses.map { JsonPrimitive(it) }))
            put("duration", duration)
            put("sort", sort)
            put("tagMode", if (tagMatchAll) "all" else "any")
            put("tagNames", JsonArray(names.map { JsonPrimitive(it) }))
        }
    }

    fun applyCollection(item: CollectionItem) {
        val filters = item.filters
        search = filters["search"]?.jsonPrimitive?.contentOrNull() ?: ""
        val stored = (filters["statuses"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull() }.orEmpty()
        val legacy = filters["status"]?.jsonPrimitive?.contentOrNull()
        statuses = when {
            stored.isNotEmpty() -> stored.toSet()
            !legacy.isNullOrBlank() && legacy != "all" -> setOf(legacy)
            else -> emptySet()
        }
        duration = filters["duration"]?.jsonPrimitive?.contentOrNull() ?: "all"
        sort = filters["sort"]?.jsonPrimitive?.contentOrNull() ?: "name"
        tagMatchAll = (filters["tagMode"]?.jsonPrimitive?.contentOrNull() ?: "any") == "all"
        val names = (filters["tagNames"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull() } ?: emptyList()
        tagIds = allTags.filter { names.contains(it.name) }.map { it.id }.toSet()
        page = 1
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    page = 1
                },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("筛选文件名") },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showFilters = true }) {
                Icon(Icons.Filled.Tune, contentDescription = "筛选")
            }
        }

        if (selectionMode) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("已选 ${selected.size}", color = Accent)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    val visible = if (treeMode) emptyList() else items.map { it.path }
                    selected.clear()
                    selected.addAll(visible)
                }) { Text("全选当前显示") }
                TextButton(onClick = {
                    scope.launch {
                        runCatching { repo.addToPlaylist(selected.toList()) }
                    }
                }) { Text("加入列表") }
                TextButton(onClick = {
                    scope.launch {
                        runCatching { repo.removeFromPlaylist(selected.toList()) }
                    }
                }) { Text("移除") }
                TextButton(onClick = {
                    showTagPicker = true
                }) { Text("标签") }
                IconButton(onClick = {
                    selectionMode = false
                    selected.clear()
                }) { Icon(Icons.Filled.Close, contentDescription = "取消选择") }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                loading && items.isEmpty() && tree.isEmpty() -> LoadingBox()
                treeMode -> {
                    if (tree.isEmpty()) {
                        EmptyHint("没有匹配的视频")
                    } else {
                        val rows = buildList {
                            fun walk(node: com.vidweb.android.data.TreeNode, depth: Int) {
                                add(TreeRow(node = node, file = null, parentPath = "", depth = depth, loading = false))
                                if (expanded.contains(node.path)) {
                                    for (child in node.children) walk(child, depth + 1)
                                    for (file in treeFiles[node.path].orEmpty()) {
                                        add(TreeRow(node = null, file = file, parentPath = node.path, depth = depth + 1, loading = false))
                                    }
                                    if (treeLoading.contains(node.path)) {
                                        add(TreeRow(node = null, file = null, parentPath = node.path, depth = depth + 1, loading = true))
                                    }
                                }
                            }
                            for (node in tree) walk(node, 0)
                        }
                        LazyColumn {
                            items(rows, key = { it.key }) { row ->
                                val indent = (12 + row.depth * 18).dp
                                when {
                                    row.loading -> Text(
                                        "加载中…",
                                        color = TextDim,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(start = indent, top = 10.dp, bottom = 10.dp),
                                    )
                                    row.node != null -> {
                                        val node = row.node
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { toggleTreeNode(node) }
                                                .padding(start = indent, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                if (expanded.contains(node.path)) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = TextDim,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Filled.Folder, contentDescription = null, tint = Accent)
                                            Spacer(Modifier.width(10.dp))
                                            Text(node.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text("${node.count}", color = TextDim, style = MaterialTheme.typography.labelSmall)
                                            IconButton(onClick = { treeMenuNode = node }) {
                                                Icon(Icons.Filled.MoreVert, contentDescription = "操作", tint = TextDim)
                                            }
                                        }
                                    }
                                    row.file != null -> {
                                        val entry = remember(row.file) { row.file.toEntry() }
                                        MediaRow(
                                            entry = entry,
                                            onClick = {
                                                val paths = treeFiles[row.parentPath].orEmpty().map { it.path }
                                                val index = paths.indexOf(row.file.path).coerceAtLeast(0)
                                                PlayerLauncher.open(context, paths, index)
                                            },
                                            onLongClick = { menuEntry = row.file },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items.isEmpty() -> EmptyHint(if (filtered > 0) "当前页无内容" else "没有匹配的视频")
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items, key = { it.path }) { item ->
                            val isSelected = selected.contains(item.path)
                            val entry = remember(item) { item.toEntry() }
                            ThumbCard(
                                entry = entry,
                                selected = isSelected,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        if (isSelected) selected.remove(item.path) else selected.add(item.path)
                                    } else {
                                        val paths = items.map { it.path }
                                        val index = paths.indexOf(item.path).coerceAtLeast(0)
                                        PlayerLauncher.open(context, paths, index)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selected.add(item.path)
                                    } else {
                                        menuEntry = item
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (!treeMode) {
            val totalPages = ((filtered + pageSize - 1) / pageSize).coerceAtLeast(1)
            Pager(
                page = page.coerceIn(1, totalPages),
                totalPages = totalPages,
                totalItems = filtered,
                pageSize = pageSize,
                onPage = { page = it },
                onPageSize = {
                    repo.set("mw/pageSize", it.toString())
                    page = 1
                },
            )
        } else {
            Text(
                "共 $filtered 个视频（树形模式）",
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("筛选", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text("状态（可多选）", style = MaterialTheme.typography.labelMedium, color = TextDim)
                FlowRow(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for ((value, label) in STATUS_OPTIONS) {
                        val selected = if (value == "all") statuses.isEmpty() else value in statuses
                        FilterChip(
                            selected = selected,
                            onClick = {
                                statuses = when {
                                    value == "all" -> emptySet()
                                    value in statuses -> statuses - value
                                    else -> statuses + value
                                }
                                page = 1
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "多选取并集；勾选「未播放 / 有进度 / 已播放」可排除损坏与失效",
                        color = TextDim,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { ApiClient.api.detectBroken() }
                                .onSuccess {
                                    Toast.makeText(context, "已开始检测，进度见任务页", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(context, "检测启动失败", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }) { Text("检测损坏") }
                }
                Text("时长", style = MaterialTheme.typography.labelMedium, color = TextDim)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DURATION_OPTIONS) { (value, label) ->
                        FilterChip(
                            selected = duration == value,
                            onClick = {
                                duration = value
                                page = 1
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Text("排序", style = MaterialTheme.typography.labelMedium, color = TextDim)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SORT_OPTIONS) { (value, label) ->
                        FilterChip(
                            selected = sort == value,
                            onClick = {
                                sort = value
                                page = 1
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Text("标签", style = MaterialTheme.typography.labelMedium, color = TextDim)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !tagMatchAll,
                        onClick = { tagMatchAll = false },
                        label = { Text("任一") },
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = tagMatchAll,
                        onClick = { tagMatchAll = true },
                        label = { Text("全部") },
                    )
                }
                if (allTags.isEmpty()) {
                    Text("暂无标签", color = TextDim, style = MaterialTheme.typography.labelSmall)
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (tag in allTags) {
                            FilterChip(
                                selected = tagIds.contains(tag.id),
                                onClick = {
                                    tagIds = if (tagIds.contains(tag.id)) tagIds - tag.id else tagIds + tag.id
                                    page = 1
                                },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("智能合集", style = MaterialTheme.typography.labelMedium, color = TextDim)
                if (collections.isEmpty()) {
                    Text("暂无合集，可把当前筛选保存为合集", color = TextDim, style = MaterialTheme.typography.labelSmall)
                } else {
                    Column {
                        for (collection in collections) {
                            val count = counts[collection.id.toString()]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            applyCollection(collection)
                                            showFilters = false
                                        },
                                        onLongClick = { collectionMenu = collection },
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(collection.name, modifier = Modifier.weight(1f))
                                if (count != null) {
                                    Text("$count", color = TextDim, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { showSaveCollection = true }) { Text("保存当前筛选为合集") }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("树形显示（不分页）", modifier = Modifier.weight(1f))
                    Switch(
                        checked = treeMode,
                        onCheckedChange = {
                            treeMode = it
                            page = 1
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        search = ""
                        statuses = emptySet()
                        duration = "all"
                        sort = "name"
                        tagIds = emptySet()
                        tagMatchAll = false
                        page = 1
                    }) { Text("重置筛选") }
                    Button(onClick = {
                        scope.launch { runCatching { ApiClient.api.playlistRestore() } }
                    }) { Text("从媒体库恢复") }
                }
            }
        }
    }

    if (showSaveCollection) {
        PromptDialog(
            title = "保存为智能合集",
            onDismiss = { showSaveCollection = false },
            onConfirm = { name ->
                showSaveCollection = false
                if (name.isNotBlank()) {
                    scope.launch {
                        runCatching { ApiClient.api.createCollection(CollectionBody(name, currentFiltersJson())) }
                        loadAux()
                    }
                }
            },
        )
    }

    renameCollection?.let { collection ->
        PromptDialog(
            title = "重命名合集",
            initial = collection.name,
            onDismiss = { renameCollection = null },
            onConfirm = { name ->
                renameCollection = null
                if (name.isNotBlank()) {
                    scope.launch {
                        runCatching { ApiClient.api.updateCollection(collection.id, CollectionUpdateBody(name = name)) }
                        loadAux()
                    }
                }
            },
        )
    }

    collectionMenu?.let { collection ->
        ActionSheet(
            title = collection.name,
            actions = listOf(
                "应用筛选" to { applyCollection(collection) },
                "重命名" to { renameCollection = collection },
                "用当前筛选更新" to {
                    scope.launch {
                        runCatching {
                            ApiClient.api.updateCollection(collection.id, CollectionUpdateBody(filters = currentFiltersJson()))
                        }
                        loadAux()
                    }
                },
                "删除合集" to {
                    scope.launch {
                        runCatching { ApiClient.api.deleteCollection(collection.id) }
                        loadAux()
                    }
                },
            ),
            onDismiss = { collectionMenu = null },
        )
    }

    treeMenuNode?.let { node ->
        ActionSheet(
            title = node.name,
            actions = listOf(
                if (expanded.contains(node.path)) "折叠" to { toggleTreeNode(node) }
                else "展开" to { toggleTreeNode(node) },
                "播放该文件夹" to {
                    scope.launch { playFolder(node) }
                },
                "整个文件夹加入播放列表" to {
                    scope.launch {
                        val files = treeFiles[node.path] ?: runCatching {
                            ApiClient.api.queryPlaylist(buildRequest(folder = node.path, limit = 500))
                        }.getOrNull()?.items.orEmpty()
                        treeFiles[node.path] = files
                        runCatching { repo.addToPlaylist(files.map { it.path }) }
                    }
                },
            ),
            onDismiss = { treeMenuNode = null },
        )
    }

    menuEntry?.let { item ->
        ActionSheet(
            title = item.name,
            actions = listOf(
                "播放" to {
                    val paths = items.map { it.path }
                    val index = paths.indexOf(item.path).coerceAtLeast(0)
                    PlayerLauncher.open(context, paths, index)
                },
                "加入播放列表" to {
                    scope.launch { runCatching { repo.addToPlaylist(listOf(item.path)) } }
                },
                "移除播放列表" to {
                    scope.launch { runCatching { repo.removeFromPlaylist(listOf(item.path)) } }
                },
                "标签 / 详情" to { detailPath = item.path },
            ),
            onDismiss = { menuEntry = null },
        )
    }

    detailPath?.let { path ->
        DetailDialog(
            path = path,
            repo = repo,
            onDismiss = { detailPath = null },
            onPlay = { p -> PlayerLauncher.open(context, listOf(p), 0) },
        )
    }

    if (showTagPicker) {
        TagPickerDialog(
            tags = allTags,
            initialSelected = emptyList(),
            onDismiss = { showTagPicker = false },
            onConfirm = { names ->
                showTagPicker = false
                scope.launch {
                    runCatching { ApiClient.api.batchTags(TagsBatchBody(selected.toList(), names)) }
                    selected.clear()
                    selectionMode = false
                    load()
                }
            },
        )
    }
}

private fun kotlinx.serialization.json.JsonElement.contentOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()

private data class TreeRow(
    val node: com.vidweb.android.data.TreeNode?,
    val file: MediaItem?,
    val parentPath: String,
    val depth: Int,
    val loading: Boolean,
) {
    val key: String
        get() = when {
            node != null -> "n:" + node.path
            file != null -> "f:" + file.path
            else -> "l:" + parentPath
        }
}
