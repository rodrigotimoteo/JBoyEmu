package com.github.rodrigotimoteo.kboyemu.presentation.navigation

/**
 * Type-safe navigation route definitions for the app
 *
 * @author rodrigotimoteo
 */
sealed class Route(val path: String) {

    /** ROM selection home screen */
    data object Home : Route("home")

    /** Active emulator screen */
    data object Emulator : Route("emulator")

    /** Settings / options screen */
    data object Settings : Route("settings")
}

