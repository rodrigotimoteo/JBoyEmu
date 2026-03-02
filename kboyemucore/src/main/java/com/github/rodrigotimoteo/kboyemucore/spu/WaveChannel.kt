package com.github.rodrigotimoteo.kboyemucore.spu

import com.github.rodrigotimoteo.kboyemucore.api.WaveChannelState

/**
 * Wave channel (CH3) that plays 4-bit samples from 16 bytes of Wave RAM (0xFF30-0xFF3F).
 * The 16 bytes contain 32 nibbles which are played back sequentially at a configurable frequency
 * with a volume shift applied.
 *
 * @author rodrigotimoteo
 */
class WaveChannel {

    /** Whether the channel is currently producing output */
    var enabled = false
        private set

    /** Current DAC output in the range 0-15 (0 when disabled) */
    var output = 0
        private set

    private val waveRam = IntArray(16)

    private var lengthCounter = 0
    private var lengthEnabled = false

    private var frequencyRaw = 0
    private var frequencyTimer = 0
    private var wavePosition = 0

    private var outputLevel = 0
    private var dacOn = false

    private var rawNr30 = 0
    private var rawNr32 = 0
    private var rawNr34 = 0

    /**
     * Writes to the DAC power register (NR30). Disables the channel when the DAC is turned off.
     *
     * @param v register value
     */
    fun writeDacPower(v: Int) {
        rawNr30 = v
        dacOn = v and 0x80 != 0
        if (!dacOn) enabled = false
    }

    /**
     * Writes the length load value (NR31)
     *
     * @param v register value
     */
    fun writeLengthLoad(v: Int) {
        lengthCounter = 256 - (v and 0xFF)
    }

    /**
     * Writes the output level / volume shift (NR32)
     *
     * @param v register value
     */
    fun writeOutputLevel(v: Int) {
        rawNr32 = v
        outputLevel = (v shr 5) and 3
    }

    /**
     * Writes the lower 8 bits of the frequency (NR33)
     *
     * @param v register value
     */
    fun writeFreqLo(v: Int) {
        frequencyRaw = (frequencyRaw and 0x700) or (v and 0xFF)
    }

    /**
     * Writes the upper 3 bits of the frequency and control flags (NR34). Triggers the channel
     * when bit 7 is set.
     *
     * @param v register value
     */
    fun writeFreqHi(v: Int) {
        rawNr34 = v
        frequencyRaw = (frequencyRaw and 0xFF) or ((v and 7) shl 8)
        lengthEnabled = v and 0x40 != 0
        if (v and 0x80 != 0) trigger()
    }

    /**
     * Writes a byte into Wave RAM at the given offset
     *
     * @param offset byte index within Wave RAM (0-15)
     * @param v byte value to write
     */
    fun writeWaveRam(offset: Int, v: Int) {
        waveRam[offset and 0x0F] = v and 0xFF
    }

    /**
     * Reads a byte from Wave RAM at the given offset
     *
     * @param offset byte index within Wave RAM (0-15)
     * @return byte value at the given offset
     */
    fun readWaveRam(offset: Int): Int = waveRam[offset and 0x0F]

    fun readDacPower(): Int = rawNr30 and 0x80
    fun readOutputLevel(): Int = rawNr32 and 0x60
    fun readFreqHi(): Int = rawNr34 and 0x40

    /**
     * Triggers the channel, reloading length, frequency timer, and wave position
     */
    private fun trigger() {
        if (!dacOn) return
        enabled = true
        if (lengthCounter == 0) lengthCounter = 256
        frequencyTimer = (2048 - frequencyRaw) * 2
        wavePosition = 0
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
     * Advances the channel by [cycles] T-cycles, stepping through the wave table and updating
     * the [output] value based on the current [outputLevel]
     *
     * @param cycles elapsed T-cycles
     */
    fun tick(cycles: Int) {
        if (!enabled) {
            output = 0; return
        }
        frequencyTimer -= cycles
        while (frequencyTimer <= 0) {
            frequencyTimer += (2048 - frequencyRaw) * 2
            wavePosition = (wavePosition + 1) and 0x1F
        }
        val byte = waveRam[wavePosition / 2]
        val nibble = if (wavePosition and 1 == 0) (byte shr 4) and 0x0F else byte and 0x0F
        output = when (outputLevel) {
            1 -> nibble
            2 -> nibble shr 1
            3 -> nibble shr 2
            else -> 0
        }
    }

    /**
     * Resets all internal state to power-off defaults
     */
    fun reset() {
        enabled = false; output = 0
        lengthCounter = 0; lengthEnabled = false
        frequencyRaw = 0; frequencyTimer = 0; wavePosition = 0
        outputLevel = 0; dacOn = false
        rawNr30 = 0; rawNr32 = 0; rawNr34 = 0
    }

    /**
     * Captures the current channel state for save state serialization
     *
     * @return snapshot of all internal channel state
     */
    fun saveState(): WaveChannelState = WaveChannelState(
        enabled, output, waveRam.copyOf(), lengthCounter, lengthEnabled,
        frequencyRaw, frequencyTimer, wavePosition, outputLevel, dacOn,
        rawNr30, rawNr32, rawNr34,
    )

    /**
     * Restores the channel from a previously captured save state
     *
     * @param s saved channel state to restore
     */
    fun loadState(s: WaveChannelState) {
        enabled = s.enabled; output = s.output
        s.waveRam.copyInto(waveRam)
        lengthCounter = s.lengthCounter; lengthEnabled = s.lengthEnabled
        frequencyRaw = s.frequencyRaw; frequencyTimer = s.frequencyTimer
        wavePosition = s.wavePosition; outputLevel = s.outputLevel; dacOn = s.dacOn
        rawNr30 = s.rawNr30; rawNr32 = s.rawNr32; rawNr34 = s.rawNr34
    }
}
