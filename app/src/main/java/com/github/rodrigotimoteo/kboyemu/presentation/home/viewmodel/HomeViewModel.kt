package com.github.rodrigotimoteo.kboyemu.presentation.home.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.github.rodrigotimoteo.kboyemu.domain.rom.usecase.LoadRomUseCase
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for the home (ROM picker) screen. Responsible for loading a ROM into the emulator
 * when the user selects a file.
 *
 * @author rodrigotimoteo
 */
@KoinViewModel
class HomeViewModel(
    private val loadRomUseCase: LoadRomUseCase,
) : ViewModel() {

    /**
     * Loads a ROM from the given content [Uri] and starts emulation
     *
     * @param uri content URI of the ROM file selected by the user
     * @return true if the ROM was loaded successfully
     */
    fun loadRom(uri: Uri): Boolean = loadRomUseCase(uri.toString())
}

