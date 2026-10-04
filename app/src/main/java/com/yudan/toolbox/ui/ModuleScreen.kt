package com.yudan.toolbox.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yudan.toolbox.core.BackendState
import com.yudan.toolbox.core.Feature
import com.yudan.toolbox.core.Module

/**
 * 模块详情页：列出该模块全部功能项。
 * 关键交互：不可用项不是"灰掉让你猜"，而是点开明确告诉你为什么、怎么解决。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(
    module: Module,
    state: BackendState,
    onBack: () -> Unit,
    onFeatureClick: (Feature) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${module.icon} ${module.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val avail = module.items.count { it.requiredLevel.value <= state.capable.value }
                Text(
                    "共 ${module.items.size} 项 · 当前权限可用 $avail 项",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(module.items, key = { it.id }) { f ->
                FeatureRow(f, state.capable.value) { onFeatureClick(f) }
            }
        }
    }
}

@Composable
private fun FeatureRow(f: Feature, capable: Int, onClick: () -> Unit) {
    val usable = f.requiredLevel.value <= capable
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (usable) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                "${f.index}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(26.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    f.name,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (usable) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    // 等级标签
                    LevelChip(f.requiredLevel.value)
                    if (f.risky) {
                        Chip("危险", MaterialTheme.colorScheme.error)
                    }
                    if (f.cmds.isEmpty()) {
                        Chip("说明型", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
            Icon(
                if (usable) Icons.Default.PlayArrow else Icons.Default.Lock,
                null,
                tint = if (usable) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LevelChip(level: Int) {
    Chip(levelLabel(level), levelColor(level))
}

@Composable
fun Chip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
