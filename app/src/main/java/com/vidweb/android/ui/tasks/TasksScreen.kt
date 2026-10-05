package com.vidweb.android.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.Repo
import com.vidweb.android.data.TaskItem
import com.vidweb.android.ui.common.ActionSheet
import com.vidweb.android.ui.common.EmptyHint
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.Danger
import com.vidweb.android.ui.theme.TextDim
import com.vidweb.android.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun stateLabel(state: String): String = when (state) {
    "running" -> "进行中"
    "queued" -> "排队中"
    "done" -> "已完成"
    "cancelled" -> "已取消"
    "error" -> "失败"
    else -> state
}

@Composable
fun TasksScreen(repo: Repo) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var menuTask by remember { mutableStateOf<TaskItem?>(null) }

    suspend fun load() {
        runCatching { ApiClient.api.tasks().items }.onSuccess { tasks = it }
    }

    LaunchedEffect(Unit) {
        while (true) {
            load()
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { scope.launch { load() } }) { Text("刷新") }
            Spacer(Modifier.weight(1f))
            Text("共 ${tasks.size} 条", color = TextDim, style = MaterialTheme.typography.labelSmall)
        }

        if (tasks.isEmpty()) {
            EmptyHint("暂无任务记录")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(tasks, key = { it.id }) { task ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                task.name.ifBlank { task.type },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                stateLabel(task.state),
                                color = if (task.state == "error") Danger else if (task.state == "running") Accent else TextDim,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append(Format.dateTime(task.createdAt))
                                if (task.total > 0) append(" · ${task.processed}/${task.total}")
                                if (task.added > 0) append(" · 新增 ${task.added}")
                            },
                            color = TextDim,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (!task.error.isNullOrBlank()) {
                            Text(task.error, color = Danger, style = MaterialTheme.typography.labelSmall)
                        }
                        if (task.state == "running" && task.total > 0) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (task.processed.toFloat() / task.total.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = Accent,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { menuTask = task }) { Text("操作") }
                        }
                    }
                }
            }
        }
    }

    menuTask?.let { task ->
        ActionSheet(
            title = task.name.ifBlank { task.type },
            actions = listOf(
                "删除记录" to {
                    scope.launch {
                        runCatching { ApiClient.api.deleteTask(task.id) }
                        load()
                    }
                },
            ),
            onDismiss = { menuTask = null },
        )
    }
}
