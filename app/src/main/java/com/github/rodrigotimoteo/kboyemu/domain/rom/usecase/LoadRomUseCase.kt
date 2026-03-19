package com.github.rodrigotimoteo.kboyemu.domain.rom.usecase

import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemu.data.rom.RomRepository
import com.github.rodrigotimoteo.kboyemu.data.savegame.SaveGameRepository
import com.github.rodrigotimoteo.kboyemu.data.savestate.SaveStateRepository
import com.github.rodrigotimoteo.kboyemu.domain.savegame.usecase.LoadGameUseCase
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import org.koin.core.annotation.Single

/**
 * Use case that orchestrates loading a ROM into the emulator. Reads the ROM bytes via
 * [RomRepository], registers the ROM hash with [SaveStateRepository] and [SaveGameRepository],
 * loads the ROM into the [KBoyEmulator], restores any battery-backed save game, starts the
 * emulation loop, and starts audio playback.
 *
 * @author rodrigotimoteo
 */
@Single
class LoadRomUseCase(
    private val romRepository: RomRepository,
    private val saveStateRepository: SaveStateRepository,
    private val saveGameRepository: SaveGameRepository,
    private val emulator: KBoyEmulator,
    private val audioPlayer: AudioPlayer,
    private val loadGameUseCase: LoadGameUseCase,
) {

    /**
     * Loads a ROM from the given content URI string. Returns the emulator's frame flow on success,
     * or null if the ROM could not be read.
     *
     * @param uri string representation of the content URI
     * @return true if the ROM was loaded successfully, false otherwise
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    operator fun invoke(uri: String): Boolean {
        val romBytes = romRepository.loadFromUri(uri) ?: return false

        saveStateRepository.setRomHash(romBytes)
        saveGameRepository.setRomHash(romBytes)
        emulator.loadRom(Rom(romBytes))
        loadGameUseCase()
        audioPlayer.start(emulator.audioRingBuffer)
        emulator.run()

        return true
    }
}