package com.github.rodrigotimoteo.kboyemu.presentation.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.github.rodrigotimoteo.kboyemu.domain.emulation.EmulationSpeed
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.ResetEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.SetEmulationSpeedUseCase
import com.github.rodrigotimoteo.kboyemu.domain.emulation.usecase.StopEmulationUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.ExportSaveGameUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.ImportSaveGameUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.LoadSaveStateUseCase
import com.github.rodrigotimoteo.kboyemu.domain.savestate.usecase.SaveSaveStateUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the settings screen. Handles save/load state actions, emulation speed control,
 * save game import/export, reset, and stopping emulation when changing ROMs.
 *
 * @author rodrigotimoteo
 */
@KoinViewModel
class SettingsViewModel(
    private val saveSaveStateUseCase: SaveSaveStateUseCase,
    private val loadSaveStateUseCase: LoadSaveStateUseCase,
    private val stopEmulationUseCase: StopEmulationUseCase,
    private val resetEmulationUseCase: ResetEmulationUseCase,
    private val setEmulationSpeedUseCase: SetEmulationSpeedUseCase,
    private val importSaveGameUseCase: ImportSaveGameUseCase,
    private val exportSaveGameUseCase: ExportSaveGameUseCase,
    private val emulator: KBoyEmulator,
) : ViewModel() {

    private val _speed = MutableStateFlow(
        EmulationSpeed.entries.firstOrNull { it.multiplier == emulator.speedMultiplier }
            ?: EmulationSpeed.NORMAL
    )

    /** Currently selected emulation speed */
    val speed: StateFlow<EmulationSpeed> = _speed.asStateFlow()

    /** Default export filename based on the loaded ROM title */
    val exportFileName: String
        get() = "${emulator.romTitle}.sav"

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

    /**
     * Resets the emulator to its initial state while preserving the save game
     */
    fun resetEmulation() = resetEmulationUseCase()

    /**
     * Changes the emulation speed to the given [EmulationSpeed]
     *
     * @param speed desired emulation speed
     */
    fun setSpeed(speed: EmulationSpeed) {
        setEmulationSpeedUseCase(speed)
        _speed.value = speed
    }

    /**
     * Imports a .sav file from the given content URI into the emulator's ERAM
     *
     * @param uri content URI of the .sav file
     * @return true if the import succeeded
     */
    fun importSaveGame(uri: String): Boolean = importSaveGameUseCase(uri)

    /**
     * Exports the current ERAM to the given content URI as a .sav file
     *
     * @param uri content URI where the .sav file should be written
     * @return true if the export succeeded
     */
    fun exportSaveGame(uri: String): Boolean = exportSaveGameUseCase(uri)
}
