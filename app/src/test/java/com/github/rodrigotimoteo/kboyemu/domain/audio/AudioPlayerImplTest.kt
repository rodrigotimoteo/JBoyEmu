package com.github.rodrigotimoteo.kboyemu.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.github.rodrigotimoteo.kboyemucore.spu.AudioRingBuffer
import com.github.rodrigotimoteo.kboyemucore.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AudioPlayerImpl]
 *
 * @author rodrigotimoteo
 */
class AudioPlayerImplTest {

    /** Mock for [AudioManager] */
    private val audioManagerMock: AudioManager = mockk(relaxed = true)

    /** Mock for [Context] */
    private val contextMock: Context = mockk {
        every { getSystemService(Context.AUDIO_SERVICE) } returns audioManagerMock
    }

    /** Mock for [Logger] */
    private val loggerMock: Logger = mockk(relaxed = true)

    /** Mock for [AudioTrack] returned by the builder */
    private val audioTrackMock: AudioTrack = mockk(relaxed = true)

    /** Mock for [AudioAttributes] */
    private val audioAttributesMock: AudioAttributes = mockk()

    /** Mock for [AudioFocusRequest] */
    private val focusRequestMock: AudioFocusRequest = mockk()

    /** Mock for [AudioFormat] */
    private val audioFormatMock: AudioFormat = mockk()

    /** Mock for [AudioRingBuffer] */
    private val ringBufferMock: AudioRingBuffer = mockk(relaxed = true)

    /** Instance of the audio player being tested */
    private lateinit var sut: AudioPlayerImpl

    @BeforeEach
    fun setUp() {
        mockkConstructor(AudioAttributes.Builder::class)
        every { anyConstructed<AudioAttributes.Builder>().setUsage(any()) } returns AudioAttributes.Builder()
        every { anyConstructed<AudioAttributes.Builder>().setContentType(any()) } returns AudioAttributes.Builder()
        every { anyConstructed<AudioAttributes.Builder>().build() } returns audioAttributesMock

        mockkConstructor(AudioFocusRequest.Builder::class)
        every { anyConstructed<AudioFocusRequest.Builder>().setAudioAttributes(any()) } returns AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN
        )
        every { anyConstructed<AudioFocusRequest.Builder>().setOnAudioFocusChangeListener(any()) } returns AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN
        )
        every { anyConstructed<AudioFocusRequest.Builder>().build() } returns focusRequestMock

        mockkStatic(AudioTrack::class)
        every { AudioTrack.getMinBufferSize(any(), any(), any()) } returns 4096

        mockkConstructor(AudioFormat.Builder::class)
        every { anyConstructed<AudioFormat.Builder>().setSampleRate(any()) } returns AudioFormat.Builder()
        every { anyConstructed<AudioFormat.Builder>().setEncoding(any()) } returns AudioFormat.Builder()
        every { anyConstructed<AudioFormat.Builder>().setChannelMask(any()) } returns AudioFormat.Builder()
        every { anyConstructed<AudioFormat.Builder>().build() } returns audioFormatMock

        mockkConstructor(AudioTrack.Builder::class)
        every { anyConstructed<AudioTrack.Builder>().setAudioAttributes(any()) } returns AudioTrack.Builder()
        every { anyConstructed<AudioTrack.Builder>().setAudioFormat(any()) } returns AudioTrack.Builder()
        every { anyConstructed<AudioTrack.Builder>().setBufferSizeInBytes(any()) } returns AudioTrack.Builder()
        every { anyConstructed<AudioTrack.Builder>().setTransferMode(any()) } returns AudioTrack.Builder()
        every { anyConstructed<AudioTrack.Builder>().build() } returns audioTrackMock

        every { audioManagerMock.requestAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        sut = AudioPlayerImpl(contextMock, loggerMock)
    }

    @AfterEach
    fun tearDown() {
        sut.stop()
        unmockkConstructor(AudioTrack.Builder::class)
        unmockkConstructor(AudioFormat.Builder::class)
        unmockkConstructor(AudioAttributes.Builder::class)
        unmockkConstructor(AudioFocusRequest.Builder::class)
        unmockkStatic(AudioTrack::class)
    }

    // ── start ───────────────────────────────────────────────────────────────

    @Test
    fun `when starting then audio focus is requested`() {
        sut.start(ringBufferMock)

        verify(exactly = 1) { audioManagerMock.requestAudioFocus(any()) }
    }

    @Test
    fun `when starting then audio track is built and played`() {
        sut.start(ringBufferMock)

        verify(exactly = 1) { anyConstructed<AudioTrack.Builder>().build() }
        verify(exactly = 1) { audioTrackMock.play() }
    }

    @Test
    fun `when starting and audio focus is not granted then logs message and starts anyway`() {
        every { audioManagerMock.requestAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_FAILED

        sut.start(ringBufferMock)

        verify { loggerMock.d("Audio focus not granted, starting anyway") }
        verify(exactly = 1) { audioTrackMock.play() }
    }

    @Test
    fun `when starting twice then second call is ignored`() {
        sut.start(ringBufferMock)
        sut.start(ringBufferMock)

        verify(exactly = 1) { anyConstructed<AudioTrack.Builder>().build() }
        verify(exactly = 1) { audioTrackMock.play() }
    }

    // ── stop ────────────────────────────────────────────────────────────────

    @Test
    fun `when stopping after start then audio track is stopped and released`() {
        sut.start(ringBufferMock)

        sut.stop()

        verify(exactly = 1) { audioTrackMock.stop() }
        verify(exactly = 1) { audioTrackMock.release() }
    }

    @Test
    fun `when stopping after start then audio focus is abandoned`() {
        sut.start(ringBufferMock)

        sut.stop()

        verify(exactly = 1) { audioManagerMock.abandonAudioFocusRequest(any()) }
    }

    @Test
    fun `when stopping without start then does not throw`() {
        assertDoesNotThrow { sut.stop() }
    }

    @Test
    fun `when stopping without start then audio track is not touched`() {
        sut.stop()

        verify(exactly = 0) { audioTrackMock.stop() }
        verify(exactly = 0) { audioTrackMock.release() }
    }

    @Test
    fun `when stopping without start then audio focus is still abandoned`() {
        sut.stop()

        verify(exactly = 1) { audioManagerMock.abandonAudioFocusRequest(any()) }
    }

    @Test
    fun `when stopping then starting again then new audio track is created`() {
        sut.start(ringBufferMock)
        sut.stop()
        sut.start(ringBufferMock)

        verify(exactly = 2) { anyConstructed<AudioTrack.Builder>().build() }
        verify(exactly = 2) { audioTrackMock.play() }
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @Test
    fun `when starting then a daemon thread named KBoy-Audio is spawned`() {
        sut.start(ringBufferMock)

        Thread.sleep(50)

        val audioThread = Thread.getAllStackTraces().keys.firstOrNull { it.name == "KBoy-Audio" }
        assertNotNull(audioThread) { "Expected a thread named KBoy-Audio" }
        assertTrue(audioThread!!.isDaemon) { "Audio thread should be a daemon" }
    }

    @Test
    fun `when stopping then audio thread is no longer alive`() {
        sut.start(ringBufferMock)
        Thread.sleep(50)

        sut.stop()
        Thread.sleep(50)

        val audioThread = Thread.getAllStackTraces().keys.firstOrNull { it.name == "KBoy-Audio" }
        assertTrue(audioThread == null || !audioThread.isAlive) { "Audio thread should be stopped" }
    }
}