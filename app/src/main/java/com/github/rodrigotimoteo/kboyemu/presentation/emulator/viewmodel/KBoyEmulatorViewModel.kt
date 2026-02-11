package com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.translateGbPixelsToArgb
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@OptIn(ExperimentalUnsignedTypes::class)
@KoinViewModel
class KBoyEmulatorViewModel(
    context: Context,
    emulator: KBoyEmulator,
) : ViewModel() {
    private val width = 160
    private val height = 144

    private val argbBuffer = IntArray(width * height)

    internal val frameBitmap = MutableStateFlow(ImageBitmap(160, 144))

    init {
        val romBytes = context.assets.open("11-op a,(hl).gb").readBytes().toUByteArray()
        emulator.loadRom(Rom(romBytes))

        viewModelScope.launch {
            emulator.frames.collect { frameBuffer ->
                translateGbPixelsToArgb(
                    frameBuffer.pixels,
                    argbBuffer
                )

                val bitmap = createBitmap(160, 144)
                bitmap.setPixels(
                    argbBuffer,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height
                )

                frameBitmap.value = bitmap.asImageBitmap()
            }
        }

        emulator.run()
    }
}
