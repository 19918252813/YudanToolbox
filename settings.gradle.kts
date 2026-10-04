pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ⚠ 故意不声明 jitpack.io —— 它在 GitHub Actions 上经常超时，
        //    是上次 "1分6秒 All jobs have failed" 的元凶。
        //    本项目已完全移除 jitpack 依赖（libsu → 自实现 Runtime.exec("su")）。
    }
}

rootProject.name = "YudanToolbox"
include(":app")
