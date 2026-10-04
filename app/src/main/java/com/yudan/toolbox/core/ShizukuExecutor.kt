package com.yudan.toolbox.core

import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku 后端 —— 免 Root 模式的主力。
 *
 * 重要认知（来自技术方案调研）：Shizuku 是 ADB 桥，**不是"无 Root 的 Root"**。
 * ADB 模式下以 shell uid 2000 运行，做不到：dd 写块设备、改 /system、装 Magisk 模块、
 * resetprop、reboot 到 recovery（PowerManager.reboot 要 REBOOT 权限，第三方 App 拿不到）。
 * 这些必须标为 L2，交给 RootExecutor。
 */
class ShizukuExecutor : PrivilegedExecutor {

    companion object { private const val TAG = "ShizukuExecutor" }

    private val listeners = mutableListOf<() -> Unit>()
    private var binderReceived = false

    override var state: BackendState = BackendState(message = "未初始化")
        private set

    init { bindListeners() }

    private fun bindListeners() {
        try {
            Shizuku.addBinderReceivedListenerSticky {
                binderReceived = true; refresh()
            }
            Shizuku.addBinderDeadListener { binderReceived = false; refresh() }
            Shizuku.addRequestPermissionResultListener { _, grantResult ->
                refresh()
                Log.i(TAG, "permission result=$grantResult")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Shizuku listener 注册失败（可能未安装 Shizuku）", t)
        }
    }

    fun onChanged(cb: () -> Unit) { listeners += cb }

    /** 请求 Shizuku 授权；已授权返回 true */
    fun requestPermission(): Boolean {
        return try {
            if (Shizuku.checkSelfPermission() == 0) true
            else { Shizuku.requestPermission(1001); false }
        } catch (t: Throwable) { Log.w(TAG, "requestPermission", t); false }
    }

    override fun refresh(): BackendState {
        val installed = try {
            android.content.pm.PackageManager.PERMISSION_GRANTED == run {
                val ctx = com.yudan.toolbox.App.instance
                ctx.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
                android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) { false }

        val running = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        val granted = try { Shizuku.checkSelfPermission() == 0 } catch (_: Throwable) { false }
        val uid = try { Shizuku.getUid() } catch (_: Throwable) { -1 }

        val level = when {
            running && granted && uid == 0 -> Level.L2   // Shizuku 以 Root 启动
            running && granted -> Level.L1               // Shizuku 以 adb 启动
            else -> Level.L0
        }

        val msg = when {
            !installed -> "未安装 Shizuku（装了才能免 Root 用 adb 级权限）"
            !running -> "Shizuku 已安装但未运行，请打开 Shizuku 启动服务"
            !granted -> "Shizuku 未授权本应用，请在 Shizuku 里授权"
            uid == 0 -> "Shizuku 以 Root 方式运行（可用全部功能）"
            else -> "Shizuku 以 ADB 方式运行（可用 adb 级功能）"
        }

        state = BackendState(
            mode = BackendMode.SHIZUKU,
            shizukuInstalled = installed,
            shizukuRunning = running,
            shizukuPermission = granted,
            shizukuUid = uid,
            capable = level,
            message = msg
        )
        listeners.forEach { runCatching { it() } }
        return state
    }

    override fun exec(cmd: String, timeoutMs: Long): Triple<Int, String, String> {
        if (!(state.shizukuRunning && state.shizukuPermission)) {
            return Triple(-1, "", "Shizuku 未就绪：${state.message}")
        }
        return runCatching {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            // 读取输出 + 错误
            val out = StringBuilder(); val err = StringBuilder()
            val t1 = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                    var l: String?; while (r.readLine().also { l = it } != null) out.appendLine(l)
                }
            }
            val t2 = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                    var l: String?; while (r.readLine().also { l = it } != null) err.appendLine(l)
                }
            }
            t1.start(); t2.start()
            val code = waitWithTimeout(process, timeoutMs)
            runCatching { t1.join(2000); t2.join(2000) }
            Triple(code, out.toString().trim(), err.toString().trim())
        }.getOrElse { t ->
            Log.w(TAG, "exec 失败", t)
            Triple(-1, "", t.message ?: "执行异常")
        }
    }

    private fun waitWithTimeout(p: ShizukuRemoteProcess, timeoutMs: Long): Int {
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < timeoutMs) {
            runCatching { if (!p.isAlive) return p.exitValue() }
            Thread.sleep(60)
        }
        runCatching { p.destroy() }
        return -2
    }

    /** 是否支持 Shizuku Binder 直连（比 shell 快、类型安全） */
    fun binderAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
}
