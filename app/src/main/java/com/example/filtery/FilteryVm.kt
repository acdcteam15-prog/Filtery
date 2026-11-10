package com.example.filtery

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Everything the screens show and every button does lives here. */
class FilteryVm(private val app: Application) : AndroidViewModel(app) {
    private val stickersReady = Stickers.init(app)      // must come first: the filter list includes your PNG props
    val prefs = Prefs(app)
    private val main = Handler(Looper.getMainLooper())

    // ---- what the screens draw
    var frame by mutableStateOf<Bitmap?>(null)
    var tick by mutableIntStateOf(0)
    var hasCamera by mutableStateOf(false)
    var cameraError by mutableStateOf<String?>(null)
    var faceSeen by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)                // a photo is being finished
    var countdown by mutableIntStateOf(0)            // timer number on screen (0 = none)
    var screenFlash by mutableStateOf(false)
    var recording by mutableStateOf(false)
    var recordSeconds by mutableIntStateOf(0)
    var videoMode by mutableStateOf(false)
    var camGranted by mutableStateOf(false)

    // ---- choices
    var front by mutableStateOf(prefs.front)
    var flash by mutableIntStateOf(prefs.flash)      // 0 off, 1 auto, 2 on
    var timer by mutableIntStateOf(prefs.timer)      // 0, 3, 10 seconds
    var grid by mutableStateOf(prefs.grid)
    var fx by mutableStateOf(Filters.byId(prefs.lastFilter))
    var intensity by mutableStateOf(prefs.lastIntensity)
    var caption by mutableStateOf("")
    var tab by mutableStateOf("cat:FACES")           // fav, recent, or cat:<NAME>
    var zoom by mutableStateOf(1f)
    val favorites = mutableStateListOf<String>().also { it.addAll(prefs.favorites) }
    val recents = mutableStateListOf<String>().also { it.addAll(prefs.recents) }

    // ---- screens on top of the camera
    var screen by mutableStateOf("camera")           // camera, gallery, settings
    var viewing by mutableStateOf<File?>(null)
    val gallery = mutableStateListOf<File>()
    var lastShot by mutableStateOf<File?>(null)

    // ---- settings mirrors (so switches redraw)
    var mirror by mutableStateOf(prefs.mirror)
    var saveToPhone by mutableStateOf(prefs.saveToPhone)
    var photoHigh by mutableStateOf(prefs.photoHigh)
    var videoSound by mutableStateOf(prefs.videoSound)
    var previewQ by mutableIntStateOf(prefs.previewQ)

    // ---- updates
    var update by mutableStateOf<UpdateInfo?>(null)
    var updateFile by mutableStateOf<File?>(null)
    var updateStatus by mutableStateOf("")
    var updateBusy by mutableStateOf(false)
    var updateProgress by mutableIntStateOf(0)
    var autoUpdate by mutableStateOf(prefs.autoUpdate)
    var wifiOnly by mutableStateOf(prefs.wifiOnly)

    val pipeline = Pipeline(
        app,
        onFrame = { b -> frame = b; tick++; if (!hasCamera) hasCamera = true },
        onFaces = { has -> faceSeen = has },
        onError = { msg -> cameraError = msg },
    )

    private var timerJob: Job? = null
    private var recorder: VideoRecorder? = null
    private var recordFile: File? = null
    private var recordJob: Job? = null
    private var bound = false

    init {
        pipeline.fx = fx
        pipeline.intensity = intensity
        pipeline.previewQ = previewQ
        refreshGallery()
        refreshUpdateState()
    }

    val updateReady: Boolean get() = updateFile != null

    /** Opens one of the screens; the camera work pauses while another screen is on top. */
    fun go(name: String) {
        if (recording && name != "camera") stopRecording()
        screen = name
        pipeline.paused = name != "camera"
        if (name == "gallery") { viewing = null; refreshGallery() }
    }

    // ------------------------------------------------------------------ camera
    fun startCamera(owner: LifecycleOwner) {
        cameraError = null
        pipeline.start(owner, front, flash)
        bound = true
    }

    /** Starts the camera only if it is not running yet (coming back from the gallery must not restart it). */
    fun ensureCamera(owner: LifecycleOwner) { if (!bound) startCamera(owner) }

    fun stopCamera() {
        if (recording) stopRecording()
        pipeline.stop()
        bound = false
    }

    fun flip(owner: LifecycleOwner) {
        front = !front
        prefs.front = front
        zoom = 1f
        if (bound) startCamera(owner)
    }

    fun cycleFlash() {
        flash = (flash + 1) % 3
        prefs.flash = flash
        pipeline.setFlash(flash)
    }

    fun cycleTimer() {
        timer = when (timer) { 0 -> 3; 3 -> 10; else -> 0 }
        prefs.timer = timer
    }

    fun toggleGrid() { grid = !grid; prefs.grid = grid }

    fun applyZoom(z: Float) {
        val max = pipeline.maxZoom.coerceAtMost(8f)
        zoom = z.coerceIn(1f, max.coerceAtLeast(1f))
        pipeline.zoom(zoom)
    }

    // ------------------------------------------------------------------ filters
    fun pick(f: Fx) {
        fx = f
        pipeline.fx = f
        intensity = 1f; pipeline.intensity = 1f; prefs.lastIntensity = 1f
        prefs.lastFilter = f.id
        if (f !== Filters.original) {
            val r = recents.toMutableList()
            r.remove(f.id); r.add(0, f.id)
            while (r.size > 24) r.removeAt(r.size - 1)
            recents.clear(); recents.addAll(r)
            prefs.recents = r
        }
    }

    fun applyIntensity(v: Float) {
        intensity = v
        pipeline.intensity = v
    }

    fun commitIntensity() { prefs.lastIntensity = intensity }

    fun applyCaption(s: String) {
        caption = s.take(40)
        pipeline.caption = caption
    }

    fun isFavorite(f: Fx) = favorites.contains(f.id)

    fun toggleFavorite(f: Fx) {
        if (f === Filters.original) return
        if (!favorites.remove(f.id)) favorites.add(f.id)
        prefs.favorites = favorites.toList()
        say(if (favorites.contains(f.id)) "★ ${f.name} added to favorites" else "${f.name} removed from favorites")
    }

    /** A random filter; funny ones are three times as likely as the calm ones. */
    fun shuffle() {
        val all = Filters.all.filter { it !== Filters.original }
        val pool = ArrayList<Fx>()
        for (f in all) { val funny = f.cat == Cat.FACES || f.cat == Cat.WARPS || f.cat == Cat.FX; repeat(if (funny) 3 else 1) { pool.add(f) } }
        var pickOne = pool.random()
        if (pickOne === fx) pickOne = pool.random()
        pick(pickOne)
        tab = "cat:${pickOne.cat.name}"
        say("${pickOne.icon} ${pickOne.name}")
    }

    fun visibleFilters(): List<Fx> = when {
        tab == "fav" -> favorites.mapNotNull { id -> Filters.all.firstOrNull { it.id == id } }
        tab == "recent" -> recents.mapNotNull { id -> Filters.all.firstOrNull { it.id == id } }
        tab.startsWith("cat:") -> {
            val c = Cat.valueOf(tab.removePrefix("cat:"))
            if (c == Cat.COLOR) listOf(Filters.original) + Filters.inCat(c) else Filters.inCat(c)
        }
        else -> Filters.all
    }

    // ------------------------------------------------------------------ taking a photo
    fun shutter() {
        if (busy) return
        if (videoMode) { if (recording) stopRecording() else startRecording(); return }
        if (countdown > 0) { timerJob?.cancel(); countdown = 0; return }          // tap again to cancel the timer
        if (timer > 0) {
            timerJob = viewModelScope.launch {
                for (i in timer downTo 1) { countdown = i; delay(1000) }
                countdown = 0
                takePhoto()
            }
        } else takePhoto()
    }

    private fun takePhoto() {
        busy = true
        viewModelScope.launch {
            if (front && flash == 2) { screenFlash = true; delay(350) }
            val shot = Saver.tempPhoto(app)
            pipeline.shoot(shot) { err ->
                screenFlash = false
                if (err != null) { busy = false; say(err); return@shoot }
                finishPhoto(shot)
            }
        }
    }

    private fun finishPhoto(shot: File) {
        val f = fx; val k = intensity; val cap = caption; val mir = front && mirror; val high = photoHigh
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                try { Photo.develop(app, shot, f, k, cap, mir, high) } catch (e: Throwable) { null } finally { shot.delete() }
            }
            if (result == null) say("The photo could not be finished.") else {
                lastShot = result
                refreshGallery()
                if (saveToPhone) {
                    val ok = withContext(Dispatchers.IO) { Saver.publish(app, result) }
                    say(if (ok) "Saved ✓ (gallery + Pictures/Filtery)" else "Saved in Filtery (the phone gallery refused the copy)")
                } else say("Saved in the Filtery gallery ✓")
            }
            busy = false
        }
    }

    // ------------------------------------------------------------------ video
    private fun startRecording() {
        try {
            val sz = Pipeline.SIZES[previewQ.coerceIn(0, Pipeline.SIZES.size - 1)]
            val file = Saver.newVideo(app)
            val rec = VideoRecorder(file, sz.width, sz.height, videoSound)
            recordFile = file; recorder = rec
            rec.start()
            pipeline.recorder = rec
            if (flash == 2 && !front) pipeline.torch(true)
            recording = true; recordSeconds = 0
            recordJob = viewModelScope.launch {
                while (recording) {
                    delay(1000)
                    recordSeconds++
                    if (recordSeconds >= 60) { stopRecording(); break }
                }
            }
        } catch (e: Exception) {
            recorder = null; recording = false
            say("Video could not start: ${e.message ?: "unknown error"}")
        }
    }

    fun stopRecording() {
        if (!recording) return
        recording = false
        recordJob?.cancel()
        pipeline.recorder = null
        pipeline.torch(false)
        val rec = recorder; val file = recordFile
        recorder = null
        if (rec == null || file == null) return
        busy = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) { rec.stop() }
            if (!file.exists() || file.length() < 2000) {
                file.delete()
                say("The video was too short.")
            } else {
                lastShot = file
                refreshGallery()
                if (saveToPhone) {
                    val ok = withContext(Dispatchers.IO) { Saver.publish(app, file) }
                    say(if (ok) "Video saved ✓ (gallery + Movies/Filtery)" else "Video saved in Filtery")
                } else say("Video saved in the Filtery gallery ✓")
            }
            busy = false
        }
    }

    // ------------------------------------------------------------------ gallery
    fun refreshGallery() {
        val l = Saver.list(app)
        gallery.clear(); gallery.addAll(l)
        if (lastShot == null || !(lastShot?.exists() ?: false)) lastShot = l.firstOrNull()
    }

    fun delete(f: File) {
        f.delete()
        if (viewing == f) viewing = null
        refreshGallery()
    }

    fun say(msg: String) {
        toast = msg
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ toast = null }, 2600)
    }

    // ------------------------------------------------------------------ settings
    fun applyMirror(v: Boolean) { mirror = v; prefs.mirror = v }
    fun applySaveToPhone(v: Boolean) { saveToPhone = v; prefs.saveToPhone = v }
    fun applyPhotoHigh(v: Boolean) { photoHigh = v; prefs.photoHigh = v }
    fun applyVideoSound(v: Boolean) { videoSound = v; prefs.videoSound = v }
    fun applyPreviewQ(v: Int) { previewQ = v; prefs.previewQ = v; pipeline.previewQ = v }
    fun applyAutoUpdate(v: Boolean) { autoUpdate = v; prefs.autoUpdate = v }
    fun applyWifiOnly(v: Boolean) { wifiOnly = v; prefs.wifiOnly = v }

    // ------------------------------------------------------------------ updates (same flow as Obdy: GitHub Releases, downloaded once and kept)
    fun refreshUpdateState() {
        val ready = Updater.ready(app)
        updateFile = ready?.first
        if (ready != null) updateStatus = "Version build ${ready.second} is downloaded and ready."
    }

    fun autoCheckUpdate() {
        if (!autoUpdate) return
        if (System.currentTimeMillis() - prefs.lastCheck < 6 * 3600 * 1000L) return
        checkUpdate(silent = true)
    }

    fun checkUpdate(silent: Boolean = false) {
        if (updateBusy) return
        updateBusy = true
        if (!silent) updateStatus = "Checking GitHub…"
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) { Updater.check(app) }
                prefs.lastCheck = System.currentTimeMillis()
                update = info
                if (info == null) { if (!silent) updateStatus = "You have the newest version." }
                else {
                    updateStatus = "New version: build ${info.code}" + (if (info.notes.isNotBlank()) "\n${info.notes.take(300)}" else "")
                    val canAuto = silent && (!wifiOnly || Updater.onUnmetered(app))
                    if (!silent || canAuto) downloadUpdate(info)
                }
            } catch (e: Exception) {
                if (!silent) updateStatus = e.message ?: "The check failed."
            }
            updateBusy = false
        }
    }

    private suspend fun downloadUpdate(info: UpdateInfo) {
        updateBusy = true; updateProgress = 0
        try {
            val f = withContext(Dispatchers.IO) { Updater.download(app, info) { p -> updateProgress = p } }
            updateFile = f
            updateStatus = "Build ${info.code} is downloaded. Press Install."
        } catch (e: Exception) {
            updateStatus = e.message ?: "The download failed."
        }
        updateBusy = false
    }

    fun installUpdate() {
        val f = updateFile ?: return
        val why = Updater.problem(app, f)
        if (why != null) { updateStatus = why; return }
        if (!Updater.install(app, f)) say("Allow installs from Filtery, then press Install again.")
    }

    override fun onCleared() {
        pipeline.release()
        super.onCleared()
    }
}
