package com.yudan.toolbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yudan.toolbox.core.BackendState
import com.yudan.toolbox.core.ExecResult
import com.yudan.toolbox.core.Feature

/**
 * 功能执行页 —— 执行 + 展示「是否真的生效」。
 *
 * 这是 App 区别于 SH 脚本的核心：每条命令执行完立刻给出 ✓/✗，
 * ✗ 时明确说原因和怎么办，绝不"没报错就算成功"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureDetailScreen(
    feature: Feature,
    state: BackendState,
    favorite: Boolean,
    onBack: () -> Unit,
    onToggleFav: () -> Unit,
    onRun: (Map<String, String>) -> Unit,
    results: List<ExecResult>,
    running: Boolean
) {
    val clipboard = LocalClipboardManager.current
    var showConfirm by remember { mutableStateOf(feature.risky) }
    var args by remember { mutableStateOf(mapOf<String, String>()) }

    // 收集命令里的占位符作为输入项
    val placeholders = remember(feature) {
        feature.cmds.flatMap { cmd ->
            Regex("\\$\\{?([a-zA-Z_][a-zA-Z_0-9]*)\\}?").findAll(cmd).map { it.groupValues[1] }
        }.distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(feature.name, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onToggleFav) {
                        Icon(
                            if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                            "收藏",
                            tint = if (favorite) Color(0xFFFFB300) else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (feature.cmds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showConfirm = true },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text(if (running) "执行中…" else "执行") }
                )
            }
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 元信息 ──
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LevelChip(feature.requiredLevel.value)
                if (feature.risky) Chip("危险操作", MaterialTheme.colorScheme.error)
                Chip(feature.moduleName, MaterialTheme.colorScheme.primary)
            }

            // ── 权限不足提示（明确，不灰显了事）──
            if (feature.requiredLevel.value > state.capable.value) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("当前权限不足", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (feature.requiredLevel.value == 2)
                                "这是 Root 级功能。Shizuku（adb 级）做不到 —— 它不能写块设备、改 /system、装 Magisk 模块或 resetprop。\n\n解决办法：① 设备 Root 后到设置切「Root 模式」；② 用电脑 adb/fastboot 手动执行下面的命令"
                            else
                                "需要 Shizuku 权限。请安装 Shizuku 并启动服务，然后在 Shizuku 里授权本应用。",
                            fontSize = 12.sp, lineHeight = 17.sp
                        )
                    }
                }
            }

            // ── 参数输入 ──
            if (placeholders.isNotEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("需要参数", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        placeholders.forEach { p ->
                            var v by remember(p) { mutableStateOf("") }
                            OutlinedTextField(
                                value = v,
                                onValueChange = { v = it; args = args + (p to it) },
                                label = { Text(p, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            }

            // ── 命令预览 ──
            if (feature.cmds.isNotEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("将执行的命令", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(feature.cmds.joinToString("\n")))
                            }) { Text("复制", fontSize = 12.sp) }
                        }
                        feature.cmds.forEach { c ->
                            Text(
                                c, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (feature.cmds.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("说明型功能", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("${feature.name} 在原脚本里是交互式/说明性功能，没有单一可执行命令。"
                            + "App 里会逐步把它改造成可一键操作的形态。", fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }

            // ── 结果 ──
            if (results.isNotEmpty()) {
                Text("执行结果", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                results.forEach { r -> ResultCard(r, onCopy = { clipboard.setText(AnnotatedString(r.cmd)) }) }
            }

            if (running) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(70.dp))
        }
    }

    // ── 危险操作二次确认 ──
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (feature.risky) "⚠ 危险操作确认" else "确认执行") },
            text = {
                Column {
                    Text(feature.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    if (feature.risky) {
                        Text("该操作可能影响系统稳定性、造成数据丢失甚至无法开机。",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text("建议先备份重要数据。确定继续吗？", fontSize = 12.sp)
                    } else {
                        Text("即将执行 ${feature.cmds.size} 条命令，执行后会立即回读校验是否真的生效。",
                            fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onRun(args) }) {
                    Text("执行", color = if (feature.risky) MaterialTheme.colorScheme.error else Color.Unspecified)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ResultCard(r: ExecResult, onCopy: () -> Unit) {
    val ok = r.effective
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) Color(0xFF2E7D5B).copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    null,
                    tint = if (ok) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (ok) "✓ 已生效" else "✗ 未生效",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = if (ok) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCopy, contentPadding = PaddingValues(0.dp)) {
                    Text("复制命令", fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (r.cmd.isNotBlank()) {
                Text("命令:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(r.cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                if (ok) r.verifyInfo else r.humanReason(),
                fontSize = 12.sp, lineHeight = 16.sp,
                color = if (ok) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error
            )
            if (!ok && r.stdout.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("系统返回:", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(r.stdout.take(300), fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                    lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(2.dp))
            Text("耗时 ${r.durationMs}ms", fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
