package com.github.rodrigotimoteo.kboyemucore.emulator

import com.github.rodrigotimoteo.kboyemucore.util.Logger

/**
 * Default implementation of [Logger] that prints logs to the console
 */
class LoggerImpl : Logger {

    override fun i(message: String) {
        println(message)
    }

    override fun e(message: String, throwable: Throwable?) {
        error("$message $throwable")
    }
}