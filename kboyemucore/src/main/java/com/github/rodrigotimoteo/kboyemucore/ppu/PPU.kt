package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("TooManyFunctions")
class PPU(
    private val bus: Bus,
) {

    /** [MutableStateFlow] of [FrameBuffer] used to update the screen when an update is triggered */
    private val _painting = MutableStateFlow(
        FrameBuffer(WIDTH, HEIGHT, ByteArray(WIDTH * HEIGHT))
    )

    /** [StateFlow] of [FrameBuffer] for use in Emulator implementation */
    val painting = _painting.asStateFlow()

    /** Variable used to control whether the PPU should go to sleep (when in VBlank and nothing changed) */
    private var getToSleep = false

    /** Variable used to control whether the PPU is in CGB mode or not (affects some drawing operations) */
    private val cgb = bus.isCGB

    /**
     * Reference to [PPUDrawer]
     */
    internal val ppuDrawer = PPUDrawer(this, bus)

    /**
     * Reference to [PPURegisters]
     */
    internal val ppuRegisters = PPURegisters(bus)

    /** Variables used to calculate and print the FPS of the PPU */
    private var frameCount = 0

    /** Variable used to store the last timestamp when the FPS was calculated */
    private var lastTimestampMs = System.currentTimeMillis()

    /**
     * Returns the window map address based on the current PPURegisters configuration
     *
     * @return windowMapAddress
     */
    val windowMapAddress: Int
        get() = if (ppuRegisters.windowTileMap) {
            ReservedAddresses.TILE_MAP_1.memoryAddress
        } else {
            ReservedAddresses.TILE_MAP_0.memoryAddress
        }

    /**
     * Returns the background map address based on the current PPURegisters configuration
     *
     * @return backgroundMapAddress
     */
    val backgroundMapAddress: Int
        get() = if (ppuRegisters.backgroundTileMap) {
            ReservedAddresses.TILE_MAP_1.memoryAddress
        } else {
            ReservedAddresses.TILE_MAP_0.memoryAddress
        }

    /**
     * Returns the tile data address based on the current PPURegisters configuration
     *
     * @return tileDataAddress
     */
    val tileDataAddress: Int
        get() = if (ppuRegisters.tileData) {
            ReservedAddresses.SWITCH_ROM_END.memoryAddress
        } else {
            ReservedAddresses.TILE_DATA_2.memoryAddress
        }

    /**
     * Updates the painting flow providing a new frame to be rendered
     *
     * @param painting content to be rendered (Color coded)
     */
    internal fun propagatePaintingUpdate(painting: ByteArray) {
        _painting.value = FrameBuffer(pixels = painting.copyOf())

        frameCount++
        val nowMs = System.currentTimeMillis()
        val elapsedMs = nowMs - lastTimestampMs
        if (elapsedMs >= 1000) {
            val fps = (frameCount * 1000.0) / elapsedMs
            println("PPU FPS: %.1f".format(fps))
            frameCount = 0
            lastTimestampMs = nowMs
        }
    }

    /**
     * Returns whether or not the LCD is On
     *
     * @return lcdOn
     */
    fun isLCDOn(): Boolean = ppuRegisters.lcdOn

    /**
     * Method used when ppu is offline (lcd is not On) to check if anything changed (instead of
     * ticking the PPU)
     */
    fun checkLCDStatus() {
        ppuRegisters.readLCDControl()
    }

    fun tick() {
        ppuRegisters.readLCDControl()
        ppuRegisters.readLCDStatus()
        draw()
    }

    private fun changeMode(mode: PPUModes) {
        var statRegister: Int = ppuRegisters.statRegister.value.toInt() and 0xFC
        var requestInterrupt = false

        when (mode) {
            PPUModes.HBLANK -> {
                if ((statRegister and 0x08) != 0) requestInterrupt = true
            }

            PPUModes.VBLANK -> {
                statRegister = statRegister or 0x01
                if ((statRegister and 0x10) != 0) requestInterrupt = true
            }

            PPUModes.OAM -> {
                statRegister = statRegister or 0x02
                if ((statRegister and 0x20) != 0) requestInterrupt = true
            }

            PPUModes.PIXEL_TRANSFER -> {
                statRegister = statRegister or 0x03
            }
        }

        ppuRegisters.statRegister.value = (statRegister and 0xFF).toUByte()
        val lycInterrupt = ppuRegisters.treatLYC()
        if (requestInterrupt || lycInterrupt) {
            bus.triggerInterrupt(InterruptNames.STAT_INT)
        }
    }

    private fun draw() {
        ppuRegisters.counter++
        ppuRegisters.readLY()

        when (ppuRegisters.mode) {
            PPUModes.HBLANK -> hBlank()
            PPUModes.VBLANK -> vBlank()
            PPUModes.OAM -> oam()
            PPUModes.PIXEL_TRANSFER -> pixelTransfer()
        }
    }

    private fun hBlank() {
        if (ppuRegisters.counter == 114) {
            ppuRegisters.counter = 0
            ppuRegisters.currentLine++
            ppuRegisters.lyRegister.value = ppuRegisters.currentLine.toUByte()
            if (ppuRegisters.currentLine > 143) {
                ppuDrawer.requestRepaint()
                changeMode(PPUModes.VBLANK)
                bus.triggerInterrupt(InterruptNames.VBLANK_INT)
            } else {
                changeMode(PPUModes.OAM)
            }
        }
    }

    private fun vBlank() {
        if (ppuRegisters.counter == 114) {
            ppuRegisters.counter = 0
            ppuRegisters.currentLine++
            ppuRegisters.lyRegister.value = ppuRegisters.currentLine.toUByte()
            if (ppuRegisters.currentLine > 153) {
                ppuRegisters.currentLine = 0
                ppuRegisters.currentLineWindow = 0
                ppuRegisters.lyRegister.value = ppuRegisters.currentLine.toUByte()
                changeMode(PPUModes.OAM)
                getToSleep = true
            }
        }
    }

    private fun oam() {
        if (getToSleep) getToSleep = false
        if (ppuRegisters.counter == 20) {
            changeMode(PPUModes.PIXEL_TRANSFER)
        }
    }

    private fun pixelTransfer() { // NOSONAR
        if (ppuRegisters.counter == 63) {
            ppuRegisters.negativeTiles =
                tileDataAddress == ReservedAddresses.TILE_DATA_2.memoryAddress

            if (ppuRegisters.backgroundOn) {
                ppuDrawer.drawBackground(backgroundMapAddress, tileDataAddress)
            }
            if (ppuRegisters.windowOn) {
                ppuDrawer.drawWindow(windowMapAddress, tileDataAddress)
            }
            if (ppuRegisters.spriteOn) {
                ppuDrawer.drawSprite()
            }

            changeMode(PPUModes.HBLANK)
        }
    }
}
