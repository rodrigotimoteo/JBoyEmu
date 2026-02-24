package com.github.rodrigotimoteo.kboyemu.presentation.emulator.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.translateGbPixelsToArgb
import com.github.rodrigotimoteo.kboyemucore.api.Button
import com.github.rodrigotimoteo.kboyemucore.api.KBoyEmulator
import com.github.rodrigotimoteo.kboyemucore.api.Rom
import com.github.rodrigotimoteo.kboyemucore.spu.SPU
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.util.concurrent.locks.LockSupport

/**
 * Represents the current state of the emulator screen
 */
sealed interface EmulatorState {
    data object WaitingForRom : EmulatorState
    data object Running : EmulatorState
}

@OptIn(ExperimentalUnsignedTypes::class)
@KoinViewModel
class KBoyEmulatorViewModel(
    private val context: Context,
    private val emulator: KBoyEmulator,
    private val logger: Logger,
) : ViewModel() {
    private val width = 160
    private val height = 144

    private val argbBuffer = IntArray(width * height)

    /** Current state of the emulator — waiting for a ROM or running */
    private val _state = MutableStateFlow<EmulatorState>(EmulatorState.WaitingForRom)
    val state: StateFlow<EmulatorState> = _state.asStateFlow()

    /** Current frame bitmap for display */
    internal val frameBitmap = MutableStateFlow(ImageBitmap(160, 144))

    /**
     * [AudioTrack] configured for stereo 16-bit PCM at [SPU.SAMPLE_RATE] Hz
     */
    private val audioTrack: AudioTrack = run {
        val minBufSize = AudioTrack.getMinBufferSize(
            SPU.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBufSize, SPU.SAMPLE_RATE / 5 * 2 * 2)

        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SPU.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }
    }

    /**
     * Dedicated audio thread that reads from the [AudioRingBuffer] and feeds the [AudioTrack].
     * The blocking write naturally paces consumption to [SPU.SAMPLE_RATE] Hz stereo.
     * Logs consumption stats once per second.
     */
    private val audioThread = Thread {
        val chunk = ShortArray(2048)
        var totalRead = 0L
        var emptyPolls = 0L
        var lastLogMs = System.currentTimeMillis()

        while (!Thread.currentThread().isInterrupted) {
            val n = emulator.audioRingBuffer.read(chunk, 0, chunk.size)
            if (n > 0) {
                audioTrack.write(chunk, 0, n)
                totalRead += n
            } else {
                emptyPolls++
                LockSupport.parkNanos(100_000)
            }

            val now = System.currentTimeMillis()
            if (now - lastLogMs >= 1000) {
                logger.d("Audio: read=$totalRead shorts/s emptyPolls=$emptyPolls " +
                        "playState=${audioTrack.playState}")
                totalRead = 0; emptyPolls = 0; lastLogMs = now
            }
        }
    }.apply {
        name = "KBoy-Audio"
        isDaemon = true
        priority = Thread.MAX_PRIORITY
    }

    /** Job collecting frames from the emulator */
    private var frameCollectorJob: Job? = null

    /**
     * Loads a ROM from the given content [Uri], starts the emulation and audio thread
     *
     * @param uri content URI of the ROM file selected by the user
     */
    fun loadRom(uri: Uri) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        val romBytes = bytes.toUByteArray()

        emulator.loadRom(Rom(romBytes))

        frameCollectorJob?.cancel()
        frameCollectorJob = viewModelScope.launch {
            emulator.frames.collect { frameBuffer ->
                translateGbPixelsToArgb(frameBuffer.pixels, argbBuffer)

                val bitmap = createBitmap(160, 144)
                bitmap.setPixels(argbBuffer, 0, width, 0, 0, width, height)
                frameBitmap.value = bitmap.asImageBitmap()
            }
        }

        audioThread.start()
        emulator.run()
        _state.value = EmulatorState.Running
    }

    fun press(button: Button) {
        emulator.press(button)
    }

    fun release(button: Button) {
        emulator.release(button)
    }

    override fun onCleared() {
        super.onCleared()
        audioThread.interrupt()
        audioTrack.stop()
        audioTrack.release()
    }
}
