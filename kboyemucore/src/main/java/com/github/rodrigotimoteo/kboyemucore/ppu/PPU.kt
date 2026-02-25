package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import com.github.rodrigotimoteo.kboyemucore.ppu.drawer.CGBPPUDrawer
import com.github.rodrigotimoteo.kboyemucore.ppu.drawer.DMGPPUDrawer
import com.github.rodrigotimoteo.kboyemucore.ppu.drawer.IPPUDrawer
import com.github.rodrigotimoteo.kboyemucore.util.HEIGHT
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import com.github.rodrigotimoteo.kboyemucore.util.WIDTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("TooManyFunctions")
class PPU(
    private val bus: Bus,
    private val logger: Logger,
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
     * Reference to the PPU drawer — DMG or CGB implementation selected at construction time
     */
    internal val ppuDrawer: IPPUDrawer = if (cgb) CGBPPUDrawer(this, bus) else DMGPPUDrawer(this, bus)

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
     * Returns whether or not the LCD is On
     *
     * @return lcdOn
     */
    /** Set to true for one tick when VBlank starts; consumed by [isVBlankStart]. */
    private var vblankStart = false

    /**
     * Returns true once per frame when VBlank has just started, then resets.
     * Used by the emulation loop to pace frames to 60fps.
     *
     * @return true if VBlank has just started, false otherwise
     */
    fun isVBlankStart(): Boolean {
        return if (vblankStart) {
            vblankStart = false
            return true
        } else {
            false
        }
    }

    val lcdOn: Boolean
        get() = ppuRegisters.lcdOn

    /**
     * Updates the painting flow providing a new frame to be rendered
     *
     * @param painting content to be rendered as 2-bit color indices
     * @param colorPixels optional 15-bit RGB555 color data for CGB frames
     */
    internal fun propagatePaintingUpdate(painting: ByteArray, colorPixels: IntArray? = null) {
        _painting.value = FrameBuffer(
            pixels = painting.copyOf(),
            colorPixels = colorPixels?.copyOf()
        )

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
     * Method used when ppu is offline (lcd is not On) to check if anything changed (instead of
     * ticking the PPU)
     */
    fun checkLCDStatus() {
        ppuRegisters.readLCDControl()
    }

    /**
     * Method used to tick the PPU, this is done by first reading the LCD control and status registers
     * to check if anything changed and then calling the draw method to handle the drawing of the PPU
     */
    fun tick() {
        ppuRegisters.readLCDControl()
        ppuRegisters.readLCDStatus()
        draw()
    }

    /**
     * Method used to change the current mode of the PPU, this is done by first updating the STAT
     * register with the new mode and then checking if an interrupt should be requested based on the
     * new mode and the current configuration of the STAT register, finally it checks if the LYC
     * interrupt should be requested and if any of the two interrupts should be requested it triggers a
     * STAT interrupt
     *
     * @param mode to change to
     */
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

    /**
     * Method used to handle the drawing of the PPU, this is done by first incrementing the counter and
     * reading the current line, then it checks the current mode and calls the corresponding method to
     * handle it (HBlank, VBlank, OAM or Pixel Transfer)
     */
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

    /**
     * Method used to handle the HBlank phase of the PPU, this is done by first checking if the counter
     * is 114 (which is the number of cycles required for the HBlank phase) and if so it increments
     * the current line and updates the LY register, then it checks if the current line is greater than
     * 143 (which is the last line of the visible area) and if so it requests a repaint, changes the
     * mode to VBlank and triggers a VBlank interrupt. If the current line is not greater than 143 it
     * changes the mode to OAM
     */
    private fun hBlank() {
        if (ppuRegisters.counter == 114) {
            ppuRegisters.counter = 0
            ppuRegisters.currentLine++
            ppuRegisters.lyRegister.value = ppuRegisters.currentLine.toUByte()
            if (ppuRegisters.currentLine > 143) {
                ppuDrawer.requestRepaint()
                vblankStart = true
                changeMode(PPUModes.VBLANK)
                bus.triggerInterrupt(InterruptNames.VBLANK_INT)
            } else {
                changeMode(PPUModes.OAM)
            }
        }
    }

    /**
     * Method used to handle the VBlank phase of the PPU, this is done by first checking if the counter
     * is 114 (which is the number of cycles required for the VBlank phase) and if so it increments
     * the current line and updates the LY register, then it checks if the current line is greater than
     * 153 (which is the last line of the VBlank phase) and if so it resets the current line and current
     * line window, updates the LY register and changes the mode to OAM. If the current line is not
     * greater than 153 it stays in VBlank mode and waits for the next tick to increment the line again
     */
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

    /**
     * Method used to handle the OAM phase of the PPU, this is done by first checking if the PPU should
     * go to sleep (if in VBlank and nothing changed) and if so it sets the getToSleep variable to false
     * and then it checks if the counter is 20 (which is the number of cycles required for the OAM phase) and
     * if so it changes the mode to PIXEL_TRANSFER
     */
    private fun oam() {
        if (getToSleep) getToSleep = false
        if (ppuRegisters.counter == 20) {
            changeMode(PPUModes.PIXEL_TRANSFER)
        }
    }

    /**
     * Method used to draw the current line, this is done by first checking if the counter is 63 (which
     * is the number of cycles required for the pixel transfer phase), if so it checks if the current
     * tile data address is the one used for negative tile numbers (which affects how the tile data is
     * interpreted), then it checks if the background, window and sprite are enabled and draws them if
     * they are. Finally it changes the mode to HBLANK
     */
    private fun pixelTransfer() { // NOSONAR
        if (ppuRegisters.counter == 63) {
            ppuRegisters.negativeTiles =
                tileDataAddress == ReservedAddresses.TILE_DATA_2.memoryAddress

            // On CGB BG is always drawn; LCDC bit 0 is master priority not BG enable
            if (ppuRegisters.backgroundOn || cgb) {
                ppuDrawer.drawBackground(backgroundMapAddress, tileDataAddress)
            }
            if (ppuRegisters.windowOn) {
                ppuDrawer.drawWindow(windowMapAddress, tileDataAddress)
            }
            if (ppuRegisters.spriteOn) {
                ppuDrawer.drawSprite()
            }

            changeMode(PPUModes.HBLANK)

            // Tick CGB HBlank HDMA after entering HBlank
            if (cgb) {
                bus.tickHdma()
            }
        }
    }
}
