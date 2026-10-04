package com.yudan.toolbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yudan.toolbox.core.*
import com.yudan.toolbox.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var core: ToolboxCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        core = ToolboxCore(this)
        core.refresh()

        setContent {
            var dark by remember { mutableStateOf(Prefs.isDark(this)) }
            YudanTheme(dark = dark) { Nav(core, dark) { dark = it } }
        }
    }

    override fun onResume() {
        super.onResume()
        core.refresh()
    }
}

/** 路由 */
object Route {
    const val HOME = "home"
    const val MODULE = "module/{key}"
    const val FEATURE = "feature/{id}"
    const val FAV = "fav"
    const val HISTORY = "history"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val AUTHOR = "author"
}

@Composable
private fun Nav(core: ToolboxCore, dark: Boolean, onDark: (Boolean) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val nav = rememberNavController()

    var state by remember { mutableStateOf(core.state()) }
    var mode by remember { mutableStateOf(Prefs.getMode(ctx)) }
    val modules = remember { FeatureRepo.modules(ctx) }

    // 收藏 / 历史（SharedPreferences 读取，进入页面时刷新）
    var favs by remember { mutableStateOf(core.favorites()) }
    var hist by remember { mutableStateOf(core.history()) }

    fun refreshAll() {
        core.refresh()
        state = core.state()
        mode = Prefs.getMode(ctx)
        favs = core.favorites()
        hist = core.history()
    }
    LaunchedEffect(Unit) { refreshAll() }

    NavHost(nav, startDestination = Route.HOME) {

        composable(Route.HOME) {
            LaunchedEffect(Unit) { refreshAll() }
            HomeScreen(
                modules = modules,
                state = state,
                mode = mode,
                onModeChange = {
                    Prefs.setMode(ctx, it)
                    if (it == BackendMode.ROOT) core.rootBackend.ensureShell()
                    refreshAll()
                },
                onModuleClick = { nav.navigate("module/${it.key}") },
                onSearchClick = { nav.navigate(Route.SEARCH) },
                onAuthorClick = { nav.navigate(Route.AUTHOR) },
                onSettingsClick = { nav.navigate(Route.SETTINGS) },
                runnableCount = FeatureRepo.load(ctx)
                    .count { it.requiredLevel.value <= state.capable.value }
            )
        }

        composable(Route.MODULE) { back ->
            val key = back.arguments?.getString("key") ?: return@composable
            val m = modules.firstOrNull { it.key == key } ?: return@composable
            ModuleScreen(m, state, { nav.popBackStack() }) { f ->
                nav.navigate("feature/${f.id}")
            }
        }

        composable(Route.FEATURE) { back ->
            val id = back.arguments?.getString("id") ?: return@composable
            val f = FeatureRepo.byId(ctx, id) ?: return@composable
            var results by remember { mutableStateOf(listOf<ExecResult>()) }
            var running by remember { mutableStateOf(false) }
            var fav by remember { mutableStateOf(false) }
            LaunchedEffect(id) { fav = core.isFav(id) }

            FeatureDetailScreen(
                feature = f, state = state, favorite = fav,
                onBack = { nav.popBackStack() },
                onToggleFav = {
                    scope.launch { core.toggleFav(f); fav = core.isFav(f.id) }
                },
                onRun = { args ->
                    scope.launch {
                        running = true; results = emptyList()
                        results = core.run(f, args)
                        running = false
                        hist = core.history()
                        refreshAll()
                    }
                },
                results = results, running = running
            )
        }

        composable(Route.FAV) {
            LaunchedEffect(Unit) { favs = core.favorites() }
            FavoriteScreen(favs, { nav.popBackStack() }) { id -> nav.navigate("feature/$id") }
        }

        composable(Route.HISTORY) {
            LaunchedEffect(Unit) { hist = core.history() }
            HistoryScreen(hist, { nav.popBackStack() },
                onClear = { scope.launch { core.clearHistory(); hist = core.history() } },
                onExport = {
                    scope.launch {
                        val text = core.exportHistory()
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                        }
                        ctx.startActivity(Intent.createChooser(i, "导出操作历史"))
                    }
                })
        }

        composable(Route.SEARCH) {
            var q by remember { mutableStateOf("") }
            val res = remember(q) { FeatureRepo.search(ctx, q) }
            SearchScreen(q, res, state.capable.value, { q = it }, { nav.popBackStack() }) { f ->
                nav.navigate("feature/${f.id}")
            }
        }

        composable(Route.SETTINGS) {
            var d by remember { mutableStateOf(dark) }
            var cf by remember { mutableStateOf(Prefs.confirmRisky(ctx)) }
            SettingsScreen(
                state = state, dark = d, confirmRisky = cf,
                onDarkChange = { d = it; Prefs.setDark(ctx, it); onDark(it) },
                onConfirmChange = { cf = it; Prefs.setConfirmRisky(ctx, it) },
                onAuthorClick = { nav.navigate(Route.AUTHOR) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Route.AUTHOR) {
            AuthorScreen(
                onBack = { nav.popBackStack() },
                onOpen = { url ->
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.onFailure {
                        Toast.makeText(ctx, "打开失败：没装浏览器或酷安", Toast.LENGTH_SHORT).show()
                    }
                },
                onCopy = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("鱼蛋主页", it))
                    Toast.makeText(ctx, "已复制主页链接", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
