package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintStream



@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val emulator = KBoyEmulatorFactory()
    val rom = File("/Users/ctw03939/StudioProjects/JBoyEmu/app/src/main/assets/01-special.gb").readBytes().toUByteArray()
    emulator.loadRom(Rom(rom))
    emulator.run()
    val debug = PrintStream("A.txt")
    System.setOut(debug)
    runBlocking {

        emulator.job()?.join()
    }
}
