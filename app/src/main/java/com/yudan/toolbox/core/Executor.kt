package com.yudan.toolbox.core

/**
 * 统一执行抽象层。
 *
 * 核心：无论底层是 Shizuku(adb) 还是 Root(su)，上层只看到这一个接口。
 * 用户切换「免 Root 模式 / Root 模式」时，只是换实现，功能代码不动。
 */
interface PrivilegedExecutor {

    /** 当前后端状态 */
    val state: BackendState

    /** 刷新状态（Shizuku 是否运行、是否授权、su 是否可用） */
    fun refresh(): BackendState

    /** 执行一条 shell 命令，返回 (exitCode, stdout, stderr) */
    fun exec(cmd: String, timeoutMs: Long = 15_000): Triple<Int, String, String>

    /** 是否有能力执行某等级的操作 */
    fun canRun(level: Level): Boolean = level.value <= state.capable.value
}

/** 命令文本工具：从命令里解析出校验所需的参数 */
object CmdParse {

    /** settings put global xxx yyy → (ns, key, value) */
    fun parseSettingsPut(cmd: String): Triple<String, String, String>? {
        val p = cmd.split(Regex("\\s+"))
        if (p.size < 5 || p[0] != "settings" || p[1] != "put") return null
        return Triple(p[2], p[3], p.drop(4).joinToString(" "))
    }

    /** settings delete global xxx → (ns, key) */
    fun parseSettingsDelete(cmd: String): Pair<String, String>? {
        val p = cmd.split(Regex("\\s+"))
        if (p.size < 4 || p[0] != "settings" || p[1] != "delete") return null
        return p[2] to p[3]
    }

    /** wm size 1080x2400 → (what, value) */
    fun parseWm(cmd: String): Pair<String, String>? {
        val p = cmd.split(Regex("\\s+"))
        if (p.size < 3 || p[0] != "wm") return null
        return p[1] to p[2]
    }

    /** pm disable-user --user 0 pkg → pkg（取最后一个 token） */
    fun parsePkg(cmd: String): String? {
        val p = cmd.split(Regex("\\s+"))
        return p.lastOrNull()?.takeIf { it.contains('.') }
    }

    /** appops set pkg OP mode → (pkg, op, mode) */
    fun parseAppops(cmd: String): Triple<String, String, String>? {
        val p = cmd.split(Regex("\\s+"))
        if (p.size < 5 || p[0] != "appops" || p[1] != "set") return null
        return Triple(p[2], p[3], p[4])
    }
}
