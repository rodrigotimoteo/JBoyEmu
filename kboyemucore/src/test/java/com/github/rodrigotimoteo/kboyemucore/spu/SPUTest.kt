package com.github.rodrigotimoteo.kboyemucore.spu

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SPUTest {

    @Test
    fun `test spu`() {
        val sut = SPU()

        assertNotNull(sut)
    }
}
