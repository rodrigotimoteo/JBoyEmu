package com.github.rodrigotimoteo.kboyemu.di

import com.github.rodrigotimoteo.kboyemu.util.EmulatorLogger
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.KBoyEmulatorViewModel
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val kBoyEmulatorModule = module {
    single<Logger> { EmulatorLogger() }
    single { KBoyEmulatorFactory(get()) }
    viewModel { KBoyEmulatorViewModel(get(), get()) }
}
