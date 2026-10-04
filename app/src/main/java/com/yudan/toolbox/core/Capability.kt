package com.yudan.toolbox.core

/**
 * 能力分级模型。
 *
 * 设计原则（来自技术方案）：等级不是"降级标签"，而是每个功能项在 UI、执行、回滚中的事实来源。
 * 绝不"灰显后点一下就静默调用 Root"——L2 功能必须在用户主动开启 Root 后端时才执行。
 */
enum class Level(val value: Int, val label: String, val short: String) {
    /** 普通 App 沙箱：只读、本地能力 */
    L0(0, "普通权限", "免Root"),
    /** Shizuku / adb 级 (uid 2000)：settings/pm/wm/appops/dumpsys 等 */
    L1(1, "Shizuku 权限", "Shizuku"),
    /** Root / su (uid 0)：分区、系统文件、模块、resetprop 等 */
    L2(2, "Root 权限", "需Root");

    companion object {
        fun of(v: Int) = entries.firstOrNull { it.value == v } ?: L0
    }
}

/** 执行后端模式——用户可在设置里切换 */
enum class BackendMode(val label: String, val desc: String) {
    /** 免 Root 模式：只用 Shizuku（adb 级）。Root 功能显示"需要 Root"，不执行 */
    SHIZUKU("免 Root 模式", "通过 Shizuku 获取 adb 级权限，覆盖 settings/pm/wm/appops 等"),
    /** Root 模式：su 优先，失败自动降级 Shizuku */
    ROOT("Root 模式", "已 Root 设备，su 执行全部功能；失败时自动降级到 Shizuku"),
    /** 只读模式：不申请任何提权，仅展示信息 */
    READONLY("只读模式", "不申请任何权限，仅查看设备信息、诊断与导出报告")
}

/** 功能项校验类型 —— 对应 SH 脚本里的"写后回读校验" */
enum class VerifyType {
    NONE,               // 只跑命令，不校验
    SETTINGS_PUT,       // settings put → settings get 回读比对
    SETTINGS_DELETE,    // settings delete → get 应为 null/空
    WM,                 // wm size/density → wm xxx 回读包含期望值
    PM_DISABLED,        // pm disable → 出现在 pm list packages -d
    PM_ENABLED,         // pm enable → 从 -d 列表消失
    APPOPS              // appops set → appops get 回读
}

/** Shizuku / Root 的可用性状态 */
data class BackendState(
    val mode: BackendMode = BackendMode.SHIZUKU,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuPermission: Boolean = false,
    val shizukuUid: Int = -1,
    val rootAvailable: Boolean = false,
    val rootGranted: Boolean = false,
    val capable: Level = Level.L0,
    val message: String = "未初始化"
) {
    /** 当前实际可用的最高等级 */
    fun canRun(required: Level): Boolean = required.value <= capable.value
}
