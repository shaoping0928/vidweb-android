package com.vidweb.android.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.RecentItem
import com.vidweb.android.data.Repo
import com.vidweb.android.player.PlayerLauncher
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.DetailDialog
import com.vidweb.android.ui.common.EmptyHint
import com.vidweb.android.ui.common.LoadingBox
import com.vidweb.android.ui.common.MediaRow
import com.vidweb.android.ui.common.toEntry
import com.vidweb.android.ui.theme.TextDim
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(repo: Repo) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RecentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var menuEntry by remember { mutableStateOf<RecentItem?>(null) }
    var detailPath by remember { mutableStateOf<String?>(null) }

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        scope.launch {
            loading = true
            searched = true
            runCatching { ApiClient.api.search(q).items }.onSuccess { results = it }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("搜索文件名、标签和评论") },
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { doSearch() }) { Text("搜索") }
        }

        if (searched) {
            Text(
                "共 ${results.size} 个结果",
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }

        when {
            loading -> LoadingBox()
            results.isEmpty() && searched -> EmptyHint("没有匹配的结果")
            results.isEmpty() -> EmptyHint("输入关键字搜索媒体库")
            else -> LazyColumn(Modifier.weight(1f)) {
                items(results, key = { it.path }) { item ->
                    MediaRow(
                        entry = item.toEntry(),
                        onClick = {
                            val paths = results.map { it.path }
                            PlayerLauncher.open(context, paths, paths.indexOf(item.path).coerceAtLeast(0))
                        },
                        onLongClick = { menuEntry = item },
                    )
                }
            }
        }
    }

    menuEntry?.let { item ->
        ActionSheet(
            title = item.name,
            actions = listOf(
                "播放" to {
                    val paths = results.map { it.path }
                    PlayerLauncher.open(context, paths, paths.indexOf(item.path).coerceAtLeast(0))
                },
                "加入播放列表" to { scope.launch { runCatching { repo.addToPlaylist(listOf(item.path)) } } },
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
}
