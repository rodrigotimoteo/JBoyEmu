package com.github.rodrigotimoteo.kboyemucore.api

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * This interface represents a contract between the consumers of the emulator such as an external
 * application for this specific purpose it will be the Android application in the app module.
 * It exposes all methods needed and the information required by the android part of the project to
 * draw and interact with the Game Boy
 *
 * @author rodrigotimoteo
 */
interface KBoyEmulator {

    /**
     * Loads a [Rom] which is an abstraction of a [ByteArray] that represents the content of the file
     * to be used in the emulator
     *
     * @param rom to be used in the emulator
     */
    fun loadRom(rom: Rom)

    /**
     * Clears everything and starts a "clean sheat" of the emulator (basically what a reset button
     * does)
     */
    fun reset()

    /**
     * Tells the emulator that a button is being pressed and inject this specific button in the
     * emulator execution flow
     *
     * @param button that is being pressed
     */
    fun press(button: Button)

    /**
     * Tells the emulator that a button is no longer being pressed and stops injecting this into the
     * emulator execution flow
     *
     * @param button that is being released
     */
    fun release(button: Button)

    /**
     * Starts the emulator execution
     */
    fun run()

    /**
     * This gives back the job created in [run] to the consumer basically enabling the consumer when
     * running headless to own the lifecycle of the emulator (and join its execution)
     *
     * @return Job created in [run]
     */
    fun job(): Job?

    /**
     * Pauses the emulator execution
     */
    fun pause()

    /**
     * [Flow] of [FrameBuffer] that exposes a [IntArray] that a consumer can use to display what is
     * being shown on the emulator
     */
    val frames: Flow<FrameBuffer>
}
