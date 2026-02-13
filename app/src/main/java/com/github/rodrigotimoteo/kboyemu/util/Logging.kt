package com.github.rodrigotimoteo.kboyemu.util

import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single
import timber.log.Timber

/**
 * Implementation of [Logger] that uses Timber to log messages
 *
 * @author rodrigotimoteo
 */
@Single
class EmulatorLogger : Logger {

    override fun i(message: String) {
        Timber.i(message)
    }

    override fun e(message: String, throwable: Throwable?) {
        Timber.e(throwable, message)
    }
}


