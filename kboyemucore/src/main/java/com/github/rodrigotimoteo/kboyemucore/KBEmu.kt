package com.github.rodrigotimoteo.kboyemucore

import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.emulator.KBoyEmulatorFactory
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintStream



@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val emulator = KBoyEmulatorFactory()
    val rom = File("/Users/ctw03939/StudioProjects/JBoyEmu/app/src/main/assets/02-interrupts.gb").readBytes().toUByteArray()
    emulator.loadRom(Rom(rom))
    val debug = PrintStream("A.txt")
    System.setOut(debug)
    emulator.run()
    runBlocking {

        emulator.job()?.join()
    }
}
