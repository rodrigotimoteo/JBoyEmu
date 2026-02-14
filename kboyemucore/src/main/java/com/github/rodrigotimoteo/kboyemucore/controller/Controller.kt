package com.github.rodrigotimoteo.kboyemucore.controller

import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.bus.Bus
import com.github.rodrigotimoteo.kboyemucore.cpu.interrupts.InterruptNames

/**
 * This class is responsible for handling the joypad input and updating the joypad value accordingly.
 *
 * @author rodrigotimoteo
 */
class Controller(
    private val bus: Bus
) {
    /**
     * Stores the current value of the joypad, which is updated when buttons are pressed and released
     */
    private var _joypadValue = 0xFF

    /**
     * Method used to update the joypad value when a button is pressed
     *
     * @param button button that was pressed
     */
    fun buttonPressed(button: Button) {
        val keyPressed = button.code

        _joypadValue = _joypadValue and ((1 shl keyPressed).inv())
        bus.triggerInterrupt(InterruptNames.JOYPAD_INT)
    }

    /**
     * Method used to update the joypad value when a button is released
     *
     * @param button button that was released
     */
    fun buttonReleased(button: Button) {
        val keyPressed = button.code

        _joypadValue = _joypadValue or (1 shl keyPressed)
    }

    /**
     * Returns the current value of the joypad based on the provided [joypadInfo] (which is the value
     * of the P1 register)
     *
     * @param joypadInfo value of the P1 register
     * @return current value of the joypad based on the provided [joypadInfo]
     */
    fun getJoypad(joypadInfo: UByte): UByte {
        if ((joypadInfo.toInt() and 0x10) == 0) {
            return ((_joypadValue and 0xF0) shr 4).toUByte()
        } else if ((joypadInfo.toInt() and 0x20) == 0) {
            return (_joypadValue and 0x0F).toUByte()
        }
        return 0x00u
    }
}
