package com.github.rodrigotimoteo.kboyemucore.api

/**
 * Enum class that carries all the values that can be injected as input in this emulator
 *
 * @author rodrigotimoteo
 */
enum class Button(val code: Int) {
    RIGHT (0),
    LEFT  (1),
    UP    (2),
    DOWN  (3),
    A     (4),
    B     (5),
    SELECT(6),
    START (7)
}
