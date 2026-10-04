package com.yudan.toolbox.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 功能清单仓库：加载 assets/features.json
 *
 * 用 Android 内置 org.json 解析 —— 零依赖、零编译器插件，
 * 不会像 kotlinx-serialization 那样因插件版本不对而失败。
 */
object FeatureRepo {

    private var cache: List<Feature>? = null
    private var modCache: List<Module>? = null

    fun load(ctx: Context): List<Feature> {
        cache?.let { return it }
        val list = try {
            val text = ctx.assets.open("features.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val arr: JSONArray = root.optJSONArray("features") ?: JSONArray()
            val out = ArrayList<Feature>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val cmdsArr = o.optJSONArray("cmds")
                val cmds = ArrayList<String>()
                if (cmdsArr != null) {
                    for (j in 0 until cmdsArr.length()) {
                        cmdsArr.optString(j).takeIf { it.isNotBlank() }?.let { cmds.add(it) }
                    }
                }
                val sub = o.optString("submenu")
                out.add(
                    Feature(
                        id = o.optString("id"),
                        module = o.optString("module"),
                        moduleName = o.optString("moduleName"),
                        moduleIcon = o.optString("moduleIcon"),
                        index = o.optInt("index"),
                        name = o.optString("name"),
                        level = o.optInt("level", 0),
                        risky = o.optBoolean("risky", false),
                        cmds = cmds,
                        verify = o.optString("verify", "none"),
                        needsInput = o.optBoolean("needsInput", false),
                        submenu = if (sub.isNullOrBlank()) null else sub
                    )
                )
            }
            out
        } catch (t: Throwable) {
            android.util.Log.w("FeatureRepo", "features.json 解析失败", t)
            emptyList()
        }
        cache = list
        return list
    }

    fun modules(ctx: Context): List<Module> {
        modCache?.let { return it }
        val mods = load(ctx).groupBy { it.module }.map { (key, items) ->
            val first = items.first()
            Module(key, first.moduleName, first.moduleIcon, items.sortedBy { it.index })
        }.sortedBy { key0(it.key) }
        modCache = mods
        return mods
    }

    private fun key0(k: String): Int = when (k) {
        "info" -> 1; "app" -> 2; "perm" -> 3; "perf" -> 4; "disp" -> 5; "batt" -> 6
        "net" -> 7; "sys" -> 8; "backup" -> 9; "file" -> 10; "log" -> 11; "input" -> 12
        "ad" -> 13; "game" -> 14; "audio" -> 15; "auto" -> 16; "bloat" -> 17
        "finger" -> 18; "cert" -> 19; "user" -> 20; "magic" -> 21; "root" -> 22
        "magisk" -> 23; "xposed" -> 24; "kernel" -> 25; "help" -> 26; "about" -> 27
        "flash" -> 28; else -> 99
    }

    fun byId(ctx: Context, id: String): Feature? = load(ctx).firstOrNull { it.id == id }

    fun search(ctx: Context, q: String): List<Feature> {
        if (q.isBlank()) return emptyList()
        return load(ctx).filter {
            it.name.contains(q, true) || it.id.contains(q, true) || it.moduleName.contains(q, true)
        }.take(100)
    }
}
