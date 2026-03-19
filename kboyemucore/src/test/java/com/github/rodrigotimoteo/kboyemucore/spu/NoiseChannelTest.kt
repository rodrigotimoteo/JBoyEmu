package com.github.rodrigotimoteo.kboyemucore.spu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [NoiseChannel]
 *
 * @author rodrigotimoteo
 */
class NoiseChannelTest {

    private lateinit var sut: NoiseChannel

    @BeforeEach
    fun setUp() {
        sut = NoiseChannel()
    }

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    fun `when created then channel is disabled`() {
        assertFalse(sut.enabled)
        assertEquals(0, sut.output)
    }

    // ── trigger ──────────────────────────────────────────────────────────────

    @Test
    fun `when triggered with valid envelope then channel becomes enabled`() {
        sut.writeEnvelope(0xF0)
        sut.writeControl(0x80)

        assertTrue(sut.enabled)
    }

    @Test
    fun `when triggered with zero DAC then channel stays disabled`() {
        sut.writeEnvelope(0x00)
        sut.writeControl(0x80)

        assertFalse(sut.enabled)
    }

    @Test
    fun `when triggered then length counter reloads if zero`() {
        sut.writeEnvelope(0xF0)
        sut.writeControl(0x80)

        assertTrue(sut.enabled)

        for (i in 0 until 64) sut.clockLength()

        assertTrue(sut.enabled)
    }

    // ── register read-back ───────────────────────────────────────────────────

    @Test
    fun `when writing envelope then readback matches`() {
        sut.writeEnvelope(0xA5)

        assertEquals(0xA5, sut.readEnvelope())
    }

    @Test
    fun `when writing polynomial then readback matches`() {
        sut.writePolynomial(0x73)

        assertEquals(0x73, sut.readPolynomial())
    }

    @Test
    fun `when writing control then readback only returns bit 6`() {
        sut.writeEnvelope(0xF0)
        sut.writeControl(0xC0)

        assertEquals(0x40, sut.readControl())
    }

    // ── length counter ───────────────────────────────────────────────────────

    @Test
    fun `when length enabled and counter reaches zero then channel disables`() {
        sut.writeLengthLoad(0x3E)
        sut.writeEnvelope(0xF0)
        sut.writeControl(0xC0)

        assertTrue(sut.enabled)

        for (i in 0 until 2) sut.clockLength()

        assertFalse(sut.enabled)
    }

    @Test
    fun `when length not enabled then clocking length does nothing`() {
        sut.writeLengthLoad(0x3F)
        sut.writeEnvelope(0xF0)
        sut.writeControl(0x80)

        assertTrue(sut.enabled)

        for (i in 0 until 100) sut.clockLength()

        assertTrue(sut.enabled)
    }

    // ── envelope ─────────────────────────────────────────────────────────────

    @Test
    fun `when envelope period is zero then clocking does nothing`() {
        sut.writeEnvelope(0xF0)
        sut.writeControl(0x80)

        sut.clockEnvelope()

        assertTrue(sut.enabled)
    }

    @Test
    fun `when envelope add is set then volume increases`() {
        sut.writeEnvelope(0x08)
        sut.writeControl(0x80)

        sut.clockEnvelope()

        assertTrue(sut.enabled)
    }

    // ── tick ──────────────────────────────────────────────────────────────────

    @Test
    fun `when ticking disabled channel then output is zero`() {
        sut.tick(100)

        assertEquals(0, sut.output)
    }

    @Test
    fun `when ticking enabled channel then output is within valid range`() {
        sut.writeEnvelope(0xF0)
        sut.writePolynomial(0x00)
        sut.writeControl(0x80)

        sut.tick(16)

        assertTrue(sut.output in 0..15)
    }

    @Test
    fun `when using 7-bit width mode then channel still produces output`() {
        sut.writeEnvelope(0xF0)
        sut.writePolynomial(0x08)
        sut.writeControl(0x80)

        sut.tick(64)

        assertTrue(sut.output in 0..15)
    }

    // ── reset ────────────────────────────────────────────────────────────────

    @Test
    fun `when reset then channel returns to initial state`() {
        sut.writeEnvelope(0xF0)
        sut.writeControl(0x80)
        assertTrue(sut.enabled)

        sut.reset()

        assertFalse(sut.enabled)
        assertEquals(0, sut.output)
    }

    // ── save and restore ─────────────────────────────────────────────────────

    @Nested
    inner class SaveAndRestore {

        @Test
        fun `when saving and loading state then channel is identical`() {
            val original = NoiseChannel()
            original.writeEnvelope(0xF2)
            original.writePolynomial(0x37)
            original.writeControl(0xC0)
            original.tick(32)

            val state = original.saveState()

            val restored = NoiseChannel()
            restored.loadState(state)

            assertEquals(original.enabled, restored.enabled)
            assertEquals(original.output, restored.output)
        }
    }
}

