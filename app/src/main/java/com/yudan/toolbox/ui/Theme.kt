package com.yudan.toolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC8),
    secondary = Color(0xFF2E7D5B),
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    error = Color(0xFFD32F2F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A5C),
    onPrimary = Color(0xFF2B1200),
    primaryContainer = Color(0xFF5A2A10),
    secondary = Color(0xFF6FD6A6),
    background = Color(0xFF121316),
    surface = Color(0xFF1C1E22),
    error = Color(0xFFFF6B6B)
)

@Composable
fun YudanTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

/** 分隔线：自己实现，避免 Material3 各版本 Divider 差异 */
@Composable
fun Line() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

/** 等级色：L0 灰 / L1 蓝 / L2 橙红 */
fun levelColor(level: Int): Color = when (level) {
    0 -> Color(0xFF8A8F98)
    1 -> Color(0xFF3B82F6)
    else -> Color(0xFFEF6C33)
}

fun levelLabel(level: Int): String = when (level) {
    0 -> "免Root"
    1 -> "Shizuku"
    else -> "需Root"
}
