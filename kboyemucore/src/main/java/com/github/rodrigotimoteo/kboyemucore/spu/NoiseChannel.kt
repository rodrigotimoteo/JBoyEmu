package com.github.rodrigotimoteo.kboyemucore.spu

import com.github.rodrigotimoteo.kboyemucore.api.NoiseChannelState

/**
 * Noise channel (CH4) that generates pseudo-random noise using a linear feedback shift register
 * (LFSR). Supports configurable clock frequency, 7-bit or 15-bit LFSR width, volume envelope,
 * and optional length counter.
 *
 * @author rodrigotimoteo
 */
class NoiseChannel {

    /** Whether the channel is currently producing output */
    var enabled = false
        private set

    /** Current DAC output in the range 0-15 (0 when disabled) */
    var output = 0
        private set

    private var lengthCounter = 0
    private var lengthEnabled = false

    private var volume = 0
    private var envelopeInitial = 0
    private var envelopeAdd = false
    private var envelopePeriod = 0
    private var envelopeTimer = 0

    private var lfsr = 0x7FFF
    private var width7 = false
    private var clockShift = 0
    private var divisorCode = 0
    private var frequencyTimer = 0

    private val divisors = intArrayOf(8, 16, 32, 48, 64, 80, 96, 112)

    private var rawNr42 = 0
    private var rawNr43 = 0
    private var rawNr44 = 0

    /**
     * Writes the length load value (NR41)
     *
     * @param v register value
     */
    fun writeLengthLoad(v: Int) {
        lengthCounter = 64 - (v and 0x3F)
    }

    /**
     * Writes to the volume envelope register (NR42). Disables the channel if the DAC is off.
     *
     * @param v register value
     */
    fun writeEnvelope(v: Int) {
        rawNr42 = v
        envelopeInitial = (v shr 4) and 0x0F
        envelopeAdd = v and 0x08 != 0
        envelopePeriod = v and 0x07
        if (v and 0xF8 == 0) enabled = false
    }

    /**
     * Writes the polynomial counter register (NR43), configuring clock shift, LFSR width,
     * and divisor code
     *
     * @param v register value
     */
    fun writePolynomial(v: Int) {
        rawNr43 = v
        clockShift = (v shr 4) and 0x0F
        width7 = v and 0x08 != 0
        divisorCode = v and 0x07
    }

    /**
     * Writes the control register (NR44). Triggers the channel when bit 7 is set.
     *
     * @param v register value
     */
    fun writeControl(v: Int) {
        rawNr44 = v
        lengthEnabled = v and 0x40 != 0
        if (v and 0x80 != 0) trigger()
    }

    fun readEnvelope(): Int = rawNr42
    fun readPolynomial(): Int = rawNr43
    fun readControl(): Int = rawNr44 and 0x40

    /**
     * Triggers the channel, reloading length, frequency timer, envelope, and LFSR
     */
    private fun trigger() {
        enabled = true
        if (lengthCounter == 0) lengthCounter = 64
        frequencyTimer = divisors[divisorCode] shl clockShift
        envelopeTimer = envelopePeriod
        volume = envelopeInitial
        lfsr = 0x7FFF
        if (rawNr42 and 0xF8 == 0) enabled = false
    }

    /**
     * Clocks the length counter, disabling the channel when it reaches zero
     */
    fun clockLength() {
        if (lengthEnabled && lengthCounter > 0) {
            if (--lengthCounter == 0) enabled = false
        }
    }

    /**
     * Clocks the volume envelope, adjusting [volume] up or down based on the envelope direction
     */
    fun clockEnvelope() {
        if (envelopePeriod == 0) return
        if (--envelopeTimer <= 0) {
            envelopeTimer = envelopePeriod
            if (envelopeAdd && volume < 15) volume++
            else if (!envelopeAdd && volume > 0) volume--
        }
    }

    /**
     * Advances the channel by [cycles] T-cycles, clocking the LFSR and updating the [output] value
     *
     * @param cycles elapsed T-cycles
     */
    fun tick(cycles: Int) {
        if (!enabled) {
            output = 0; return
        }
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += divisors[divisorCode] shl clockShift
            val xorBit = (lfsr xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (xorBit shl 14)
            if (width7) lfsr = (lfsr and 0x7FBF) or (xorBit shl 6)
        }
        output = if (lfsr and 1 == 0) volume else 0
    }

    /**
     * Resets all internal state to power-off defaults
     */
    fun reset() {
        enabled = false; output = 0
        lengthCounter = 0; lengthEnabled = false
        volume = 0; envelopeInitial = 0; envelopeAdd = false
        envelopePeriod = 0; envelopeTimer = 0
        lfsr = 0x7FFF; width7 = false; clockShift = 0; divisorCode = 0
        frequencyTimer = 0
        rawNr42 = 0; rawNr43 = 0; rawNr44 = 0
    }

    /**
     * Captures the current channel state for save state serialization
     *
     * @return snapshot of all internal channel state
     */
    fun saveState(): NoiseChannelState = NoiseChannelState(
        enabled, output, lengthCounter, lengthEnabled, volume,
        envelopeInitial, envelopeAdd, envelopePeriod, envelopeTimer,
        lfsr, width7, clockShift, divisorCode, frequencyTimer,
        rawNr42, rawNr43, rawNr44,
    )

    /**
     * Restores the channel from a previously captured save state
     *
     * @param s saved channel state to restore
     */
    fun loadState(s: NoiseChannelState) {
        enabled = s.enabled; output = s.output
        lengthCounter = s.lengthCounter; lengthEnabled = s.lengthEnabled
        volume = s.volume; envelopeInitial = s.envelopeInitial
        envelopeAdd = s.envelopeAdd; envelopePeriod = s.envelopePeriod
        envelopeTimer = s.envelopeTimer; lfsr = s.lfsr; width7 = s.width7
        clockShift = s.clockShift; divisorCode = s.divisorCode
        frequencyTimer = s.frequencyTimer
        rawNr42 = s.rawNr42; rawNr43 = s.rawNr43; rawNr44 = s.rawNr44
    }
}
