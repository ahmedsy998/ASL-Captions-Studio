package com.example

import android.app.Application
import android.util.Log
import com.example.ai.ensureFirebaseInitialized

class StudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val initialized = ensureFirebaseInitialized(this)
        Log.i("StudioApplication", "Firebase process initialization status: $initialized")
    }
}
