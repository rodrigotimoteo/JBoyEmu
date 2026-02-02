package com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.translateGbPixelsToArgb
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import timber.log.Timber

@OptIn(ExperimentalUnsignedTypes::class)
@KoinViewModel
class KBoyEmulatorViewModel(
    context: Context,
    emulator: KBoyEmulator,
) : ViewModel() {
    private val width = 160
    private val height = 144

    private val bitmap = createBitmap(width, height)

    private val argbBuffer = IntArray(width * height)

    internal var frameBitmap: StateFlow<ImageBitmap>

    init {
        val romBytes = context.assets.open("08-misc instrs.gb").readBytes().toUByteArray()
        emulator.loadRom(Rom(romBytes))
        emulator.run()
        frameBitmap = emulator.frames
            .map { frame ->
                translateGbPixelsToArgb(
                    frame.pixels,
                    argbBuffer
                )

                bitmap.setPixels(
                    argbBuffer,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height
                )

                bitmap.asImageBitmap()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                bitmap.asImageBitmap()
            )
    }

}