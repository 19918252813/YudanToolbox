package com.yudan.toolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yudan.toolbox.core.BackendMode
import com.yudan.toolbox.core.BackendState
import com.yudan.toolbox.core.Level
import com.yudan.toolbox.core.Module

/**
 * 首页：分类卡片宫格（4A 方案）。
 * 顶部是后端状态条（一眼看到当前是免 Root 还是 Root、能否用），下面是 28 个模块宫格。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modules: List<Module>,
    state: BackendState,
    mode: BackendMode,
    onModeChange: (BackendMode) -> Unit,
    onModuleClick: (Module) -> Unit,
    onSearchClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    runnableCount: Int
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("鱼蛋工具箱", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "by 鱼蛋 · ${modules.sumOf { it.items.size }} 项功能",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, "搜索功能")
                    }
                    IconButton(onClick = onAuthorClick) {
                        Icon(Icons.Default.Person, "作者主页")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // ── 后端状态条 ──
            BackendBar(state, mode, onModeChange, runnableCount)

            // ── 模块宫格 ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(modules, key = { it.key }) { m ->
                    ModuleCard(m, state) { onModuleClick(m) }
                }
            }
        }
    }
}

@Composable
private fun BackendBar(
    state: BackendState, mode: BackendMode, onModeChange: (BackendMode) -> Unit, runnable: Int
) {
    val ok = state.capable != Level.L0 || mode == BackendMode.READONLY
    val bg = if (ok) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer

    Card(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    state.message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "当前可用 $runnable 项 · 等级 ${state.capable.label}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(8.dp))

            // 模式切换：免 Root / Root / 只读
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip("免 Root 模式", mode == BackendMode.SHIZUKU, Modifier.weight(1f)) {
                    onModeChange(BackendMode.SHIZUKU)
                }
                ModeChip("Root 模式", mode == BackendMode.ROOT, Modifier.weight(1f)) {
                    onModeChange(BackendMode.ROOT)
                }
                ModeChip("只读", mode == BackendMode.READONLY, Modifier.weight(1f)) {
                    onModeChange(BackendMode.READONLY)
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, fontSize = 11.sp, maxLines = 1, modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        },
        modifier = modifier.height(32.dp)
    )
}

@Composable
private fun ModuleCard(m: Module, state: BackendState, onClick: () -> Unit) {
    // 统计该模块里当前权限下能跑多少
    val runnable = m.items.count { it.requiredLevel.value <= state.capable.value }
    val total = m.items.size
    val ratio = if (total == 0) 0f else runnable.toFloat() / total

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(m.icon, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                m.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            // 可用比例条
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .background(
                            if (ratio > 0.6f) MaterialTheme.colorScheme.primary
                            else if (ratio > 0.2f) Color(0xFFF0A500)
                            else MaterialTheme.colorScheme.error,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            Text(
                "$runnable/$total",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
