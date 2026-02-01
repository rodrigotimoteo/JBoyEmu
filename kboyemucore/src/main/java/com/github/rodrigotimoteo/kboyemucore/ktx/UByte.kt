package com.github.rodrigotimoteo.kboyemucore.ktx

private fun checkInvalidBit(bit: Int) = require(bit in 0..7)

fun UByte.testBit(bit: Int): Boolean {
    checkInvalidBit(bit)
    return this and (1 shl bit).toUByte() == (1 shl bit).toUByte()
}

fun UByte.setBit(bit: Int): UByte {
    checkInvalidBit(bit)
    return this or ((1 shl bit).toUByte())
}

fun UByte.resetBit(bit: Int): UByte {
    checkInvalidBit(bit)
    return this and (1 shl bit).inv().toUByte()
}