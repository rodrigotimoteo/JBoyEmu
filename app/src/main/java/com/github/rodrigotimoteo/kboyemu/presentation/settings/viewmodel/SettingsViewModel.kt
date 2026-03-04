package com.github.rodrigotimoteo.kboyemu.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.StopEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.LoadSaveStateUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.SaveSaveStateUseCase
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the settings screen. Handles save/load state actions and stopping emulation
 * when changing ROMs.
 *
 * @author rodrigotimoteo
 */
@KoinViewModel
class SettingsViewModel(
    private val saveSaveStateUseCase: SaveSaveStateUseCase,
    private val loadSaveStateUseCase: LoadSaveStateUseCase,
    private val stopEmulationUseCase: StopEmulationUseCase,
) : ViewModel() {

    /**
     * Saves the current emulator state
     */
    fun saveState() = saveSaveStateUseCase()

    /**
     * Loads the last saved emulator state
     */
    fun loadState() = loadSaveStateUseCase()

    /**
     * Stops the current emulation session so the user can pick a new ROM
     */
    fun stopEmulation() = stopEmulationUseCase()
}

