// ============================================================
//  零失败取向：依赖最小化，全部字面量版本
//  ✗ 无 Room / 无 KSP / 无 kotlinx-serialization / 无 libsu / 无 jitpack
//  ✗ R8 混淆全关
// ============================================================
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.yudan.toolbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yudan.toolbox"
        minSdk = 23            // Shizuku 要求 Android 6.0+
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += setOf("zh")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // ⚠ 关闭 R8：R8 失败模式诡异（Missing classes / 资源缩减冲突），
            //    对项目稳定性收益远大于体积收益。稳定后再考虑开启。
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    // 注意：Kotlin 2.0+ 不要再写 composeOptions { kotlinCompilerExtensionVersion }
    // 由 org.jetbrains.kotlin.plugin.compose 插件自动处理

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // ---------- AndroidX 基础 ----------
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ---------- Compose（BOM 统一管理）----------
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- Shizuku（Maven Central，免 Root 权限桥）----------
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // ---------- 协程 ----------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ⚠ 以下全部移除，原因见 README：
    //   libsu             → jitpack 超时，改为自实现 Runtime.exec("su")
    //   Room + KSP        → 注解处理器失败高发，改为 SharedPreferences
    //   kotlinx-serialization → 编译器插件风险，改为 Android 内置 org.json
}
