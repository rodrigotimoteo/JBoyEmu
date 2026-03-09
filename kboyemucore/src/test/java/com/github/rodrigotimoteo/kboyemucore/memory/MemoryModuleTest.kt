package com.github.rodrigotimoteo.kboyemucore.memory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MemoryModule]
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
class MemoryModuleTest {

    @Nested
    inner class SingleBankModule {

        private val sut = MemoryModule(size = 0x100, memoryOffset = 0x0)

        @Test
        fun `when created then all values are zero`() {
            for (addr in 0 until 0x100) {
                assertEquals(0x00u.toUByte(), sut.getValue(addr))
            }
        }

        @Test
        fun `when setting a value then it can be read back`() {
            sut.setValue(0x42, 0xABu)

            assertEquals(0xABu.toUByte(), sut.getValue(0x42))
        }

        @Test
        fun `when setting a value then other addresses are unaffected`() {
            sut.setValue(0x42, 0xABu)

            assertEquals(0x00u.toUByte(), sut.getValue(0x43))
        }

        @Test
        fun `when using memory offset then addresses are correctly translated`() {
            val module = MemoryModule(size = 0x100, memoryOffset = 0x8000)

            module.setValue(0x8000, 0xFFu)

            assertEquals(0xFFu.toUByte(), module.getValue(0x8000))
        }
    }

    @Nested
    inner class MultiBankSingleSimultaneous {

        @Test
        fun `when switching banks then different data is accessible`() {
            val sut = MemoryModule(
                size = 0x100,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 4,
            )

            sut.activeBank = 0
            sut.setValue(0x00, 0x11u)

            sut.activeBank = 1
            sut.setValue(0x00, 0x22u)

            sut.activeBank = 0
            assertEquals(0x11u.toUByte(), sut.getValue(0x00))

            sut.activeBank = 1
            assertEquals(0x22u.toUByte(), sut.getValue(0x00))
        }
    }

    @Nested
    inner class MultiBankDualSimultaneous {

        @Test
        fun `when using dual simultaneous then low region reads from bank zero`() {
            val sut = MemoryModule(
                size = 0x100,
                simultaneousBanks = 2,
                memoryOffset = 0x0,
                numberOfBanks = 4,
            )

            sut.setValue(0x00, 0xAAu)

            assertEquals(0xAAu.toUByte(), sut.getValue(0x00))
        }

        @Test
        fun `when using dual simultaneous then high region reads from active bank`() {
            val sut = MemoryModule(
                size = 0x100,
                simultaneousBanks = 2,
                memoryOffset = 0x0,
                numberOfBanks = 4,
            )

            sut.activeBank = 2
            sut.setValue(0x100, 0xBBu)

            assertEquals(0xBBu.toUByte(), sut.getValue(0x100))
        }
    }

    @Nested
    inner class InitializedContent {

        @Test
        fun `when created with content then memory is correctly populated`() {
            val content = UByteArray(0x200) { (it and 0xFF).toUByte() }

            val sut = MemoryModule(
                content = content,
                size = 0x100,
                simultaneousBanks = 2,
                memoryOffset = 0x0,
                numberOfBanks = 2,
            )

            assertEquals(0x00u.toUByte(), sut.getValue(0x00))
            assertEquals(0x42u.toUByte(), sut.getValue(0x42))
        }
    }

    @Nested
    inner class GetValueFromBank {

        @Test
        fun `when reading from specific bank then correct data is returned`() {
            val sut = MemoryModule(
                size = 0x100,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 2,
            )

            sut.activeBank = 0
            sut.setValue(0x00, 0xAAu)

            sut.activeBank = 1
            sut.setValue(0x00, 0xBBu)

            assertEquals(0xAAu.toUByte(), sut.getValueFromBank(0x00, 0))
            assertEquals(0xBBu.toUByte(), sut.getValueFromBank(0x00, 1))
        }

        @Test
        fun `when bank index out of range then it is clamped`() {
            val sut = MemoryModule(
                size = 0x100,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 2,
            )

            sut.activeBank = 1
            sut.setValue(0x00, 0xCCu)

            assertEquals(0xCCu.toUByte(), sut.getValueFromBank(0x00, 99))
        }
    }

    @Nested
    inner class SnapshotAndRestore {

        @Test
        fun `when snapshotting then banks are correctly captured`() {
            val sut = MemoryModule(
                size = 0x10,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 2,
            )

            sut.activeBank = 0
            sut.setValue(0x00, 0xAAu)

            sut.activeBank = 1
            sut.setValue(0x00, 0xBBu)

            val snapshot = sut.snapshotBanks()

            assertEquals(2, snapshot.size)
            assertEquals(0xAA.toByte(), snapshot[0][0])
            assertEquals(0xBB.toByte(), snapshot[1][0])
        }

        @Test
        fun `when restoring from snapshot then data matches`() {
            val sut = MemoryModule(
                size = 0x10,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 2,
            )

            val banks = listOf(
                ByteArray(0x10) { 0x11 },
                ByteArray(0x10) { 0x22 },
            )
            sut.restoreBanks(banks)

            sut.activeBank = 0
            assertEquals(0x11u.toUByte(), sut.getValue(0x00))

            sut.activeBank = 1
            assertEquals(0x22u.toUByte(), sut.getValue(0x00))
        }

        @Test
        fun `when snapshot is mutated then original module is not affected`() {
            val sut = MemoryModule(
                size = 0x10,
                simultaneousBanks = 1,
                memoryOffset = 0x0,
                numberOfBanks = 1,
            )

            sut.setValue(0x00, 0xFFu)
            val snapshot = sut.snapshotBanks()

            snapshot[0][0] = 0x00

            assertEquals(0xFFu.toUByte(), sut.getValue(0x00))
        }
    }
}

