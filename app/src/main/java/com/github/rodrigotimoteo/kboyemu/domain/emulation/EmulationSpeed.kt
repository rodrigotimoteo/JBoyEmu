package com.github.rodrigotimoteo.kboyemu.domain.emulation

/**
 * Available emulation speed options. Each entry maps to a multiplier used by the emulator core
 * for frame pacing.
 *
 * @param multiplier speed factor (0 = unlimited, 1 = 60 fps, 2 = 120 fps, etc.)
 * @param label human-readable label displayed in the UI
 *
 * @author rodrigotimoteo
 */
enum class EmulationSpeed(val multiplier: Int, val label: String) {
    NORMAL(1, "100%"),
    DOUBLE(2, "200%"),
    TRIPLE(3, "300%"),
    QUINTUPLE(5, "500%"),
    UNLIMITED(0, "Unlimited"),
}

