package com.yudan.toolbox.core

/**
 * 功能项：由 assets/features.json 驱动（从 SH 脚本自动提取生成）。
 *
 * 注意：不再使用 kotlinx-serialization（编译器插件是失败高发区），
 * 改用 Android 内置的 org.json 手写解析，零依赖、零插件。
 */
data class Feature(
    val id: String,
    val module: String,
    val moduleName: String,
    val moduleIcon: String,
    val index: Int,
    val name: String,
    /** 所需能力等级：0=L0 1=L1(Shizuku) 2=L2(Root) */
    val level: Int = 0,
    val risky: Boolean = false,
    val cmds: List<String> = emptyList(),
    val verify: String = "none",
    val needsInput: Boolean = false,
    val submenu: String? = null
) {
    val requiredLevel: Level get() = Level.of(level)
    val verifyType: VerifyType
        get() = when (verify) {
            "settings_put" -> VerifyType.SETTINGS_PUT
            "settings_delete" -> VerifyType.SETTINGS_DELETE
            "wm" -> VerifyType.WM
            "pm_disabled" -> VerifyType.PM_DISABLED
            "appops" -> VerifyType.APPOPS
            else -> VerifyType.NONE
        }
}

/** 模块分组 */
data class Module(
    val key: String,
    val name: String,
    val icon: String,
    val items: List<Feature>
)

/** 执行结果 */
data class ExecResult(
    val featureId: String,
    val cmd: String,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = -1,
    /** 是否真的生效（写后回读校验通过才算） */
    val effective: Boolean = false,
    val verifyInfo: String = "",
    val failureReason: String = "",
    val level: Level = Level.L0,
    val durationMs: Long = 0
) {
    /** 把系统返回翻译成人话 —— 沿用 SH 脚本的 _diag 逻辑 */
    fun humanReason(): String = when {
        "not found" in stderr || "not found" in stdout ->
            "当前环境没有这条命令（缺少 toybox / 不是完整 Android shell）"
        "Background activity start blocked" in stdout || "blocked" in stdout ->
            "Android 10+ 后台启动限制：后台进程不允许直接弹页面"
        "Permission Denial" in stdout || "Permission denied" in stdout ||
                "not permitted" in stdout || "SecurityException" in stdout ->
            "权限不足：需要 Shizuku / Root 授权"
        "Unknown package" in stdout || "unknown package" in stdout ->
            "包名不存在"
        "read-only" in stdout || "Read-only" in stdout ->
            "分区只读：需 Root 重新挂载为可写"
        exitCode != 0 && stdout.isBlank() ->
            "命令退出码 $exitCode，无输出"
        else -> failureReason.ifEmpty { "写入后回读校验未通过" }
    }
}

/** 收藏项（SharedPreferences 存储，替代 Room） */
data class Favorite(
    val featureId: String,
    val name: String,
    val moduleName: String,
    val addedAt: Long = System.currentTimeMillis()
)

/** 历史记录（SharedPreferences 存储，替代 Room） */
data class History(
    val id: Long = 0,
    val featureId: String,
    val featureName: String,
    val cmd: String,
    val effective: Boolean,
    val detail: String,
    val level: Int,
    val at: Long = System.currentTimeMillis()
)
