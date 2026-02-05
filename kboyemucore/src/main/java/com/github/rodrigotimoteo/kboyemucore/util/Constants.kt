package com.github.rodrigotimoteo.kboyemucore.util

/** Screen Dimensions */
internal const val WIDTH = 160
internal const val HEIGHT = 144

/** CPU Specific */
internal const val CARRY_BIT = 4
internal const val HALF_CARRY_BIT = 5
internal const val SUBTRACT_BIT = 6
internal const val ZERO_BIT = 7

internal const val PROGRAM_COUNTER_INITIAL_VALUE = 0x0100
internal const val STACK_POINTER_INITIAL_VALUE = 0xFFFE

internal const val AF_INITIAL_VALUE = 0x01B0
internal const val BC_INITIAL_VALUE = 0x0013
internal const val DE_INITIAL_VALUE = 0x00D8
internal const val HL_INITIAL_VALUE = 0x014D

/** Others */
internal const val EIGHT_BITS = 8
internal const val FILTER_TOP_BITS = 0xFF00
internal const val FILTER_LOWER_BITS = 0x00FF
internal const val FILTER_16_BITS = 0xFFFF
