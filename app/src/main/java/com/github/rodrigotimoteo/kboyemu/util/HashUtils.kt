package com.github.rodrigotimoteo.kboyemu.util

import java.security.MessageDigest

/**
 * Computes an MD5 hex string from the given [UByteArray]. Used to generate deterministic
 * identifiers for ROM content so that save states and save games are stored per-ROM.
 *
 * @param bytes raw ROM content
 * @return lowercase hex-encoded MD5 hash
 *
 * @author rodrigotimoteo
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun md5Hex(bytes: UByteArray): String =
    MessageDigest.getInstance("MD5")
        .digest(bytes.asByteArray())
        .joinToString("") { "%02x".format(it) }

