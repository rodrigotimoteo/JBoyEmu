package com.github.rodrigotimoteo.kboyemu.presentation.emulator.uistate

/**
 * Represents the current state of the emulator screen
 */
sealed interface EmulatorUiState {
    data object WaitingForRom : EmulatorUiState
    data object Running : EmulatorUiState
}
