package com.github.rodrigotimoteo.kboyemucore.api

/**
 * Value class to facilitate the passing of a "Rom" file parsed as a Byte array
 *
 * @property bytes content to be used as ROM
 *
 * @author rodrigotimoteo
 */
@JvmInline
value class Rom(val bytes: ByteArray)
