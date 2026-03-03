package com.github.rodrigotimoteo.kboyemu.di

import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import org.koin.dsl.module

/**
 * Manual Koin module for dependencies that live outside this module's annotation scan scope
 * (e.g. types produced by factory methods in `kboyemucore`). Everything else is auto-discovered
 * by KSP via [AppModule].
 */
val kBoyEmulatorModule = module {
    single<KBoyEmulator> { KBoyEmulatorFactory(get()) }
}
