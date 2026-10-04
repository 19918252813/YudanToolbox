package com.yudan.toolbox.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 功能执行调度器 —— 五段流水线的中枢：
 *   命令渲染 → 权限裁决 → 执行 → 写后回读 → 审计
 *
 * 关键规则：
 *  - L2（Root）功能在「免 Root 模式」下**绝不静默执行**，直接返回"需要 Root"
 *  - 免 Root 模式下，L1 用 Shizuku；Root 模式下，su 优先、失败自动降级 Shizuku
 */
class ToolboxCore(private val ctx: Context) {

    private val shizuku = ShizukuExecutor()
    private val root = RootExecutor()

    val shizukuBackend get() = shizuku
    val rootBackend get() = root

    /** 当前后端状态（UI 观察它） */
    fun state(): BackendState {
        val mode = Prefs.getMode(ctx)
        return when (mode) {
            BackendMode.READONLY -> BackendState(
                mode = mode, capable = Level.L0, message = "只读模式：仅查看信息，不做任何修改"
            )
            BackendMode.ROOT -> {
                val r = root.refresh()
                if (r.rootGranted) r else {
                    val s = shizuku.refresh()
                    s.copy(message = "Root 不可用，已降级：${s.message}")
                }
            }
            BackendMode.SHIZUKU -> shizuku.refresh()
        }
    }

    fun refresh() {
        when (Prefs.getMode(ctx)) {
            BackendMode.ROOT -> { root.ensureShell(); root.refresh(); shizuku.refresh() }
            BackendMode.SHIZUKU -> shizuku.refresh()
            BackendMode.READONLY -> {}
        }
    }

    private fun pickExecutor(): PrivilegedExecutor = when (Prefs.getMode(ctx)) {
        BackendMode.ROOT -> if (root.refresh().rootGranted) root else shizuku
        BackendMode.SHIZUKU -> shizuku
        BackendMode.READONLY -> shizuku
    }

    /**
     * 执行一个功能项。
     * @param args 用户输入的参数
     */
    suspend fun run(feature: Feature, args: Map<String, String> = emptyMap()): List<ExecResult> =
        withContext(Dispatchers.IO) {
            val mode = Prefs.getMode(ctx)
            val st = state()

            // ---- 权限裁决 ----
            if (mode == BackendMode.READONLY && feature.requiredLevel != Level.L0) {
                return@withContext listOf(
                    ExecResult(feature.id, "", "", "", -1, false,
                        "只读模式下不执行修改类操作，请到设置切换模式", "只读模式",
                        feature.requiredLevel, 0)
                )
            }

            if (feature.requiredLevel == Level.L2 && !st.canRun(Level.L2)) {
                return@withContext listOf(
                    ExecResult(feature.id, "", "", "", -1, false,
                        needRootHint(feature), "需要 Root", Level.L2, 0)
                )
            }

            if (feature.requiredLevel == Level.L1 && !st.canRun(Level.L1)) {
                return@withContext listOf(
                    ExecResult(feature.id, "", "", "", -1, false,
                        "需要 Shizuku 权限：${st.message}", "需要 Shizuku", Level.L1, 0)
                )
            }

            // ---- 执行 + 校验 ----
            val exec = pickExecutor()
            val results = feature.cmds.mapNotNull { cmd ->
                val rendered = render(cmd, args)
                if (rendered.isBlank()) null
                else VerifyEngine.run(feature, rendered, exec, feature.requiredLevel)
            }

            // ---- 审计：写入历史（SharedPreferences）----
            results.forEach { r ->
                Store.addHistory(
                    ctx,
                    History(
                        featureId = feature.id,
                        featureName = feature.name,
                        cmd = r.cmd,
                        effective = r.effective,
                        detail = if (r.effective) r.verifyInfo else r.humanReason(),
                        level = feature.requiredLevel.value
                    )
                )
            }

            if (results.isEmpty()) {
                listOf(ExecResult(feature.id, "", "", "", -1, false,
                    "该项无可执行命令（多为交互式/说明型功能）", "无命令",
                    feature.requiredLevel, 0))
            } else results
        }

    /** 渲染命令：替换占位符 */
    private fun render(cmd: String, args: Map<String, String>): String {
        var out = cmd
        args.forEach { (k, v) -> out = out.replace("\$$k", v).replace("{$k}", v) }
        out = out.replace(Regex("\\$\\{?[a-zA-Z_][a-zA-Z_0-9]*\\}?"), "")
        return out.trim()
    }

    private fun needRootHint(f: Feature): String = buildString {
        append("该功能需要 Root 权限：${f.name}\n")
        append("Shizuku（adb 级）做不到 —— 它不能写块设备、改 /system、装模块或 resetprop。\n")
        append("可选：① 设备 Root 后在设置里切到「Root 模式」；② 用电脑 adb/fastboot 手动执行")
    }

    // ---------- 收藏 ----------
    fun favorites() = Store.favorites(ctx)
    suspend fun isFav(id: String) = withContext(Dispatchers.IO) { Store.isFav(ctx, id) }
    suspend fun toggleFav(f: Feature) = withContext(Dispatchers.IO) { Store.toggleFav(ctx, f) }

    // ---------- 历史 ----------
    fun history() = Store.history(ctx)
    suspend fun clearHistory() = withContext(Dispatchers.IO) { Store.clearHistory(ctx) }

    /** 导出历史为文本 */
    suspend fun exportHistory(): String = withContext(Dispatchers.IO) {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
        val fmt2 = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
        val list = Store.history(ctx)
        val sb = StringBuilder("鱼蛋工具箱 · 操作历史\n导出时间: ${fmt.format(java.util.Date())}\n共 ${list.size} 条\n\n")
        list.forEach { h ->
            sb.appendLine("[${if (h.effective) "✓" else "✗"}] ${h.featureName}  (${h.featureId})")
            sb.appendLine("    命令: ${h.cmd}")
            sb.appendLine("    结果: ${h.detail}")
            sb.appendLine("    时间: ${fmt2.format(java.util.Date(h.at))}")
            sb.appendLine()
        }
        sb.toString()
    }
}
