package com.github.rodrigotimoteo.kboyemu

import android.app.Application
import com.github.rodrigotimoteo.kboyemu.di.kBoyEmulatorModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.core.context.startKoin
import timber.log.Timber

/** [Application] class for KBoyEmulator Android App */
@KoinApplication
class KBoyEmulatorApplication: Application() {

    /** [onCreate]: Setup Timber logging */
    override fun onCreate() {
        super.onCreate()
        setupTimber()

        startKoin {
            androidContext(this@KBoyEmulatorApplication)
            modules(kBoyEmulatorModule)
        }
    }

    /** Setup [Timber], modify log tags and log app version */
    private fun setupTimber() {
        Timber.plant(
            object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    val className = super.createStackElementTag(element)
                    return "KBoyEmulatorApp::$className::${element.methodName}"
                }
            },
        )
        Timber.i("Starting KBoyEmulator Application")
    }
}
