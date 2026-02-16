package com.github.rodrigotimoteo.kboyemucore.api

/**
 * Enum class that carries all the values that can be injected as input in this emulator
 *
 * @author rodrigotimoteo
 */
enum class Button(val code: Int) {
    A     (0),
    B     (1),
    SELECT(2),
    START (3),
    RIGHT (4),
    LEFT  (5),
    UP    (6),
    DOWN  (7)
}
