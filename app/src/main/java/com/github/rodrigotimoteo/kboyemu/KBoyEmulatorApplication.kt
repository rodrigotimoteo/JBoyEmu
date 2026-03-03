package com.github.rodrigotimoteo.kboyemu

import android.app.Application
import com.github.rodrigotimoteo.kboyemu.di.AppModule
import com.github.rodrigotimoteo.kboyemu.di.kBoyEmulatorModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import timber.log.Timber

/** [Application] class for KBoyEmulator Android App */
class KBoyEmulatorApplication: Application() {

    /** [onCreate]: Setup Timber logging */
    override fun onCreate() {
        super.onCreate()
        setupTimber()

        startKoin {
            androidContext(this@KBoyEmulatorApplication)
            modules(AppModule().module, kBoyEmulatorModule)
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
