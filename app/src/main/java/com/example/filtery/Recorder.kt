package com.example.filtery

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

/** Collects the video track and the sound track into one .mp4; samples that arrive before the file starts are kept and written first. */
private class Mux(path: String, private val tracksNeeded: Int) {
    private class Pending(val track: Int, val data: ByteArray, val flags: Int, val pts: Long)

    private val m = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var added = 0
    private var started = false
    private val pending = ArrayList<Pending>()

    @Synchronized
    fun add(f: MediaFormat): Int {
        val i = m.addTrack(f)
        added++
        if (added == tracksNeeded) {
            m.start(); started = true
            for (p in pending) {
                val bi = MediaCodec.BufferInfo()
                bi.set(0, p.data.size, p.pts, p.flags)
                m.writeSampleData(p.track, ByteBuffer.wrap(p.data), bi)
            }
            pending.clear()
        }
        return i
    }

    @Synchronized
    fun write(track: Int, buf: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!started) {
            val arr = ByteArray(info.size)
            buf.get(arr)
            pending.add(Pending(track, arr, info.flags, info.presentationTimeUs))
            return
        }
        m.writeSampleData(track, buf, info)
    }

    @Synchronized
    fun finish() {
        try { if (started) m.stop() } catch (_: Exception) { }
        try { m.release() } catch (_: Exception) { }
    }
}

/**
 * Records the filtered frames (already drawn by the filter engine) and the microphone into an .mp4.
 * Frames are pushed with [push]; call [stop] to finish the file.
 */
class VideoRecorder(private val file: File, private val w: Int, private val h: Int, wantAudio: Boolean) {
    private val lock = Any()
    private val venc: MediaCodec
    private val surface: Surface
    private var aenc: MediaCodec? = null
    private var rec: AudioRecord? = null
    private val mux: Mux
    private val t0 = System.nanoTime() / 1000
    @Volatile private var running = true
    private var vThread: Thread? = null
    private var aThread: Thread? = null
    private val dst = Rect(0, 0, w, h)
    val hasAudio: Boolean

    init {
        val vf = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h)
        vf.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        vf.setInteger(MediaFormat.KEY_BIT_RATE, (w * h * 30 * 0.13).toInt().coerceIn(2_000_000, 9_000_000))
        vf.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        vf.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        venc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        venc.configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = venc.createInputSurface()
        venc.start()

        var audioOk = false
        if (wantAudio) {
            try {
                val rate = 44100
                val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val r = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 16384))
                if (r.state == AudioRecord.STATE_INITIALIZED) {
                    val af = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 1)
                    af.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    af.setInteger(MediaFormat.KEY_BIT_RATE, 64000)
                    af.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
                    val ae = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                    ae.configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    ae.start()
                    rec = r; aenc = ae; audioOk = true
                } else r.release()
            } catch (_: Exception) { audioOk = false }
        }
        hasAudio = audioOk
        mux = Mux(file.absolutePath, if (audioOk) 2 else 1)
    }

    fun start() {
        vThread = Thread { drainVideo() }.also { it.name = "filtery-venc"; it.start() }
        if (hasAudio) aThread = Thread { audioLoop() }.also { it.name = "filtery-audio"; it.start() }
    }

    /** Draws one finished picture into the video. */
    fun push(bmp: Bitmap) {
        synchronized(lock) {
            if (!running) return
            try {
                val cv = surface.lockCanvas(null)
                cv.drawBitmap(bmp, null, dst, null)
                surface.unlockCanvasAndPost(cv)
            } catch (_: Exception) { }
        }
    }

    private fun drainVideo() {
        val info = MediaCodec.BufferInfo()
        var track = -1
        var last = -1L
        try {
            while (true) {
                val i = venc.dequeueOutputBuffer(info, 10_000)
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = mux.add(venc.outputFormat)
                } else if (i >= 0) {
                    val buf = venc.getOutputBuffer(i)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                    if (buf != null && info.size > 0 && track >= 0) {
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        var pts = info.presentationTimeUs - t0
                        if (pts <= last) pts = last + 1
                        last = pts
                        info.presentationTimeUs = pts
                        mux.write(track, buf, info)
                    }
                    venc.releaseOutputBuffer(i, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
            }
        } catch (_: Exception) { }
    }

    private fun audioLoop() {
        val r = rec ?: return
        val ae = aenc ?: return
        val info = MediaCodec.BufferInfo()
        var track = -1
        var samples = 0L
        var basePts = -1L
        var last = -1L
        val pcm = ByteArray(2048)

        fun drain(wait: Long): Boolean {          // true when the end of stream was reached
            while (true) {
                val i = ae.dequeueOutputBuffer(info, wait)
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) track = mux.add(ae.outputFormat)
                else if (i >= 0) {
                    val buf = ae.getOutputBuffer(i)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                    if (buf != null && info.size > 0 && track >= 0) {
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        var pts = info.presentationTimeUs
                        if (pts <= last) pts = last + 1
                        last = pts
                        info.presentationTimeUs = pts
                        mux.write(track, buf, info)
                    }
                    ae.releaseOutputBuffer(i, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return true
                } else return false
            }
        }

        try {
            r.startRecording()
            while (running) {
                val n = r.read(pcm, 0, pcm.size)
                if (n <= 0) continue
                if (basePts < 0) basePts = (System.nanoTime() / 1000 - t0 - n / 2L * 1_000_000L / 44100L).coerceAtLeast(0L)
                val ii = ae.dequeueInputBuffer(10_000)
                if (ii >= 0) {
                    val ib = ae.getInputBuffer(ii)
                    if (ib != null) { ib.clear(); ib.put(pcm, 0, n) }
                    ae.queueInputBuffer(ii, 0, n, basePts + samples * 1_000_000L / 44100L, 0)
                    samples += n / 2
                }
                drain(0)
            }
            // end of stream
            var ii = ae.dequeueInputBuffer(100_000)
            if (ii >= 0) ae.queueInputBuffer(ii, 0, 0, basePts.coerceAtLeast(0L) + samples * 1_000_000L / 44100L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            var guard = 0
            while (guard++ < 200) { if (drain(10_000)) break }
        } catch (_: Exception) { }
    }

    /** Finishes the file. Safe to call once; blocks for a moment while the encoders flush. */
    fun stop() {
        synchronized(lock) { running = false }
        try { venc.signalEndOfInputStream() } catch (_: Exception) { }
        try { aThread?.join(3000) } catch (_: Exception) { }
        try { vThread?.join(3000) } catch (_: Exception) { }
        try { rec?.stop() } catch (_: Exception) { }
        try { rec?.release() } catch (_: Exception) { }
        try { aenc?.stop() } catch (_: Exception) { }
        try { aenc?.release() } catch (_: Exception) { }
        try { venc.stop() } catch (_: Exception) { }
        try { venc.release() } catch (_: Exception) { }
        try { surface.release() } catch (_: Exception) { }
        mux.finish()
    }
}
