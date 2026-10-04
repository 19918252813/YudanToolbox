// ============================================================
//  版本全部锁死，且互相咬合。不要随意升级任何一个。
//  AGP 8.7.3 → 必须 Gradle 8.9
//  Kotlin 2.0.21 → compose 插件与 serialization 插件必须同为 2.0.21
// ============================================================
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Kotlin 2.0+ 起 Compose 编译器并入 Kotlin 仓库，版本必须与 Kotlin 完全一致
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
