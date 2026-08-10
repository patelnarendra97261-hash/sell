package com.example

import android.app.Application
import android.util.Log

class LiquorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isGmsSecurityException = (throwable is SecurityException &&
                    (throwable.message?.contains("com.google.android.gms") == true ||
                            throwable.stackTrace.any { it.className.contains("com.google.android.gms") }))

            if (isGmsSecurityException) {
                Log.w("LiquorApp", "Safely intercepted GMS background broker SecurityException on emulator: ${throwable.message}")
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
