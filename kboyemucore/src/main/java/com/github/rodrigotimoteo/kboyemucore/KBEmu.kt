package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import com.github.rodrigotimoteo.kboyemucore.emulator.LoggerImpl
import kotlinx.coroutines.runBlocking
import java.io.File


@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val logger = LoggerImpl()
    val emulator = KBoyEmulatorFactory(logger)
    val rom = File("").readBytes().toUByteArray()
    emulator.loadRom(Rom(rom))
//    val debug = PrintStream("A.txt")
//    System.setOut(debug)
    emulator.run()
    runBlocking {
        emulator.job()?.join()
    }
}
