package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator

/**
 * Exposes a way for consumers to create instances of the Emulator
 *
 * @author rodrigotimoteo
 */
object KBoyEmulatorFactory {

    /**
     * Creates and returns a new instance of a [KBoyEmulator]
     *
     * @return a [KBoyEmulator] to be used by a consumer
     */
    operator fun invoke(): KBoyEmulator {
        return KBoyEmulatorImpl()
    }
}
