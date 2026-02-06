package com.github.rodrigotimoteo.kboyemucore.util

import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator

/**
 * Helper interface to enable logging in the [KBoyEmulator] core implementation
 */
interface Logger {

    /**
     * Logs an information log
     *
     * @param message to log
     */
    fun i(message: String)

    /**
     * Logs a debug log
     *
     * @param message to log
     */
    fun d(message: String)

    /**
     * Logs an error log with the possibility of logging an exception
     *
     * @param message to log
     * @param throwable error that needs to be logged
     */
    fun e(message: String, throwable: Throwable?)
}
