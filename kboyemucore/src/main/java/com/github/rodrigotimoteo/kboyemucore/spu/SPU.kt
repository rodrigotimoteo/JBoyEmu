package com.github.rodrigotimoteo.kboyemucore.spu

import com.github.rodrigotimoteo.kboyemucore.api.SpuState
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.util.Logger

/**
 * Game Boy APU (Sound Processing Unit) that mixes four sound channels into stereo PCM and writes
 * them to an [AudioRingBuffer]. The emulation loop calls [tick] with T-cycle counts while register
 * access is routed through [writeRegister] and [readRegister] from the MemoryManager.
 *
 * @author rodrigotimoteo
 */
class SPU(
    private val logger: Logger,
) {

    /**
     * Ring buffer shared between the emulation thread (producer) and the audio thread (consumer)
     */
    val ringBuffer = AudioRingBuffer()

    /** Channel 1 — square wave with frequency sweep */
    private val ch1 = SquareChannel(hasSweep = true)

    /** Channel 2 — square wave without sweep */
    private val ch2 = SquareChannel(hasSweep = false)

    /** Channel 3 — wave playback from Wave RAM */
    private val ch3 = WaveChannel()

    /** Channel 4 — LFSR-based noise */
    private val ch4 = NoiseChannel()

    /** NR52 bit 7 — global sound on/off */
    private var masterOn = true

    /** NR50 — master volume for left (bits 6-4) and right (bits 2-0) */
    private var nr50 = 0x77

    /** NR51 — channel panning configuration */
    private var nr51 = 0xF3

    /** Frame sequencer countdown timer */
    private var frameSeqTimer = FRAME_SEQ_PERIOD

    /** Current step of the frame sequencer (0-7) */
    private var frameSeqStep = 0

    /** Fractional accumulator for down-sampling to [SAMPLE_RATE] Hz */
    private var sampleAccumulator = 0

    /** Debug sample counter used to log stats once per second */
    private var dbgSampleCount = 0
    private var dbgMinL = Short.MAX_VALUE.toInt()
    private var dbgMaxL = Short.MIN_VALUE.toInt()
    private var dbgMinR = Short.MAX_VALUE.toInt()
    private var dbgMaxR = Short.MIN_VALUE.toInt()
    private var dbgDrops = 0

    companion object {
        const val SAMPLE_RATE = 44_100
        private const val CPU_FREQ = 4_194_304
        private const val FRAME_SEQ_PERIOD = 8_192
    }

    /**
     * Advances the SPU by [tCycles] T-cycles. Ticks all channels, steps the frame sequencer,
     * and generates output samples at [SAMPLE_RATE] Hz via fractional accumulation.
     *
     * @param tCycles elapsed T-cycles since last call
     */
    fun tick(tCycles: Int) {
        if (!masterOn) return

        ch1.tick(tCycles); ch2.tick(tCycles); ch3.tick(tCycles); ch4.tick(tCycles)

        frameSeqTimer -= tCycles
        while (frameSeqTimer <= 0) {
            frameSeqTimer += FRAME_SEQ_PERIOD
            stepFrameSequencer()
        }

        sampleAccumulator += tCycles * SAMPLE_RATE
        while (sampleAccumulator >= CPU_FREQ) {
            sampleAccumulator -= CPU_FREQ
            mixAndPush()
        }
    }

    /**
     * Writes a value to an APU register. When the APU is powered off only NR52 accepts writes.
     *
     * @param address memory address of the register (0xFF10-0xFF3F)
     * @param value byte value to write
     */
    fun writeRegister(address: Int, value: Int) {
        if (!masterOn && address != ReservedAddresses.NR52.memoryAddress) return

        when (address) {
            ReservedAddresses.NR10.memoryAddress -> ch1.writeSweep(value)
            ReservedAddresses.NR11.memoryAddress -> ch1.writeDutyLength(value)
            ReservedAddresses.NR12.memoryAddress -> ch1.writeEnvelope(value)
            ReservedAddresses.NR13.memoryAddress -> ch1.writeFreqLo(value)
            ReservedAddresses.NR14.memoryAddress -> ch1.writeFreqHi(value)
            ReservedAddresses.NR21.memoryAddress -> ch2.writeDutyLength(value)
            ReservedAddresses.NR22.memoryAddress -> ch2.writeEnvelope(value)
            ReservedAddresses.NR23.memoryAddress -> ch2.writeFreqLo(value)
            ReservedAddresses.NR24.memoryAddress -> ch2.writeFreqHi(value)
            ReservedAddresses.NR30.memoryAddress -> ch3.writeDacPower(value)
            ReservedAddresses.NR31.memoryAddress -> ch3.writeLengthLoad(value)
            ReservedAddresses.NR32.memoryAddress -> ch3.writeOutputLevel(value)
            ReservedAddresses.NR33.memoryAddress -> ch3.writeFreqLo(value)
            ReservedAddresses.NR34.memoryAddress -> ch3.writeFreqHi(value)
            ReservedAddresses.NR41.memoryAddress -> ch4.writeLengthLoad(value)
            ReservedAddresses.NR42.memoryAddress -> ch4.writeEnvelope(value)
            ReservedAddresses.NR43.memoryAddress -> ch4.writePolynomial(value)
            ReservedAddresses.NR44.memoryAddress -> ch4.writeControl(value)
            ReservedAddresses.NR50.memoryAddress -> nr50 = value
            ReservedAddresses.NR51.memoryAddress -> nr51 = value
            ReservedAddresses.NR52.memoryAddress -> {
                val wasOn = masterOn
                masterOn = value and 0x80 != 0
                if (wasOn && !masterOn) powerOff()
            }

            in ReservedAddresses.WAVE_START.memoryAddress..ReservedAddresses.WAVE_END.memoryAddress ->
                ch3.writeWaveRam(address - ReservedAddresses.WAVE_START.memoryAddress, value)
        }
    }

    /**
     * Reads the current value of an APU register, applying the appropriate read masks for
     * write-only bits.
     *
     * @param address memory address of the register (0xFF10-0xFF3F)
     * @return current register value with unused bits set high
     */
    fun readRegister(address: Int): Int = when (address) {
        ReservedAddresses.NR10.memoryAddress -> ch1.readSweep() or 0x80
        ReservedAddresses.NR11.memoryAddress -> ch1.readDuty() or 0x3F
        ReservedAddresses.NR12.memoryAddress -> ch1.readEnvelope()
        ReservedAddresses.NR13.memoryAddress -> 0xFF
        ReservedAddresses.NR14.memoryAddress -> ch1.readFreqHi() or 0xBF
        ReservedAddresses.NR21.memoryAddress -> ch2.readDuty() or 0x3F
        ReservedAddresses.NR22.memoryAddress -> ch2.readEnvelope()
        ReservedAddresses.NR23.memoryAddress -> 0xFF
        ReservedAddresses.NR24.memoryAddress -> ch2.readFreqHi() or 0xBF
        ReservedAddresses.NR30.memoryAddress -> ch3.readDacPower() or 0x7F
        ReservedAddresses.NR31.memoryAddress -> 0xFF
        ReservedAddresses.NR32.memoryAddress -> ch3.readOutputLevel() or 0x9F
        ReservedAddresses.NR33.memoryAddress -> 0xFF
        ReservedAddresses.NR34.memoryAddress -> ch3.readFreqHi() or 0xBF
        ReservedAddresses.NR41.memoryAddress -> 0xFF
        ReservedAddresses.NR42.memoryAddress -> ch4.readEnvelope()
        ReservedAddresses.NR43.memoryAddress -> ch4.readPolynomial()
        ReservedAddresses.NR44.memoryAddress -> ch4.readControl() or 0xBF
        ReservedAddresses.NR50.memoryAddress -> nr50
        ReservedAddresses.NR51.memoryAddress -> nr51
        ReservedAddresses.NR52.memoryAddress -> {
            var v = 0x70
            if (masterOn) v = v or 0x80
            if (ch1.enabled) v = v or 0x01
            if (ch2.enabled) v = v or 0x02
            if (ch3.enabled) v = v or 0x04
            if (ch4.enabled) v = v or 0x08
            v
        }

        in ReservedAddresses.WAVE_START.memoryAddress..ReservedAddresses.WAVE_END.memoryAddress ->
            ch3.readWaveRam(address - ReservedAddresses.WAVE_START.memoryAddress)

        else -> 0xFF
    }

    private fun stepFrameSequencer() {
        when (frameSeqStep) {
            0, 4 -> clockLength()
            2, 6 -> {
                clockLength(); ch1.clockSweep()
            }

            7 -> clockEnvelope()
        }
        frameSeqStep = (frameSeqStep + 1) and 7
    }

    private fun clockLength() {
        ch1.clockLength(); ch2.clockLength(); ch3.clockLength(); ch4.clockLength()
    }

    private fun clockEnvelope() {
        ch1.clockEnvelope(); ch2.clockEnvelope(); ch4.clockEnvelope()
    }

    /**
     * Mixes the four channels, applies master volume and panning, then pushes one stereo sample
     * pair into the [ringBuffer]. Channel outputs are converted from unsigned 0-15 to signed
     * -15..+15 to match the hardware DAC behavior. Logs debug stats once per second.
     */
    private fun mixAndPush() {
        val lVol = ((nr50 shr 4) and 7) + 1
        val rVol = (nr50 and 7) + 1

        val c1 = ch1.output * 2 - 15
        val c2 = ch2.output * 2 - 15
        val c3 = ch3.output * 2 - 15
        val c4 = ch4.output * 2 - 15

        var lMix = 0;
        var rMix = 0
        if (nr51 and 0x10 != 0) lMix += c1
        if (nr51 and 0x20 != 0) lMix += c2
        if (nr51 and 0x40 != 0) lMix += c3
        if (nr51 and 0x80 != 0) lMix += c4
        if (nr51 and 0x01 != 0) rMix += c1
        if (nr51 and 0x02 != 0) rMix += c2
        if (nr51 and 0x04 != 0) rMix += c3
        if (nr51 and 0x08 != 0) rMix += c4

        val lSample = lMix * lVol * 48
        val rSample = rMix * rVol * 48

        if (lSample < dbgMinL) dbgMinL = lSample
        if (lSample > dbgMaxL) dbgMaxL = lSample
        if (rSample < dbgMinR) dbgMinR = rSample
        if (rSample > dbgMaxR) dbgMaxR = rSample
        if (!ringBuffer.writeSample(lSample.toShort(), rSample.toShort())) dbgDrops++

        if (++dbgSampleCount >= SAMPLE_RATE) {
            logger.d(
                "SPU: L=[$dbgMinL..$dbgMaxL] R=[$dbgMinR..$dbgMaxR] " +
                        "ch1=[${ch1.enabled}] ch2=[${ch2.enabled}] " +
                        "ch3=[${ch3.enabled}] ch4=[${ch4.enabled}] " +
                        "nr50=$nr50 nr51=$nr51 drops=$dbgDrops"
            )
            dbgSampleCount = 0
            dbgMinL = Short.MAX_VALUE.toInt(); dbgMaxL = Short.MIN_VALUE.toInt()
            dbgMinR = Short.MAX_VALUE.toInt(); dbgMaxR = Short.MIN_VALUE.toInt()
            dbgDrops = 0
        }
    }

    /**
     * Resets all channels and master control registers when NR52 bit 7 transitions from 1 to 0
     */
    private fun powerOff() {
        ch1.reset(); ch2.reset(); ch3.reset(); ch4.reset()
        nr50 = 0; nr51 = 0
    }

    /**
     * Captures the complete SPU state for save state serialization
     *
     * @return snapshot of all SPU and channel state
     */
    fun saveState(): SpuState = SpuState(
        masterOn, nr50, nr51, frameSeqTimer, frameSeqStep, sampleAccumulator,
        ch1.saveState(), ch2.saveState(), ch3.saveState(), ch4.saveState(),
    )

    /**
     * Restores the SPU from a previously captured save state
     *
     * @param s saved SPU state to restore
     */
    fun loadState(s: SpuState) {
        masterOn = s.masterOn; nr50 = s.nr50; nr51 = s.nr51
        frameSeqTimer = s.frameSeqTimer; frameSeqStep = s.frameSeqStep
        sampleAccumulator = s.sampleAccumulator
        ch1.loadState(s.ch1); ch2.loadState(s.ch2)
        ch3.loadState(s.ch3); ch4.loadState(s.ch4)
    }
}
