package com.github.rodrigotimoteo.kboyemucore.spu

import com.github.rodrigotimoteo.kboyemucore.api.SquareChannelState

/**
 * Square-wave channel used for CH1 and CH2. CH1 includes frequency sweep functionality controlled
 * by the [hasSweep] flag. Produces a square wave with configurable duty cycle, volume envelope,
 * and optional length counter.
 *
 * @param hasSweep whether this channel supports frequency sweep (true for CH1, false for CH2)
 *
 * @author rodrigotimoteo
 */
class SquareChannel(private val hasSweep: Boolean) {

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

    private var frequencyRaw = 0
    private var dutyIndex = 0
    private var dutyStep = 0
    private var frequencyTimer = 0

    private var sweepPeriod = 0
    private var sweepNegate = false
    private var sweepShift = 0
    private var sweepTimer = 0
    private var sweepEnabled = false
    private var sweepShadow = 0

    private var rawNrX0 = 0
    private var rawNrX1 = 0
    private var rawNrX2 = 0
    private var rawNrX4 = 0

    private val dutyTable = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, true),
        booleanArrayOf(true, false, false, false, false, false, false, true),
        booleanArrayOf(true, false, false, false, false, true, true, true),
        booleanArrayOf(false, true, true, true, true, true, true, false),
    )

    /**
     * Writes to the sweep register (NR10). Only has effect when [hasSweep] is true.
     *
     * @param v register value
     */
    fun writeSweep(v: Int) {
        if (!hasSweep) return
        rawNrX0 = v
        sweepPeriod = (v shr 4) and 7
        sweepNegate = v and 0x08 != 0
        sweepShift = v and 0x07
    }

    /**
     * Writes to the duty/length register (NR11/NR21)
     *
     * @param v register value
     */
    fun writeDutyLength(v: Int) {
        rawNrX1 = v
        dutyIndex = (v shr 6) and 3
        lengthCounter = 64 - (v and 0x3F)
    }

    /**
     * Writes to the volume envelope register (NR12/NR22). Disables the channel if the DAC is off.
     *
     * @param v register value
     */
    fun writeEnvelope(v: Int) {
        rawNrX2 = v
        envelopeInitial = (v shr 4) and 0x0F
        envelopeAdd = v and 0x08 != 0
        envelopePeriod = v and 0x07
        if (v and 0xF8 == 0) enabled = false
    }

    /**
     * Writes the lower 8 bits of the frequency (NR13/NR23)
     *
     * @param v register value
     */
    fun writeFreqLo(v: Int) {
        frequencyRaw = (frequencyRaw and 0x700) or (v and 0xFF)
    }

    /**
     * Writes the upper 3 bits of the frequency and control flags (NR14/NR24). Triggers the
     * channel when bit 7 is set.
     *
     * @param v register value
     */
    fun writeFreqHi(v: Int) {
        rawNrX4 = v
        frequencyRaw = (frequencyRaw and 0xFF) or ((v and 7) shl 8)
        lengthEnabled = v and 0x40 != 0
        if (v and 0x80 != 0) trigger()
    }

    fun readSweep(): Int = rawNrX0
    fun readDuty(): Int = rawNrX1 and 0xC0
    fun readEnvelope(): Int = rawNrX2
    fun readFreqHi(): Int = rawNrX4 and 0x40

    /**
     * Triggers the channel, reloading length, frequency timer, envelope, and sweep state
     */
    private fun trigger() {
        enabled = true
        if (lengthCounter == 0) lengthCounter = 64
        frequencyTimer = (2048 - frequencyRaw) * 4
        envelopeTimer = envelopePeriod
        volume = envelopeInitial

        if (hasSweep) {
            sweepShadow = frequencyRaw
            sweepTimer = if (sweepPeriod > 0) sweepPeriod else 8
            sweepEnabled = sweepPeriod > 0 || sweepShift > 0
            if (sweepShift > 0) calcSweep()
        }

        if (rawNrX2 and 0xF8 == 0) enabled = false
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
     * Clocks the frequency sweep, calculating a new frequency and disabling the channel on overflow
     */
    fun clockSweep() {
        if (!hasSweep) return
        if (--sweepTimer <= 0) {
            sweepTimer = if (sweepPeriod > 0) sweepPeriod else 8
            if (sweepEnabled && sweepPeriod > 0) {
                val newFreq = calcSweep()
                if (newFreq <= 2047 && sweepShift > 0) {
                    frequencyRaw = newFreq
                    sweepShadow = newFreq
                    calcSweep()
                }
            }
        }
    }

    private fun calcSweep(): Int {
        val delta = sweepShadow shr sweepShift
        val newFreq = if (sweepNegate) sweepShadow - delta else sweepShadow + delta
        if (newFreq > 2047) enabled = false
        return newFreq
    }

    /**
     * Advances the channel by [cycles] T-cycles, stepping through the duty waveform and
     * updating the [output] value
     *
     * @param cycles elapsed T-cycles
     */
    fun tick(cycles: Int) {
        if (!enabled) {
            output = 0; return
        }
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += (2048 - frequencyRaw) * 4
            dutyStep = (dutyStep + 1) and 7
        }
        output = if (dutyTable[dutyIndex][dutyStep]) volume else 0
    }

    /**
     * Resets all internal state to power-off defaults
     */
    fun reset() {
        enabled = false; output = 0
        lengthCounter = 0; lengthEnabled = false
        volume = 0; envelopeInitial = 0; envelopeAdd = false
        envelopePeriod = 0; envelopeTimer = 0
        frequencyRaw = 0; dutyIndex = 0; dutyStep = 0; frequencyTimer = 0
        sweepPeriod = 0; sweepNegate = false; sweepShift = 0
        sweepTimer = 0; sweepEnabled = false; sweepShadow = 0
        rawNrX0 = 0; rawNrX1 = 0; rawNrX2 = 0; rawNrX4 = 0
    }

    /**
     * Captures the current channel state for save state serialization
     *
     * @return snapshot of all internal channel state
     */
    fun saveState(): SquareChannelState = SquareChannelState(
        enabled, output, lengthCounter, lengthEnabled, volume,
        envelopeInitial, envelopeAdd, envelopePeriod, envelopeTimer,
        frequencyRaw, dutyIndex, dutyStep, frequencyTimer,
        sweepPeriod, sweepNegate, sweepShift, sweepTimer, sweepEnabled, sweepShadow,
        rawNrX0, rawNrX1, rawNrX2, rawNrX4,
    )

    /**
     * Restores the channel from a previously captured save state
     *
     * @param s saved channel state to restore
     */
    fun loadState(s: SquareChannelState) {
        enabled = s.enabled; output = s.output
        lengthCounter = s.lengthCounter; lengthEnabled = s.lengthEnabled
        volume = s.volume; envelopeInitial = s.envelopeInitial
        envelopeAdd = s.envelopeAdd; envelopePeriod = s.envelopePeriod
        envelopeTimer = s.envelopeTimer; frequencyRaw = s.frequencyRaw
        dutyIndex = s.dutyIndex; dutyStep = s.dutyStep; frequencyTimer = s.frequencyTimer
        sweepPeriod = s.sweepPeriod; sweepNegate = s.sweepNegate; sweepShift = s.sweepShift
        sweepTimer = s.sweepTimer; sweepEnabled = s.sweepEnabled; sweepShadow = s.sweepShadow
        rawNrX0 = s.rawNrX0; rawNrX1 = s.rawNrX1; rawNrX2 = s.rawNrX2; rawNrX4 = s.rawNrX4
    }
}
