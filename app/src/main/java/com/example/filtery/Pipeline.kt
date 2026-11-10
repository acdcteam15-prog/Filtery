package com.example.filtery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.google.mlkit.vision.face.Face as MlFace

private const val TAG = "Filtery"

/** Turns ML Kit's face results into our own [Face] (and works with no internet: the model is inside the app). */
internal object FaceFinder {
    fun options(accurate: Boolean, tracking: Boolean): FaceDetectorOptions {
        val b = FaceDetectorOptions.Builder()
            .setPerformanceMode(if (accurate) FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE else FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
        if (tracking) b.enableTracking()
        return b.build()
    }

    fun convert(f: MlFace, dw: Float, dh: Float, idx: Int): Face {
        val b = f.boundingBox
        val id = f.trackingId ?: idx
        val rot = -f.headEulerAngleZ
        val base = Face.fromBox(id, b.exactCenterX() / dw, b.exactCenterY() / dh, b.width() / dw, b.height() / dh, rot, dw / dh)
        val le = f.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val re = f.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        if (le != null && re != null) {
            val a = le.x / dw; val ay = le.y / dh; val c = re.x / dw; val cy = re.y / dh
            if (a <= c) { base.lx = a; base.ly = ay; base.rx = c; base.ry = cy } else { base.lx = c; base.ly = cy; base.rx = a; base.ry = ay }
        }
        val nose = f.getLandmark(FaceLandmark.NOSE_BASE)?.position
        if (nose != null) { base.nx = nose.x / dw; base.ny = nose.y / dh }
        val ml = f.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mr = f.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mb = f.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        if (ml != null && mr != null) { base.mx = (ml.x + mr.x) / 2f / dw; base.my = (ml.y + mr.y) / 2f / dh }
        else if (mb != null) { base.mx = mb.x / dw; base.my = mb.y / dh - 0.03f * b.height() / dh }
        return base
    }

    /** One careful look at a finished photo. Blocks until done: call from a background thread. */
    fun detectBlocking(src: Bitmap): List<Face> {
        val sc = min(1f, 720f / max(src.width, src.height))
        val small = if (sc < 1f) Bitmap.createScaledBitmap(src, (src.width * sc).roundToInt(), (src.height * sc).roundToInt(), true) else src
        val det = FaceDetection.getClient(options(true, false))
        val latch = CountDownLatch(1)
        val holder = arrayOfNulls<List<MlFace>>(1)
        try {
            det.process(InputImage.fromBitmap(small, 0))
                .addOnSuccessListener { holder[0] = it }
                .addOnCompleteListener { latch.countDown() }
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: Exception) { Log.w(TAG, "face detection failed", e) }
        try { det.close() } catch (_: Exception) { }
        val res = holder[0] ?: emptyList()
        val out = ArrayList<Face>()
        for ((i, f) in res.withIndex()) out.add(convert(f, small.width.toFloat(), small.height.toFloat(), i))
        if (small !== src) small.recycle()
        return out
    }
}

/** Opens a photo taken by the camera, turns it upright, applies the filter at full size and saves it. */
object Photo {
    private fun decode(path: String, maxLong: Int): Bitmap? {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, o)
        var sample = 1
        while (max(o.outWidth, o.outHeight) / sample > maxLong) sample *= 2
        val d = BitmapFactory.Options()
        d.inSampleSize = sample
        d.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bmp = BitmapFactory.decodeFile(path, d) ?: return null
        val m = Matrix()
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        }
        return if (m.isIdentity) bmp else {
            val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (r !== bmp) bmp.recycle()
            r
        }
    }

    /** Returns the finished picture's file in the app gallery, or null if it failed. */
    fun develop(ctx: Context, shot: File, fx: Fx, k: Float, caption: String, mirror: Boolean, high: Boolean): File? {
        var maxLong = if (high) 4096 else 2048
        for (attempt in 0 until 3) {
            try {
                var up = decode(shot.absolutePath, maxLong) ?: return null
                if (mirror) {
                    val m = Matrix(); m.postScale(-1f, 1f)
                    val r = Bitmap.createBitmap(up, 0, 0, up.width, up.height, m, true)
                    if (r !== up) up.recycle()
                    up = r
                }
                // same shape as the live picture (like a phone screen), so the photo matches what you saw
                val aim = Pipeline.SIZES[1].width.toFloat() / Pipeline.SIZES[1].height
                val have = up.width.toFloat() / up.height
                if (abs(have - aim) > 0.01f) {
                    val nw = if (have > aim) (up.height * aim).roundToInt() else up.width
                    val nh = if (have > aim) up.height else (up.width / aim).roundToInt()
                    val cr = Bitmap.createBitmap(up, (up.width - nw) / 2, (up.height - nh) / 2, nw, nh)
                    if (cr !== up) up.recycle()
                    up = cr
                }
                val faces = if (fx.face) FaceFinder.detectBlocking(up) else emptyList()
                val out = Bitmap.createBitmap(up.width, up.height, Bitmap.Config.ARGB_8888)
                val t = (System.currentTimeMillis() % 60000L) / 1000f
                FxEngine().render(up, out, fx, k, faces, t, caption.ifBlank { null })
                up.recycle()
                val f = Saver.saveJpeg(ctx, out, 92)
                out.recycle()
                return f
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "photo too big, retrying smaller", e)
                maxLong = maxLong * 2 / 3
            }
        }
        return null
    }
}

/**
 * Camera -> filter -> screen/video, all on the phone. CameraX hands over frames; each frame is made upright, filtered by [FxEngine]
 * and shown (and recorded when a recorder is attached). Faces are found by ML Kit only while the chosen filter needs them.
 */
class Pipeline(
    private val app: Context,
    private val onFrame: (Bitmap) -> Unit,
    private val onFaces: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) {
    companion object {
        /** Live picture sizes (about 9:16 like a phone screen): fast, balanced, sharp. */
        val SIZES = arrayOf(Size(432, 768), Size(544, 960), Size(720, 1280))
    }

    private val main = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadExecutor()
    private val engine = FxEngine()
    private val t0 = System.nanoTime()
    private val posted = AtomicBoolean(false)
    private val detecting = AtomicBoolean(false)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    @Volatile var fx: Fx = Filters.original
    @Volatile var intensity: Float = 1f
    @Volatile var caption: String = ""
    @Volatile var previewQ: Int = 1
    @Volatile var recorder: VideoRecorder? = null
    @Volatile var front: Boolean = true
    @Volatile var paused: Boolean = false
    @Volatile private var faces: List<Face> = emptyList()
    @Volatile private var lastDetect = 0L
    private var missed = 0
    private var hadFace = false
    private val tracked = HashMap<Int, Face>()

    private var frame: Bitmap? = null
    private var outs: Array<Bitmap>? = null
    private var outIdx = 0

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var detector: com.google.mlkit.vision.face.FaceDetector? = null

    val hasFlash: Boolean get() = try { camera?.cameraInfo?.hasFlashUnit() == true } catch (_: Exception) { false }
    val maxZoom: Float get() = try { camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f } catch (_: Exception) { 1f }

    fun start(owner: LifecycleOwner, front: Boolean, flashMode: Int) {
        this.front = front
        val fut = ProcessCameraProvider.getInstance(app)
        fut.addListener(Runnable {
            try {
                val p = fut.get()
                provider = p
                val sel = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                if (!p.hasCamera(sel)) { onError(if (front) "No front camera found." else "No back camera found."); return@Runnable }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .setResolutionStrategy(ResolutionStrategy(Size(960, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                            .build()
                    )
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(exec, ImageAnalysis.Analyzer { proxy -> analyze(proxy) })
                val ic = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build())
                    .setFlashMode(flashFor(flashMode))
                    .build()
                imageCapture = ic
                p.unbindAll()
                camera = p.bindToLifecycle(owner, sel, analysis, ic)
            } catch (e: Exception) {
                Log.w(TAG, "camera start failed", e)
                onError("The camera could not start: ${e.message ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun stop() {
        try { provider?.unbindAll() } catch (_: Exception) { }
        camera = null
    }

    fun release() {
        stop()
        try { detector?.close() } catch (_: Exception) { }
        exec.shutdown()
    }

    private fun flashFor(m: Int) = when (m) { 1 -> ImageCapture.FLASH_MODE_AUTO; 2 -> ImageCapture.FLASH_MODE_ON; else -> ImageCapture.FLASH_MODE_OFF }

    fun setFlash(m: Int) { try { imageCapture?.flashMode = flashFor(m) } catch (_: Exception) { } }

    fun torch(on: Boolean) { try { camera?.cameraControl?.enableTorch(on) } catch (_: Exception) { } }

    fun zoom(ratio: Float) { try { camera?.cameraControl?.setZoomRatio(ratio) } catch (_: Exception) { } }

    /** Takes a full-size picture into [file]; [done] is called on the main thread. */
    fun shoot(file: File, done: (String?) -> Unit) {
        val ic = imageCapture
        if (ic == null) { done("The camera is not ready yet."); return }
        ic.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(app), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) { done(null) }
            override fun onError(exception: ImageCaptureException) { done(exception.message ?: "The photo failed.") }
        })
    }

    // ------------------------------------------------------------------ one live frame
    private fun analyze(proxy: ImageProxy) {
        try {
            if (paused) return
            val sz = SIZES[previewQ.coerceIn(0, SIZES.size - 1)]
            var fr = frame
            if (fr == null || fr.width != sz.width) {
                fr = Bitmap.createBitmap(sz.width, sz.height, Bitmap.Config.ARGB_8888)
                frame = fr
                outs = Array(3) { Bitmap.createBitmap(sz.width, sz.height, Bitmap.Config.ARGB_8888) }
            }
            val raw = proxy.toBitmap()
            drawUpright(raw, proxy.imageInfo.rotationDegrees, front, fr)
            raw.recycle()

            val f = fx
            if (f.face) detect(fr)
            val o = outs!![outIdx]
            outIdx = (outIdx + 1) % 3
            val t = (System.nanoTime() - t0) / 1_000_000_000f
            engine.render(fr, o, f, intensity, if (f.face) faces else emptyList(), t, caption.ifBlank { null })
            recorder?.push(o)
            if (posted.compareAndSet(false, true)) main.post { posted.set(false); onFrame(o) }
        } catch (e: Throwable) {
            Log.w(TAG, "frame failed", e)
        } finally {
            proxy.close()
        }
    }

    private fun drawUpright(raw: Bitmap, deg: Int, mirror: Boolean, into: Bitmap) {
        val W = into.width.toFloat(); val H = into.height.toFloat()
        val sideways = deg % 180 != 0
        val rw = (if (sideways) raw.height else raw.width).toFloat()
        val rh = (if (sideways) raw.width else raw.height).toFloat()
        val sc = max(W / rw, H / rh)
        val m = Matrix()
        m.postTranslate(-raw.width / 2f, -raw.height / 2f)
        m.postRotate(deg.toFloat())
        m.postScale(if (mirror) -sc else sc, sc)
        m.postTranslate(W / 2f, H / 2f)
        Canvas(into).drawBitmap(raw, m, paint)
    }

    // ------------------------------------------------------------------ faces
    private fun detect(fr: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastDetect < 70) return
        if (!detecting.compareAndSet(false, true)) return
        lastDetect = now
        try {
            val det = detector ?: FaceDetection.getClient(FaceFinder.options(false, true)).also { detector = it }
            val small = Bitmap.createBitmap(216, 384, Bitmap.Config.ARGB_8888)
            Canvas(small).drawBitmap(fr, null, android.graphics.Rect(0, 0, 216, 384), paint)
            det.process(InputImage.fromBitmap(small, 0))
                .addOnSuccessListener { list -> gotFaces(list, 216f, 384f) }
                .addOnCompleteListener { detecting.set(false); small.recycle() }
        } catch (e: Exception) {
            detecting.set(false)
        }
    }

    private fun gotFaces(list: List<MlFace>, dw: Float, dh: Float) {
        if (list.isEmpty()) {
            missed++
            if (missed > 6) { faces = emptyList(); tracked.clear() }
        } else {
            missed = 0
            val seen = HashSet<Int>()
            val res = ArrayList<Face>()
            for ((i, f) in list.withIndex()) {
                val nf = FaceFinder.convert(f, dw, dh, i)
                val prev = tracked[nf.id]
                val use = if (prev != null) { prev.lerp(nf, 0.55f); prev } else { tracked[nf.id] = nf; nf }
                seen.add(nf.id)
                res.add(use.duplicate())
            }
            tracked.keys.retainAll(seen)
            faces = res
        }
        val has = faces.isNotEmpty()
        if (has != hadFace) { hadFace = has; onFaces(has) }
    }
}
