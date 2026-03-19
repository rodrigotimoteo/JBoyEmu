package com.github.rodrigotimoteo.kboyemucore.spu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SquareChannel]
 *
 * @author rodrigotimoteo
 */
class SquareChannelTest {

    @Nested
    inner class WithoutSweep {

        private lateinit var sut: SquareChannel

        @BeforeEach
        fun setUp() {
            sut = SquareChannel(hasSweep = false)
        }

        @Test
        fun `when created then channel is disabled`() {
            assertFalse(sut.enabled)
            assertEquals(0, sut.output)
        }

        @Test
        fun `when triggered with valid envelope then channel becomes enabled`() {
            sut.writeEnvelope(0xF0)
            sut.writeFreqHi(0x80)

            assertTrue(sut.enabled)
        }

        @Test
        fun `when triggered with zero envelope then channel stays disabled`() {
            sut.writeEnvelope(0x00)
            sut.writeFreqHi(0x80)

            assertFalse(sut.enabled)
        }

        @Test
        fun `when writing sweep then nothing happens without sweep support`() {
            sut.writeSweep(0x77)

            assertEquals(0, sut.readSweep())
        }

        @Test
        fun `when clocking sweep then nothing happens without sweep support`() {
            sut.writeEnvelope(0xF0)
            sut.writeFreqHi(0x80)

            sut.clockSweep()

            assertTrue(sut.enabled)
        }

        @Test
        fun `when writing duty and length then duty is readable`() {
            sut.writeDutyLength(0x80)

            assertEquals(0x80, sut.readDuty())
        }

        @Test
        fun `when writing envelope then envelope is readable`() {
            sut.writeEnvelope(0xF3)

            assertEquals(0xF3, sut.readEnvelope())
        }

        @Test
        fun `when clocking length with length enabled then length decreases`() {
            sut.writeEnvelope(0xF0)
            sut.writeDutyLength(0x3E)
            sut.writeFreqHi(0xC0)

            assertTrue(sut.enabled)

            for (i in 0 until 2) sut.clockLength()

            assertFalse(sut.enabled)
        }

        @Test
        fun `when clocking envelope with add direction then volume increases`() {
            sut.writeEnvelope(0x08)
            sut.writeFreqHi(0x80)

            assertTrue(sut.enabled)

            sut.clockEnvelope()

            sut.tick(4)
            assertTrue(sut.output >= 0)
        }

        @Test
        fun `when ticking disabled channel then output is zero`() {
            sut.tick(100)

            assertEquals(0, sut.output)
        }

        @Test
        fun `when reset then channel returns to initial state`() {
            sut.writeEnvelope(0xF0)
            sut.writeFreqHi(0x80)
            assertTrue(sut.enabled)

            sut.reset()

            assertFalse(sut.enabled)
            assertEquals(0, sut.output)
        }
    }

    @Nested
    inner class WithSweep {

        private lateinit var sut: SquareChannel

        @BeforeEach
        fun setUp() {
            sut = SquareChannel(hasSweep = true)
        }

        @Test
        fun `when writing sweep then sweep is readable`() {
            sut.writeSweep(0x73)

            assertEquals(0x73, sut.readSweep())
        }

        @Test
        fun `when sweep overflows frequency then channel is disabled`() {
            sut.writeSweep(0x11)
            sut.writeEnvelope(0xF0)
            sut.writeFreqLo(0xFF)
            sut.writeFreqHi(0x87)

            for (i in 0 until 20) sut.clockSweep()

            assertFalse(sut.enabled)
        }

        @Test
        fun `when sweep is set to negate then frequency decreases`() {
            sut.writeSweep(0x19)
            sut.writeEnvelope(0xF0)
            sut.writeFreqLo(0x00)
            sut.writeFreqHi(0x84)

            for (i in 0 until 10) sut.clockSweep()

            assertTrue(sut.enabled)
        }
    }

    @Nested
    inner class SaveAndRestore {

        @Test
        fun `when saving and loading state then channel is identical`() {
            val original = SquareChannel(hasSweep = true)
            original.writeSweep(0x37)
            original.writeEnvelope(0xF2)
            original.writeDutyLength(0xC0)
            original.writeFreqLo(0x80)
            original.writeFreqHi(0xC3)
            original.tick(16)

            val state = original.saveState()

            val restored = SquareChannel(hasSweep = true)
            restored.loadState(state)

            assertEquals(original.enabled, restored.enabled)
            assertEquals(original.output, restored.output)
        }
    }
}

