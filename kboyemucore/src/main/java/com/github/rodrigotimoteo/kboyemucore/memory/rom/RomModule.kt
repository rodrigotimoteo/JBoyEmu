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
    fun getRamStatus(): Boolean

    /**
     * Method that returns the number of ram banks present in this rom
     *
     * @return the bit that translates into how many banks are available
     */
    fun getRamBanks(): Int
}
