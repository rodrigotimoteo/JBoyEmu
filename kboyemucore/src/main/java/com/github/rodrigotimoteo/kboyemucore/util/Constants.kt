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

/** CGB-specific initial values */
internal const val AF_INITIAL_VALUE_CGB = 0x11B0
internal const val BC_INITIAL_VALUE_CGB = 0x0000
internal const val DE_INITIAL_VALUE_CGB = 0xFF56
internal const val HL_INITIAL_VALUE_CGB = 0x000D

/** Others */
internal const val EIGHT_BITS = 8
internal const val FILTER_TOP_BITS = 0xFF00
internal const val FILTER_LOWER_BITS = 0x00FF
internal const val FILTER_16_BITS = 0xFFFF

/** Frame pacing */
internal const val FRAME_DURATION_MS_60FPS = 1000L / 61
internal const val FRAME_DURATION_MS_120FPS = 1000L / 120

/** Illegal State Strings */
internal const val ACCESSING_FRAME_BEFORE_READY =
    "Emulator has not been initialized first pass a valid ROM"
internal const val REGISTER_DOES_NOT_EXIST =
    "This register is not accessible through this method"
