package com.github.rodrigotimoteo.kboyemucore.ppu

import com.github.rodrigotimoteo.kboyemucore.DisplayPanel
import com.github.rodrigotimoteo.kboyemucore.api.FrameBuffer
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames
import com.github.rodrigotimoteo.kboyemucore.memory.ReservedAddresses
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PPU(
    private val bus: Bus,
) {

    private var display: DisplayPanel? = null

    private val LCDC_STATUS = 0xff41
    private val WINDOW_Y_REGISTER = 0xff4a
    private val WINDOW_X_REGISTER = 0xff4b

    private var mode = PPUModes.HBLANK

    private val _painting = MutableSharedFlow<FrameBuffer>(replay = 1)
    val painting = _painting.asSharedFlow()
    private var getToSleep = false

    private var cgb = false


    /**
     * Reference to [PPUDrawer]
     */
    internal val ppuDrawer = PPUDrawer(this, bus)

    /**
     * Reference to [PPURegisters]
     */
    internal val ppuRegisters = PPURegisters(this, bus)

    internal fun propagatePaintingUpdate() {

    }

    fun isGetToSleep(): Boolean {
        return getToSleep
    }

    fun setCgbMode() {
        cgb = true
        display!!.setCgbMode()
    }

    fun reset() {
//        ppuRegisters.resetCounter()
    }

    fun cycle() {
        ppuRegisters.readLCDControl()
        readLCDStatus()
        draw()
    }

    private fun readLCDStatus() {
        val bit: Int

        val LCD: Char = memory.getMemory(LCDC_STATUS)
        //Read bit 1 and 0
        bit = LCD.code and 0x03
        mode = bit
        memory.setPpuMode(mode)
    }

    private fun readWindow() {
        this.windowY = memory.getMemory(WINDOW_Y_REGISTER)
        this.windowX = memory.getMemory(WINDOW_X_REGISTER) - 7
    }



    private fun treatLYC(): Boolean {
        val lyc = bus.getValue(ReservedAddresses.LYC.memoryAddress).toInt()

        if (currentLine == lyc) {
            memory.setBit(LCDC_STATUS, 2)
            return (memory.getMemory(LCDC_STATUS) and 0x40) !== 0
        } else {
            memory.resetBit(LCDC_STATUS, 2)
        }

        return false
    }

    private fun changeMode(mode: PPUModes) {
        var lcdStatus: Int = bus.getValue(LCDC_STATUS).toInt() and 0xFC
        var requestInterrupt = false

        when (mode) {
            PPUModes.HBLANK -> {
                if ((lcdStatus and 0x08) != 0) requestInterrupt = true
            }

            PPUModes.VBLANK -> {
                lcdStatus = lcdStatus or 0x01
                if ((lcdStatus and 0x10) != 0) requestInterrupt = true
            }

            PPUModes.OAM -> {
                lcdStatus = lcdStatus or 0x02
                if ((lcdStatus and 0x20) != 0) requestInterrupt = true
            }

            PPUModes.PIXEL_TRANSFER -> {
                lcdStatus = lcdStatus or 0x03
            }
        }

        bus.setValueFromPPU(ReservedAddresses.LCDC.memoryAddress, lcdStatus.toUByte())
        //System.out.println(currentLine + "   " + Integer.toHexString(memory.getMemory(LYC_REGISTER)));
        val lycInterrupt = treatLYC()
        if (requestInterrupt || lycInterrupt) {
            bus.triggerInterrupt(InterruptNames.STAT_INT)
        }
    }

    private fun draw() {
        ppuRegisters.counter++
        ppuRegisters.readLY()

        when (mode) {
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
                display!!.drawImage(painting)
                requestRepaint()
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
            readWindow()
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
