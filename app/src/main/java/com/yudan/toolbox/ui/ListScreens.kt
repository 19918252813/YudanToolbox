package com.yudan.toolbox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yudan.toolbox.core.BackendMode
import com.yudan.toolbox.core.BackendState
import com.yudan.toolbox.core.Feature
import com.yudan.toolbox.core.Level
import com.yudan.toolbox.core.Favorite
import com.yudan.toolbox.core.History

/** 设置页：模式切换、危险确认开关、深色、关于、作者主页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BackendState,
    dark: Boolean,
    confirmRisky: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onConfirmChange: (Boolean) -> Unit,
    onAuthorClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp)) {

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("当前状态", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(state.message, fontSize = 12.sp, lineHeight = 17.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("能力等级：${state.capable.label}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Text("运行模式", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(BackendMode.entries.toList()) { m ->
                Row(
                    Modifier.fillMaxWidth().clickable { }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(m.desc, fontSize = 11.sp, lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Line()
            }

            item { Spacer(Modifier.height(12.dp)) }
            item {
                Text("通用", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            }
            item {
                SwitchRow("深色模式", "跟随系统或手动切换", dark, onDarkChange)
                Line()
                SwitchRow("危险操作二次确认", "执行危险功能前弹窗确认", confirmRisky, onConfirmChange)
                Line()
            }

            item { Spacer(Modifier.height(12.dp)) }
            item {
                Text("关于", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
            }
            item {
                Row(Modifier.fillMaxWidth().clickable(onClick = onAuthorClick).padding(vertical = 14.dp)) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("作者主页 · 鱼蛋", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("酷安 @鱼蛋 · 点一下跳转", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Line()
            }

            item {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠ 免责声明", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "本工具涉及系统修改、Root 与刷机操作，可能导致数据丢失、设备无法开机或失去保修。"
                                + "所有后果由操作者自行承担。\n\n"
                                + "本应用不提供、也不会实现任何修改 IMEI / 设备串号的功能 —— "
                                + "修改 IMEI 在多数国家和地区属于违法行为。\n\n"
                                + "请勿对他人设备执行本工具的任何修改类操作。",
                            fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, on: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = on)
    }
}

/** 收藏页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(favs: List<Favorite>, onBack: () -> Unit, onClick: (String) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("我的收藏", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } })
    }) { pad ->
        if (favs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.StarBorder, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("还没有收藏", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("在功能详情页点右上角星标添加", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favs, key = { it.featureId }) { f ->
                    Card(Modifier.fillMaxWidth().clickable { onClick(f.featureId) },
                        shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(f.moduleName, fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

/** 历史页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    list: List<History>, onBack: () -> Unit, onClear: () -> Unit, onExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("操作历史", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onExport) { Icon(Icons.Default.Share, "导出") }
                    IconButton(onClear) { Icon(Icons.Default.DeleteSweep, "清空") }
                })
        }
    ) { pad ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("暂无操作记录", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.id }) { h ->
                    val ok = h.effective
                    Card(shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ok) Color(0xFF2E7D5B).copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Text(if (ok) "✓" else "✗", fontSize = 14.sp,
                                color = if (ok) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(h.featureName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (h.cmd.isNotBlank())
                                    Text(h.cmd, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp, maxLines = 2, lineHeight = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                Text(h.detail, fontSize = 11.sp, lineHeight = 15.sp,
                                    color = if (ok) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.error)
                                Text("${levelLabel(h.level)} · " +
                                    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                                        .format(java.util.Date(h.at)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 搜索页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String, results: List<Feature>, capable: Int,
    onQuery: (String) -> Unit, onBack: () -> Unit, onClick: (Feature) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = {
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                placeholder = { Text("搜功能名，如「动画」「DPI」「禁用」", fontSize = 12.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                )
            )
        }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (query.isNotBlank()) {
                item {
                    Text("找到 ${results.size} 项", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                items(results, key = { it.id }) { f ->
                    Card(Modifier.fillMaxWidth().clickable { onClick(f) }, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${f.moduleName} · ${f.id}", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                            }
                            LevelChip(f.requiredLevel.value)
                        }
                    }
                }
            }
        }
    }
}
