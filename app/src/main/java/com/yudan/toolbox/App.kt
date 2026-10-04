package com.yudan.toolbox

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf

class App : Application() {
    companion object {
        @Volatile lateinit var instance: App
            private set
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
