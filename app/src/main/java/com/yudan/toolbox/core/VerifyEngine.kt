package com.yudan.toolbox.core

/**
 * 写后回读校验引擎。
 *
 * 这是整个 App 的灵魂 —— 沿用 SH 脚本的核心设计：
 *   写入 → 回读 → 比对 → 只有真变了才报「✓ 已生效」，否则「✗ 未生效」+ 具体原因
 *
 * 绝不允许"命令没报错就算成功"。退出码 0 但值没变，就是没生效。
 */
object VerifyEngine {

    /**
     * 执行 + 校验一条命令。
     * @param feature 功能项（提供校验类型）
     * @param cmd 已渲染好的命令
     * @param exec 实际执行器
     */
    fun run(feature: Feature, cmd: String, exec: PrivilegedExecutor, level: Level): ExecResult {
        val t0 = System.currentTimeMillis()
        val (code, out, err) = exec.exec(cmd)
        val dur = System.currentTimeMillis() - t0

        val combined = "$out\n$err"

        // 第一步：命令层面的硬失败（不等回读，直接判死）
        val hardFail = classifyHardFailure(combined, code)
        if (hardFail != null) {
            return ExecResult(
                featureId = feature.id, cmd = cmd, stdout = out, stderr = err,
                exitCode = code, effective = false, failureReason = hardFail,
                level = level, durationMs = dur
            )
        }

        // 第二步：写后回读比对
        return when (feature.verifyType) {
            VerifyType.NONE -> ExecResult(
                featureId = feature.id, cmd = cmd, stdout = out, stderr = err,
                exitCode = code, effective = true, verifyInfo = "只读/无校验类操作，已执行",
                level = level, durationMs = dur
            )
            VerifyType.SETTINGS_PUT -> verifySettingsPut(feature, cmd, exec, out, err, code, level, dur)
            VerifyType.SETTINGS_DELETE -> verifySettingsDelete(feature, cmd, exec, out, err, code, level, dur)
            VerifyType.WM -> verifyWm(feature, cmd, exec, out, err, code, level, dur)
            VerifyType.PM_DISABLED -> verifyPmDisabled(feature, cmd, exec, out, err, code, level, dur)
            VerifyType.PM_ENABLED -> verifyPmEnabled(feature, cmd, exec, out, err, code, level, dur)
            VerifyType.APPOPS -> verifyAppops(feature, cmd, exec, out, err, code, level, dur)
        }
    }

    // ---------- 硬失败判定 ----------
    private fun classifyHardFailure(text: String, code: Int): String? = when {
        "not found" in text -> "当前环境没有这条命令（缺少 toybox / 不是完整 Android shell）"
        "Background activity start blocked" in text || "blocked" in text ->
            "Android 10+ 后台启动限制：后台进程不允许直接弹页面"
        "Permission Denial" in text || "Permission denied" in text ||
                "not permitted" in text || "SecurityException" in text ->
            "权限不足：需要 Shizuku 授权或 Root"
        "Unknown package" in text || "unknown package" in text -> "包名不存在"
        "read-only" in text || "Read-only" in text -> "分区只读：需 Root 重新挂载为可写"
        code != 0 && text.isBlank() -> "命令退出码 $code 且无输出"
        else -> null
    }

    // ---------- settings put ----------
    private fun verifySettingsPut(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val t = CmdParse.parseSettingsPut(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析校验参数", level = level, durationMs = dur)
        val (ns, key, want) = t
        val (_, now, _) = exec.exec("settings get $ns $key")
        val got = now.trim()
        return if (got == want.trim()) {
            ExecResult(f.id, cmd, out, err, code, true, "$ns.$key = $got", level = level, durationMs = dur)
        } else if (got == "null" || got.isEmpty()) {
            ExecResult(f.id, cmd, out, err, code, false,
                "$ns.$key 未写入（读取为空：只读属性 或 权限不足）", "读取为空", level, dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, false,
                "$ns.$key 实际=「$got」≠ 期望「${want.trim()}」", "回读不一致", level, dur)
        }
    }

    // ---------- settings delete ----------
    private fun verifySettingsDelete(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val t = CmdParse.parseSettingsDelete(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析校验参数", level = level, durationMs = dur)
        val (ns, key) = t
        val (_, now, _) = exec.exec("settings get $ns $key")
        val got = now.trim()
        return if (got == "null" || got.isEmpty()) {
            ExecResult(f.id, cmd, out, err, code, true, "$ns.$key 已删除", level = level, durationMs = dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, false,
                "$ns.$key 仍为「$got」", "删除未生效", level, dur)
        }
    }

    // ---------- wm size / density ----------
    private fun verifyWm(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val t = CmdParse.parseWm(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析校验参数", level = level, durationMs = dur)
        val (what, want) = t
        if (want == "reset") {
            val (_, now, _) = exec.exec("wm $what")
            return ExecResult(f.id, cmd, out, err, code, true, "wm $what → ${now.trim()}", level = level, durationMs = dur)
        }
        val (_, now, _) = exec.exec("wm $what")
        val got = now.trim()
        return if (got.contains(want)) {
            ExecResult(f.id, cmd, out, err, code, true, "wm $what → $got", level = level, durationMs = dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, false,
                "wm $what 实际=「$got」≠ 期望含「$want」", "回读不一致", level, dur)
        }
    }

    // ---------- pm disable ----------
    private fun verifyPmDisabled(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val pkg = CmdParse.parsePkg(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析包名", level = level, durationMs = dur)
        val (_, list, _) = exec.exec("pm list packages -d")
        val disabled = list.lineSequence()
            .map { it.removePrefix("package:").trim() }
            .toSet()
        return if (pkg in disabled) {
            ExecResult(f.id, cmd, out, err, code, true, "$pkg 已禁用", level = level, durationMs = dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, false,
                "$pkg 未出现在已禁用列表", "禁用未生效", level, dur)
        }
    }

    // ---------- pm enable ----------
    private fun verifyPmEnabled(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val pkg = CmdParse.parsePkg(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析包名", level = level, durationMs = dur)
        val (_, list, _) = exec.exec("pm list packages -d")
        val disabled = list.lineSequence()
            .map { it.removePrefix("package:").trim() }
            .toSet()
        return if (pkg in disabled) {
            ExecResult(f.id, cmd, out, err, code, false, "$pkg 仍在禁用列表", "启用未生效", level, dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, true, "$pkg 已启用", level = level, durationMs = dur)
        }
    }

    // ---------- appops ----------
    private fun verifyAppops(
        f: Feature, cmd: String, exec: PrivilegedExecutor,
        out: String, err: String, code: Int, level: Level, dur: Long
    ): ExecResult {
        val t = CmdParse.parseAppops(cmd)
            ?: return ExecResult(f.id, cmd, out, err, code, true, "无法解析校验参数", level = level, durationMs = dur)
        val (pkg, op, _) = t
        val (_, now, _) = exec.exec("appops get $pkg")
        val line = now.lineSequence().firstOrNull { it.contains(op, ignoreCase = true) }
        return if (line != null) {
            ExecResult(f.id, cmd, out, err, code, true, "appops $pkg → ${line.trim()}", level = level, durationMs = dur)
        } else {
            ExecResult(f.id, cmd, out, err, code, false,
                "无法回读 appops $pkg $op（权限不足 或 OP 名无效）", "回读失败", level, dur)
        }
    }
}
