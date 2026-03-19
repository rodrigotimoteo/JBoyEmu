package com.github.rodrigotimoteo.kboyemucore.controller

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Unit tests for [Controller]
 *
 * @author rodrigotimoteo
 */
class ControllerTest {

    private val busMock: Bus = mockk(relaxed = true)

    private lateinit var sut: Controller

    @BeforeEach
    fun setUp() {
        sut = Controller(busMock)
    }

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    fun `when no buttons pressed and action buttons selected then all bits high`() {
        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Fu.toUByte(), result)
    }

    @Test
    fun `when no buttons pressed and direction buttons selected then all bits high`() {
        val result = sut.getJoypad(0xEFu)

        assertEquals(0x0Fu.toUByte(), result)
    }

    @Test
    fun `when neither row selected then returns zero`() {
        val result = sut.getJoypad(0xFFu)

        assertEquals(0x00u.toUByte(), result)
    }

    // ── buttonPressed ────────────────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(Button::class)
    fun `when pressing a button then joypad interrupt is triggered`(button: Button) {
        sut.buttonPressed(button)

        verify(exactly = 1) { busMock.triggerInterrupt(InterruptNames.JOYPAD_INT) }
    }

    @Test
    fun `when pressing A then action bit 0 goes low`() {
        sut.buttonPressed(Button.A)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Eu.toUByte(), result)
    }

    @Test
    fun `when pressing B then action bit 1 goes low`() {
        sut.buttonPressed(Button.B)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Du.toUByte(), result)
    }

    @Test
    fun `when pressing SELECT then action bit 2 goes low`() {
        sut.buttonPressed(Button.SELECT)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Bu.toUByte(), result)
    }

    @Test
    fun `when pressing START then action bit 3 goes low`() {
        sut.buttonPressed(Button.START)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x07u.toUByte(), result)
    }

    @Test
    fun `when pressing RIGHT then direction bit 0 goes low`() {
        sut.buttonPressed(Button.RIGHT)

        val result = sut.getJoypad(0xEFu)

        assertEquals(0x0Eu.toUByte(), result)
    }

    @Test
    fun `when pressing LEFT then direction bit 1 goes low`() {
        sut.buttonPressed(Button.LEFT)

        val result = sut.getJoypad(0xEFu)

        assertEquals(0x0Du.toUByte(), result)
    }

    @Test
    fun `when pressing UP then direction bit 2 goes low`() {
        sut.buttonPressed(Button.UP)

        val result = sut.getJoypad(0xEFu)

        assertEquals(0x0Bu.toUByte(), result)
    }

    @Test
    fun `when pressing DOWN then direction bit 3 goes low`() {
        sut.buttonPressed(Button.DOWN)

        val result = sut.getJoypad(0xEFu)

        assertEquals(0x07u.toUByte(), result)
    }

    // ── buttonReleased ───────────────────────────────────────────────────────

    @Test
    fun `when pressing and releasing A then bit returns high`() {
        sut.buttonPressed(Button.A)
        sut.buttonReleased(Button.A)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Fu.toUByte(), result)
    }

    @Test
    fun `when pressing two buttons and releasing one then only released one returns high`() {
        sut.buttonPressed(Button.A)
        sut.buttonPressed(Button.B)

        sut.buttonReleased(Button.A)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Du.toUByte(), result)
    }

    // ── mixed rows ───────────────────────────────────────────────────────────

    @Test
    fun `when pressing action button then direction row is unaffected`() {
        sut.buttonPressed(Button.A)

        val result = sut.getJoypad(0xEFu)

        assertEquals(0x0Fu.toUByte(), result)
    }

    @Test
    fun `when pressing direction button then action row is unaffected`() {
        sut.buttonPressed(Button.UP)

        val result = sut.getJoypad(0xDFu)

        assertEquals(0x0Fu.toUByte(), result)
    }
}
