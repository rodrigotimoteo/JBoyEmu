package com.github.rodrigotimoteo.kboyemucore.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Complete snapshot of the emulator state that can be serialized to and from a byte array.
 * Used for save states (quick save / quick load) so the user can resume exactly where they left off.
 *
 * All internal emulator state is captured here: CPU registers, memory banks, PPU counters,
 * SPU channel state, interrupt flags, and CGB-specific data such as palette RAM and HDMA state.
 *
 * Serialized using Protocol Buffers via kotlinx.serialization for compact binary output.
 *
 * @author rodrigotimoteo
 */
@Serializable
data class SaveState(
    val cpu: CpuState,
    val timers: TimerState,
    val interrupts: InterruptState,
    val memory: MemoryState,
    val ppu: PpuState,
    val spu: SpuState,
) {
    companion object {
        /**
         * Serializes the save state into a compact binary byte array
         *
         * @return binary representation of this save state
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun SaveState.toByteArray(): ByteArray = ProtoBuf.encodeToByteArray(serializer(), this)

        /**
         * Deserializes a save state from a binary byte array
         *
         * @param bytes binary data produced by [toByteArray]
         * @return restored save state
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromByteArray(bytes: ByteArray): SaveState = ProtoBuf.decodeFromByteArray(serializer(), bytes)
    }
}

/**
 * Snapshot of CPU register values and execution flags
 */
@Serializable
data class CpuState(
    val a: Int,
    val f: Int,
    val b: Int,
    val c: Int,
    val d: Int,
    val e: Int,
    val h: Int,
    val l: Int,
    val programCounter: Int,
    val stackPointer: Int,
    val halted: Boolean,
    val stopped: Boolean,
)

/**
 * Snapshot of CPU timer counters and configuration
 */
@Serializable
data class TimerState(
    val machineCycles: Int,
    val interruptChangedCounter: Int,
    val timerClockCounter: Int,
    val dividerClockTimer: Int,
    val totalDividerTimer: Int,
    val timerFrequency: Int,
    val timerEnabled: Boolean,
    val handleOverflow: Boolean,
)

/**
 * Snapshot of interrupt controller state
 */
@Serializable
data class InterruptState(
    val interruptMasterEnabled: Boolean,
    val haltBug: Boolean,
    val interruptChange: Boolean,
    val changeToState: Boolean,
)

/**
 * Snapshot of all memory modules including bank selection state
 */
@Serializable
data class MemoryState(
    val vram: List<ByteArray>,
    val wram: List<ByteArray>,
    val oam: ByteArray,
    val eram: List<ByteArray>?,
    val bottomRegisters: ByteArray,
    val vramBank: Int,
    val wramBank: Int,
    val romBank: Int,
    val eramBank: Int,
    val ramEnabled: Boolean,
    val hdmaActive: Boolean,
    val hdmaSource: Int,
    val hdmaDest: Int,
    val hdmaRemaining: Int,
    val bgPaletteIndex: Int,
    val bgPaletteAutoInc: Boolean,
    val objPaletteIndex: Int,
    val objPaletteAutoInc: Boolean,
)

/**
 * Snapshot of PPU state including mode, counters, and drawer-specific state
 */
@Serializable
data class PpuState(
    val mode: Int,
    val counter: Int,
    val currentLine: Int,
    val currentLineWindow: Int,
    val drawer: PpuDrawerState,
)

/**
 * Snapshot of PPU drawer state. On DMG this is empty; on CGB it contains the BG and OBJ
 * palette RAM. Using a single class with nullable fields keeps the serialization schema simple.
 */
@Serializable
data class PpuDrawerState(
    val bgPaletteRam: IntArray? = null,
    val objPaletteRam: IntArray? = null,
)

/**
 * Snapshot of a single square wave channel
 */
@Serializable
data class SquareChannelState(
    val enabled: Boolean,
    val output: Int,
    val lengthCounter: Int,
    val lengthEnabled: Boolean,
    val volume: Int,
    val envelopeInitial: Int,
    val envelopeAdd: Boolean,
    val envelopePeriod: Int,
    val envelopeTimer: Int,
    val frequencyRaw: Int,
    val dutyIndex: Int,
    val dutyStep: Int,
    val frequencyTimer: Int,
    val sweepPeriod: Int,
    val sweepNegate: Boolean,
    val sweepShift: Int,
    val sweepTimer: Int,
    val sweepEnabled: Boolean,
    val sweepShadow: Int,
    val rawNrX0: Int,
    val rawNrX1: Int,
    val rawNrX2: Int,
    val rawNrX4: Int,
)

/**
 * Snapshot of the wave channel
 */
@Serializable
data class WaveChannelState(
    val enabled: Boolean,
    val output: Int,
    val waveRam: IntArray,
    val lengthCounter: Int,
    val lengthEnabled: Boolean,
    val frequencyRaw: Int,
    val frequencyTimer: Int,
    val wavePosition: Int,
    val outputLevel: Int,
    val dacOn: Boolean,
    val rawNr30: Int,
    val rawNr32: Int,
    val rawNr34: Int,
)

/**
 * Snapshot of the noise channel
 */
@Serializable
data class NoiseChannelState(
    val enabled: Boolean,
    val output: Int,
    val lengthCounter: Int,
    val lengthEnabled: Boolean,
    val volume: Int,
    val envelopeInitial: Int,
    val envelopeAdd: Boolean,
    val envelopePeriod: Int,
    val envelopeTimer: Int,
    val lfsr: Int,
    val width7: Boolean,
    val clockShift: Int,
    val divisorCode: Int,
    val frequencyTimer: Int,
    val rawNr42: Int,
    val rawNr43: Int,
    val rawNr44: Int,
)

/**
 * Snapshot of the complete SPU state
 */
@Serializable
data class SpuState(
    val masterOn: Boolean,
    val nr50: Int,
    val nr51: Int,
    val frameSeqTimer: Int,
    val frameSeqStep: Int,
    val sampleAccumulator: Int,
    val ch1: SquareChannelState,
    val ch2: SquareChannelState,
    val ch3: WaveChannelState,
    val ch4: NoiseChannelState,
)

