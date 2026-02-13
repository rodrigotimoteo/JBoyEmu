package com.github.rodrigotimoteo.kboyemu.di

import com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel.KBoyEmulatorViewModel
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val kBoyEmulatorModule = module {
    single { KBoyEmulatorFactory(get()) }
    viewModel { KBoyEmulatorViewModel(get(), get()) }
}
