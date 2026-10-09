package com.vidweb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vidweb.android.data.ApiClient
import com.vidweb.android.data.Prefs
import com.vidweb.android.data.Repo
import com.vidweb.android.ui.common.LoadingBox
import com.vidweb.android.ui.lib.LibScreen
import com.vidweb.android.ui.path.PathScreen
import com.vidweb.android.ui.search.SearchScreen
import com.vidweb.android.ui.settings.SettingsScreen
import com.vidweb.android.ui.tags.TagFilesScreen
import com.vidweb.android.ui.tags.TagsScreen
import com.vidweb.android.ui.tasks.TasksScreen
import com.vidweb.android.ui.theme.Accent
import com.vidweb.android.ui.theme.TextDim
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.vidweb.android.ui.theme.VidWebTheme {
                AppRoot((application as VidWebApp).repo)
            }
        }
    }
}

private enum class ServerStatus { Checking, Online, Offline }

@Composable
private fun AppRoot(repo: Repo) {
    var status by remember { mutableStateOf(ServerStatus.Checking) }
    var savedUrl by remember { mutableStateOf(Prefs.DEFAULT_SERVER) }

    suspend fun connect(url: String): Boolean {
        ApiClient.setBaseUrl(url)
        val ok = runCatching { ApiClient.api.config() }.isSuccess
        if (ok) repo.loadState()
        return ok
    }

    LaunchedEffect(Unit) {
        val url = runCatching { repo.prefs.serverUrl.first() }.getOrDefault(Prefs.DEFAULT_SERVER)
        savedUrl = url
        status = if (connect(url)) ServerStatus.Online else ServerStatus.Offline
    }

    when (status) {
        ServerStatus.Checking -> Box(Modifier.fillMaxSize()) { LoadingBox() }
        ServerStatus.Offline -> SetupScreen(
            initialUrl = savedUrl,
            onSave = { url ->
                repo.prefs.setServerUrl(url)
                savedUrl = url
            },
            onConnect = { url ->
                val ok = connect(url)
                if (ok) status = ServerStatus.Online
                ok
            },
        )
        ServerStatus.Online -> MainShell(
            repo = repo,
            onDisconnect = { status = ServerStatus.Offline },
        )
    }
}

@Composable
private fun SetupScreen(
    initialUrl: String,
    onSave: suspend (String) -> Unit,
    onConnect: suspend (String) -> Boolean,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = Accent)
        Spacer(Modifier.height(12.dp))
        Text("连接 VidWeb 服务器", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "输入运行 Media Web 后端服务的地址，例如 http://192.168.1.10:8765",
            color = TextDim,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("服务器地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val ok = onConnect(url)
                        message = if (ok) "连接成功" else "无法连接，请检查地址与服务状态"
                        if (ok) onSave(url)
                        busy = false
                    }
                },
            ) { Text(if (busy) "连接中…" else "连接") }
        }
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message.startsWith("连接成功")) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Accent)
                    Spacer(Modifier.width(6.dp))
                }
                Text(message, color = if (message.startsWith("连接成功")) Accent else MaterialTheme.colorScheme.error)
            }
        }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("path", "路径库", Icons.Filled.Folder),
    NavItem("lib", "视频库", Icons.Filled.Home),
    NavItem("search", "搜索", Icons.Filled.Search),
    NavItem("tags", "标签", Icons.Filled.ListAlt),
    NavItem("tasks", "任务", Icons.Filled.CheckCircle),
    NavItem("settings", "设置", Icons.Filled.Settings),
)

@Composable
private fun MainShell(repo: Repo, onDisconnect: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    val onSelect: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                for (item in NAV_ITEMS) {
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = { onSelect(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            AppNavHost(
                navController = navController,
                repo = repo,
                onDisconnect = onDisconnect,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    for (item in NAV_ITEMS) {
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onSelect(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            },
        ) { padding ->
            AppNavHost(
                navController = navController,
                repo = repo,
                onDisconnect = onDisconnect,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    repo: Repo,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "path",
        modifier = modifier,
    ) {
        composable("path") { PathScreen(repo) }
        composable("lib") { LibScreen(repo) }
        composable("search") { SearchScreen(repo) }
        composable("tags") { TagsScreen(repo, onOpenTag = { id, name ->
            navController.navigate("tagfiles/$id?name=${android.net.Uri.encode(name)}")
        }) }
        composable("tagfiles/{id}?name={name}") { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val name = entry.arguments?.getString("name") ?: "标签"
            TagFilesScreen(repo, id, name, onBack = { navController.popBackStack() })
        }
        composable("tasks") { TasksScreen(repo) }
        composable("settings") { SettingsScreen(repo, onDisconnect = onDisconnect) }
    }
}
