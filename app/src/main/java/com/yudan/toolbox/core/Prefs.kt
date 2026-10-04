package com.yudan.toolbox.core

import android.content.Context
import android.content.SharedPreferences

/** 轻量配置：后端模式、主题、危险操作确认等 */
object Prefs {
    private const val F = "yudan_prefs"
    private const val K_MODE = "backend_mode"
    private const val K_DARK = "dark_mode"
    private const val K_CONFIRM = "confirm_risky"
    private const val K_AUTHORIZED_ROOT = "authorized_root"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(F, Context.MODE_PRIVATE)

    var mode: BackendMode
        get() = BackendMode.entries.firstOrNull { it.name == tmpMode } ?: BackendMode.SHIZUKU
        set(v) { tmpMode = v.name }

    private var tmpMode: String? = null

    fun getMode(ctx: Context): BackendMode =
        BackendMode.entries.firstOrNull { it.name == sp(ctx).getString(K_MODE, BackendMode.SHIZUKU.name) }
            ?: BackendMode.SHIZUKU

    fun setMode(ctx: Context, m: BackendMode) { sp(ctx).edit().putString(K_MODE, m.name).apply() }

    fun isDark(ctx: Context): Boolean = sp(ctx).getBoolean(K_DARK, true)
    fun setDark(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean(K_DARK, v).apply() }

    fun confirmRisky(ctx: Context): Boolean = sp(ctx).getBoolean(K_CONFIRM, true)
    fun setConfirmRisky(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean(K_CONFIRM, v).apply() }

    /** 用户是否主动确认过"我要用 Root"——L2 功能的前提 */
    fun rootAuthorized(ctx: Context): Boolean = sp(ctx).getBoolean(K_AUTHORIZED_ROOT, false)
    fun setRootAuthorized(ctx: Context, v: Boolean) { sp(ctx).edit().putBoolean(K_AUTHORIZED_ROOT, v).apply() }
}
