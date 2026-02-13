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

    private val _painting = MutableStateFlow(
        FrameBuffer(WIDTH, HEIGHT, ByteArray(WIDTH * HEIGHT))
    )

    val painting = _painting.asStateFlow()

    private var getToSleep = false

    private var cgb = false

    /**
     * Reference to [PPUDrawer]
     */
    internal val ppuDrawer = PPUDrawer(this, bus)

    /**
     * Reference to [PPURegisters]
     */
    internal val ppuRegisters = PPURegisters(bus)

    private var frameCount = 0
    private var lastTimestampMs = System.currentTimeMillis()

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

    fun isGetToSleep(): Boolean {
        return getToSleep
    }

    fun setCgbMode() {
        cgb = true
//        display!!.setCgbMode()
    }

    fun tick() {
        ppuRegisters.readLCDControl()
        ppuRegisters.readLCDStatus()
        draw()
    }

    private fun changeMode(mode: PPUModes) {
        var statRegister: Int = bus.getValue(ReservedAddresses.LCDC.memoryAddress).toInt() and 0xFC
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

        bus.setValueFromPPU(ReservedAddresses.STAT.memoryAddress, statRegister.toUByte())
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
            bus.setValueFromPPU(
                ReservedAddresses.LY.memoryAddress,
                ppuRegisters.currentLine.toUByte()
            )
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
            bus.setValueFromPPU(
                ReservedAddresses.LY.memoryAddress,
                ppuRegisters.currentLine.toUByte()
            )
            if (ppuRegisters.currentLine > 153) {
                ppuRegisters.currentLine = 0
                ppuRegisters.currentLineWindow = 0
                bus.setValueFromPPU(
                    ReservedAddresses.LY.memoryAddress,
                    ppuRegisters.currentLine.toUByte()
                )
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
            val windowMapAddress: Int = if (ppuRegisters.windowTileMap) {
                ReservedAddresses.TILE_MAP_1.memoryAddress
            } else {
                ReservedAddresses.TILE_MAP_0.memoryAddress
            }

            val backgroundMapAddress: Int = if (ppuRegisters.backgroundTileMap) {
                ReservedAddresses.TILE_MAP_1.memoryAddress
            } else {
                ReservedAddresses.TILE_MAP_0.memoryAddress
            }

            // SWITCH_ROM_END is where TILE_DATA_0 starts
            val tileDataAddress = if (ppuRegisters.tileData) {
                ReservedAddresses.SWITCH_ROM_END.memoryAddress
            } else {
                ReservedAddresses.TILE_DATA_2.memoryAddress
            }

            if (tileDataAddress == ReservedAddresses.TILE_DATA_2.memoryAddress) {
                ppuRegisters.negativeTiles = true
            }

            //System.out.println(currentLine + "  " + Integer.toHexString(backgroundMapAddress));
//                    System.out.println(scrollX + " " + scrollY + "  " + cpu.getIsHalted());

            ppuRegisters.setScrolls()
            ppuRegisters.readWindow()
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
