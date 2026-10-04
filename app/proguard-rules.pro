# ============================================================
#  鱼蛋工具箱 ProGuard/R8 混淆加密规则 v2
#  作用: 类名/方法名/变量名混淆, 反编译后难以阅读
# ============================================================

# ---------- 基础保留 ----------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively

# ---------- 日志/调试信息擦除 ----------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ---------- Room 数据库(必须保留, 否则反射失败) ----------
-keep class com.yudan.toolbox.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---------- Shizuku ----------
-dontwarn rikka.shizuku.**
-keep class rikka.shizuku.** { *; }
-keep class * implements rikka.shizuku.Shizuku.OnBinderReceivedListener { *; }

# ---------- libsu Root ----------
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# ---------- Compose 不混淆 UI 组件(避免反射问题) ----------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ---------- Kotlin 元数据 ----------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---------- 序列化(features.json 解析) ----------
-keepattributes *Annotation*
-keep,includedescriptorclasses class com.yudan.toolbox.**$$serializer { *; }
-keepclassmembers class com.yudan.toolbox.** {
    *** Companion;
}
-keepclasseswithmembers class com.yudan.toolbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- 混淆优化开关 ----------
-allowaccessmodification
-flattenpackagehierarchy 'obf'
