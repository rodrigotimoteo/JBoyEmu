package com.github.rodrigotimoteo.kboyemu.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import com.github.rodrigotimoteo.kboyemucore.spu.SPU
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single
import java.util.concurrent.locks.LockSupport

/**
 * Default [com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer] implementation that uses an Android [android.media.AudioTrack] in streaming mode.
 * A dedicated high-priority daemon thread reads PCM samples from the emulator's [com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer]
 * and writes them to the track. Consumption stats are logged once per second.
 *
 * @author rodrigotimoteo
 */
@Single
class AudioPlayerImpl(
    private val logger: Logger,
) : AudioPlayer {

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null

    override fun start(ringBuffer: AudioRingBuffer) {
        if (audioThread != null) return

        val minBufSize = AudioTrack.getMinBufferSize(
            SPU.Companion.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBufSize, SPU.Companion.SAMPLE_RATE / 5 * 2 * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SPU.Companion.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        audioTrack = track

        audioThread = Thread {
            val chunk = ShortArray(2048)
            var totalRead = 0L
            var emptyPolls = 0L
            var lastLogMs = System.currentTimeMillis()

            while (!Thread.currentThread().isInterrupted) {
                val n = ringBuffer.read(chunk, 0, chunk.size)
                if (n > 0) {
                    track.write(chunk, 0, n)
                    totalRead += n
                } else {
                    emptyPolls++
                    LockSupport.parkNanos(100_000)
                }

                val now = System.currentTimeMillis()
                if (now - lastLogMs >= 1000) {
                    logger.d(
                        "Audio: read=$totalRead shorts/s emptyPolls=$emptyPolls " +
                                "playState=${track.playState}"
                    )
                    totalRead = 0; emptyPolls = 0; lastLogMs = now
                }
            }
        }.apply {
            name = "KBoy-Audio"
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    override fun stop() {
        audioThread?.interrupt()
        audioThread = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}