package com.github.rodrigotimoteo.kboyemucore.spu

/**
 * Lock-free single-producer / single-consumer ring buffer for stereo PCM samples.
 * The SPU writes stereo pairs from the emulation thread via [writeSample] and the Android audio
 * thread drains them via [read].
 *
 * Indices wrap at [capacity] so they never overflow.
 *
 * @param capacity total Short slots, must be a power of 2 and ≥ 4.
 *
 * @author rodrigotimoteo
 */
class AudioRingBuffer(private val capacity: Int = 16384) {

    init {
        require(capacity >= 4 && capacity and (capacity - 1) == 0)
    }

    private val mask = capacity - 1
    private val buf = ShortArray(capacity)

    @Volatile
    private var head = 0
    @Volatile
    private var tail = 0

    /**
     * Writes one stereo sample pair into the buffer
     *
     * @param left sample value for the left channel
     * @param right sample value for the right channel
     * @return true if the pair was written, false if the buffer was full (sample dropped)
     */
    fun writeSample(left: Short, right: Short): Boolean {
        val h = head
        val nextHead = (h + 2) and mask
        if (nextHead == tail) return false

        buf[h] = left
        buf[(h + 1) and mask] = right
        head = nextHead
        return true
    }

    /**
     * Reads up to [maxSamples] shorts into [dst] starting at [offset]. Always returns an even
     * count to maintain stereo L/R alignment.
     *
     * @param dst destination array
     * @param offset starting index in [dst]
     * @param maxSamples maximum number of shorts to read
     * @return number of shorts actually read (always even)
     */
    fun read(dst: ShortArray, offset: Int, maxSamples: Int): Int {
        val t = tail
        val h = head
        val avail = (h - t + capacity) and mask
        val n = minOf(avail, maxSamples) and 0x7FFF_FFFE

        if (n <= 0) return 0

        for (i in 0 until n) dst[offset + i] = buf[(t + i) and mask]
        tail = (t + n) and mask
        return n
    }
}




