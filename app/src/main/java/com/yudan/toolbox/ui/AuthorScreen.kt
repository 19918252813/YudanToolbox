package com.yudan.toolbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val HOME_URL = "https://www.coolapk.com/u/42391964"

/**
 * 作者主页页。
 *
 * 与 SH 脚本不同：App 是前台进程，不受 Android 10+「后台启动 Activity」限制，
 * 所以 am/Intent 唤起是真能生效的 —— 这里是唯一不需要兜底方案的地方。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val ctx = LocalContext.current
    var coolapkInstalled by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        coolapkInstalled = runCatching {
            ctx.packageManager.getPackageInfo("com.coolapk.market", 0); true
        }.getOrDefault(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("作者主页", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // 头像占位
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🐟", fontSize = 40.sp)
                }
            }

            Text("鱼蛋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("酷安 @鱼蛋", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(Modifier.height(6.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "这个工具箱从 5300 行的 SH 脚本一路改到 App。"
                        + "每一条功能都保留了「写后回读校验」—— 没真生效就明说，绝不假报成功。"
                        + "\n\n有问题、想要新功能、或者发现哪条命令没生效，欢迎来酷安找我。",
                    fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // 打开主页
            Button(
                onClick = { onOpen(HOME_URL) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("打开作者主页", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            // 酷安 App 内打开
            if (coolapkInstalled == true) {
                OutlinedButton(
                    onClick = { onOpen("coolapk://u/42391964") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("在酷安 App 内打开", fontSize = 14.sp)
                }
            } else if (coolapkInstalled == false) {
                Text("未检测到酷安 App，将用浏览器打开", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            OutlinedButton(
                onClick = { onCopy(HOME_URL) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("复制主页链接", fontSize = 14.sp)
            }

            Spacer(Modifier.weight(1f))
            Text("v1.0.0 · 鱼蛋工具箱", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}
