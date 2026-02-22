package com.github.rodrigotimoteo.kboyemucore.memory.rom

/**
 * Defines specific behavior hold by memory modules responsible for holding the contents of roms,
 * these are mostly ram bank controllers at least for now
 *
 * @author rodrigotimoteo
 **/
interface RomModule {

    /**
     * Method that checks if ram is currently enabled
     *
     * @return current ram status
     */
    val ramStatus: Boolean

    /**
     * Active external RAM bank selected by the cartridge controller.
     * Defaults to 0 for controllers that do not support RAM banking.
     */
    val ramBankNumber: Int

    /**
     * Method that returns the number of rom banks present in this rom
     *
     * @return the bit that translates into how many banks are available
     */
    val romBanks: Int

    /**
     * Method that returns the number of ram banks present in this rom
     *
     * @return the bit that translates into how many banks are available
     */
    val ramBanks: Int

    /**
     * True when an RTC register is currently mapped into the ERAM region (0xA000–0xBFFF).
     * Always false for non-RTC cartridges.
     */
    val hasRtcMapped: Boolean get() = false

    /**
     * Reads the currently-latched RTC register value.
     * Only meaningful when [hasRtcMapped] is true.
     */
    fun readRtcRegister(): UByte = 0xFFu

    /**
     * Writes a value to the currently-selected RTC register.
     * Only meaningful when [hasRtcMapped] is true.
     */
    fun writeRtcRegister(value: UByte) {}

    /**
     * Advances the RTC by one second. Should be called once per real-time second
     * from the emulation loop. No-op for non-RTC cartridges.
     */
    fun tickRtc() {}
}
