package com.github.rodrigotimoteo.kboyemu.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import com.github.rodrigotimoteo.kboyemu.data.audio.AudioPlayer
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import com.github.rodrigotimoteo.kboyemucore.spu.SPU
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import org.koin.core.annotation.Single
import java.util.concurrent.locks.LockSupport

/**
 * Default [AudioPlayer] implementation that uses an Android [AudioTrack] in streaming mode.
 * A dedicated high-priority daemon thread reads PCM samples from the emulator's [AudioRingBuffer]
 * and writes them to the track. Manages audio focus so playback pauses when another app takes
 * priority and resumes when focus is regained. Consumption stats are logged once per second.
 *
 * @author rodrigotimoteo
 */
@Single
class AudioPlayerImpl(
    context: Context,
    private val logger: Logger,
) : AudioPlayer {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** The currently active [AudioTrack], or null if not started */
    private var audioTrack: AudioTrack? = null

    /** The thread responsible for consuming audio samples and writing to the track */
    private var audioThread: Thread? = null

    /** Whether the audio thread should be actively writing samples to the track */
    @Volatile
    private var playing = false

    /** Audio attributes shared between the [AudioTrack] and the [AudioFocusRequest] */
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** Focus request used to acquire and release audio focus */
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    logger.d("Audio focus gained")
                    audioTrack?.play()
                    audioTrack?.setVolume(1f)
                    playing = true
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS -> {
                    logger.d("Audio focus lost")
                    playing = false
                    audioTrack?.pause()
                    audioTrack?.flush()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    logger.d("Audio focus ducking")
                    audioTrack?.setVolume(0.2f)
                }
            }
        }
        .build()

    override fun start(ringBuffer: AudioRingBuffer) {
        if (audioThread != null) return

        val focusResult = audioManager.requestAudioFocus(focusRequest)
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            logger.d("Audio focus not granted, starting anyway")
        }

        val minBufSize = AudioTrack.getMinBufferSize(
            SPU.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = maxOf(minBufSize, SPU.SAMPLE_RATE / 5 * 2 * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
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

        audioTrack = track
        playing = true

        audioThread = Thread {
            val chunk = ShortArray(2048)
            var totalRead = 0L
            var emptyPolls = 0L
            var lastLogMs = System.currentTimeMillis()

            while (!Thread.currentThread().isInterrupted) {
                if (!playing) {
                    LockSupport.parkNanos(1_000_000)
                    continue
                }

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
        playing = false
        audioThread?.interrupt()
        audioThread = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}