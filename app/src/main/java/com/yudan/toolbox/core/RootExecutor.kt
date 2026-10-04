package com.yudan.toolbox.core

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Root 后端 —— 用 Runtime.exec("su") 自行实现，不依赖 libsu。
 *
 * 为什么不用 libsu：它托管在 jitpack.io，GitHub Actions 上经常超时，
 * 是上次 "1分6秒 All jobs have failed" 的元凶之一。
 * 自实现的代价：没有 shell 会话复用（每条命令新开进程，慢约 100-300ms），
 * 对工具箱这类零散命令场景完全可接受。
 */
class RootExecutor : PrivilegedExecutor {

    companion object { private const val TAG = "RootExecutor" }

    override var state: BackendState = BackendState(mode = BackendMode.ROOT, message = "未初始化")
        private set

    /** 探测 su 是否可用 */
    override fun refresh(): BackendState {
        val (code, out) = execRaw("id -u")
        val isRoot = code == 0 && out.trim() == "0"
        val msg = when {
            isRoot -> "已获取 Root（可运行全部功能，含分区/模块/刷机）"
            out.contains("not found") || code == 127 -> "未 Root：找不到 su 命令"
            else -> "未获取 Root。请先 Root 后使用，或切回「免 Root 模式」"
        }
        state = BackendState(
            mode = BackendMode.ROOT,
            rootAvailable = isRoot,
            rootGranted = isRoot,
            capable = if (isRoot) Level.L2 else Level.L0,
            message = msg
        )
        return state
    }

    /** 首次探测会触发 su 授权弹窗 */
    fun ensureShell() { refresh() }

    override fun exec(cmd: String, timeoutMs: Long): Triple<Int, String, String> {
        if (!state.rootGranted) refresh()
        if (!state.rootGranted) {
            return Triple(-1, "", "未获取 Root：${state.message}")
        }
        return execRaw("su -c '${cmd.replace("'", "'\\''")}'", timeoutMs)
    }

    /** 底层执行：带超时，避免 su 等待授权时永久卡死 */
    private fun execRaw(cmd: String, timeoutMs: Long = 10_000): Triple<Int, String, String> {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = StringBuilder()
            val err = StringBuilder()
            val t1 = Thread {
                try {
                    BufferedReader(InputStreamReader(p.inputStream)).use { r ->
                        var l: String?; while (r.readLine().also { l = it } != null) out.appendLine(l)
                    }
                } catch (_: Throwable) {}
            }
            val t2 = Thread {
                try {
                    BufferedReader(InputStreamReader(p.errorStream)).use { r ->
                        var l: String?; while (r.readLine().also { l = it } != null) err.appendLine(l)
                    }
                } catch (_: Throwable) {}
            }
            t1.start(); t2.start()
            val t0 = System.currentTimeMillis()
            var code = -1
            while (System.currentTimeMillis() - t0 < timeoutMs) {
                try {
                    p.exitValue().let { code = it; break }
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(50)
                }
            }
            if (code == -1) { // 超时
                try { p.destroy() } catch (_: Throwable) {}
                code = -2
            }
            try { t1.join(1000); t2.join(1000) } catch (_: Throwable) {}
            Triple(code, out.toString().trim(), err.toString().trim())
        } catch (t: Throwable) {
            Log.w(TAG, "root exec 失败", t)
            Triple(-1, "", t.message ?: "Root 执行异常")
        }
    }

    /** 判断当前是哪个 Root 方案 */
    fun rootFlavor(): String {
        val (_, out, _) = exec("ls /data/adb 2>/dev/null; command -v magisk ksud apd 2>/dev/null")
        return when {
            out.contains("ksud") || out.contains("ksu") -> "KernelSU"
            out.contains("apd") || out.contains("apatch") -> "APatch"
            out.contains("magisk") -> "Magisk"
            else -> "未知 Root 方案"
        }
    }
}
