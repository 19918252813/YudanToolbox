package com.yudan.toolbox.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地存储 —— 用 SharedPreferences + JSON 实现，替代 Room。
 *
 * 为什么不用 Room：Room 依赖 KSP 注解处理器，是 CI 编译失败的高发区。
 * 本项目只需存「收藏列表」和「最近 300 条历史」这种小数据，
 * SharedPreferences 完全够用，且零编译风险。
 */
object Store {

    private const val P = "yudan_store"
    private const val K_FAV = "favorites"
    private const val K_HIST = "history"
    private const val MAX_HIST = 300

    private fun sp(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)

    // ---------- 收藏 ----------
    fun favorites(ctx: Context): List<Favorite> {
        val s = sp(ctx).getString(K_FAV, "[]") ?: "[]"
        return try {
            val arr = JSONArray(s)
            val out = ArrayList<Favorite>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    Favorite(
                        featureId = o.optString("id"),
                        name = o.optString("name"),
                        moduleName = o.optString("mod"),
                        addedAt = o.optLong("at", 0L)
                    )
                )
            }
            out.sortedByDescending { it.addedAt }
        } catch (_: Throwable) { emptyList() }
    }

    fun isFav(ctx: Context, id: String): Boolean =
        favorites(ctx).any { it.featureId == id }

    fun toggleFav(ctx: Context, f: Feature) {
        val list = favorites(ctx).toMutableList()
        if (list.any { it.featureId == f.id }) {
            list.removeAll { it.featureId == f.id }
        } else {
            list.add(0, Favorite(f.id, f.name, f.moduleName))
        }
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.featureId); put("name", it.name)
                put("mod", it.moduleName); put("at", it.addedAt)
            })
        }
        sp(ctx).edit().putString(K_FAV, arr.toString()).apply()
    }

    // ---------- 历史 ----------
    fun history(ctx: Context): List<History> {
        val s = sp(ctx).getString(K_HIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(s)
            val out = ArrayList<History>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    History(
                        id = o.optLong("i", i.toLong()),
                        featureId = o.optString("fid"),
                        featureName = o.optString("fname"),
                        cmd = o.optString("cmd"),
                        effective = o.optBoolean("ok", false),
                        detail = o.optString("detail"),
                        level = o.optInt("lv", 0),
                        at = o.optLong("at", 0L)
                    )
                )
            }
            out.sortedByDescending { it.at }
        } catch (_: Throwable) { emptyList() }
    }

    fun addHistory(ctx: Context, h: History) {
        val list = history(ctx).toMutableList()
        list.add(0, h.copy(id = System.currentTimeMillis()))
        val trimmed = list.take(MAX_HIST)
        val arr = JSONArray()
        trimmed.forEach {
            arr.put(JSONObject().apply {
                put("i", it.id); put("fid", it.featureId); put("fname", it.featureName)
                put("cmd", it.cmd); put("ok", it.effective)
                put("detail", it.detail); put("lv", it.level); put("at", it.at)
            })
        }
        sp(ctx).edit().putString(K_HIST, arr.toString()).apply()
    }

    fun clearHistory(ctx: Context) {
        sp(ctx).edit().putString(K_HIST, "[]").apply()
    }
}
