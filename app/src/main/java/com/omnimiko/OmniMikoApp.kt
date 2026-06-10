package com.omnimiko

import android.app.Application
import com.omnimiko.di.AppContainer

/** Application entry point. Holds the process-wide [AppContainer]. */
class OmniMikoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Use the mock engine in debug builds so the app runs end-to-end without
        // a multi-gigabyte model present. Release builds use on-device inference.
        container = AppContainer(this, useMockEngine = BuildConfig.DEBUG)
    }
}
