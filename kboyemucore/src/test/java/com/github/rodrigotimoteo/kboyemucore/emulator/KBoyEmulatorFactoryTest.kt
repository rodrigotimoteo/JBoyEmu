package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test class for [KBoyEmulatorFactory]
 *
 * @author rodrigotimoteo
 */
class KBoyEmulatorFactoryTest {

    /** Logger mock for testing */
    private val logger: Logger = mockk(relaxed = true)

    @Test
    fun `when invoking factory then it returns a KBoyEmulator implementation`() {
        val emulator: KBoyEmulator = KBoyEmulatorFactory(logger)

        assertTrue(emulator is KBoyEmulatorImpl)
    }

    @Test
    fun `when invoking factory twice then it returns distinct instances`() {
        val first = KBoyEmulatorFactory(logger)
        val second = KBoyEmulatorFactory(logger)

        assertNotSame(first, second)
    }
}
