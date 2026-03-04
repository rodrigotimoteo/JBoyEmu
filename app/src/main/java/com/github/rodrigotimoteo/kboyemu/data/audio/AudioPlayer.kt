package com.github.rodrigotimoteo.kboyemu.data.audio

import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer

/**
 * Contract for the platform audio output. Consumes PCM samples from the emulator's
 * [AudioRingBuffer] and feeds them to the device speaker. Lifecycle is tied to the emulator
 * session — call [start] after loading a ROM and [stop] when the session ends.
 *
 * @author rodrigotimoteo
 */
interface AudioPlayer {

    /**
     * Starts consuming samples from the given [AudioRingBuffer] and playing them. This call is
     * idempotent — calling it twice has no effect.
     *
     * @param ringBuffer source of stereo PCM samples produced by the SPU
     */
    fun start(ringBuffer: AudioRingBuffer)

    /**
     * Stops playback and releases all platform resources. After this call the player cannot be
     * restarted.
     */
    fun stop()
}

