package com.example.filtery.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.filtery.Cat
import com.example.filtery.Filters
import com.example.filtery.FilteryVm
import com.example.filtery.Fx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen(vm: FilteryVm, owner: LifecycleOwner, askPermission: () -> Unit) {
    LaunchedEffect(vm.camGranted) { if (vm.camGranted) vm.ensureCamera(owner) }

    // The picture fills the screen width (9:16, like a phone screen); the buttons sit on top of it.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        PreviewBox(vm, owner, askPermission, Modifier.fillMaxWidth().aspectRatio(0.5625f).align(Alignment.TopCenter))
        TopBar(vm, Modifier.align(Alignment.TopCenter))
        Controls(vm, owner, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------------------------------------- top bar
@Composable
private fun TopBar(vm: FilteryVm, modifier: Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RoundBtn(onClick = { vm.cycleFlash() }) {
            AppIcon(when (vm.flash) { 1 -> Ic.BoltAuto; 2 -> Ic.Bolt; else -> Ic.BoltOff }, size = 20.dp, color = if (vm.flash == 0) Color.White else Brand.Orange)
        }
        RoundBtn(onClick = { vm.cycleTimer() }) {
            if (vm.timer == 0) AppIcon(Ic.Timer, size = 20.dp) else Text("${vm.timer}s", color = Brand.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        RoundBtn(onClick = { vm.shuffle() }) { AppIcon(Ic.Dice, size = 22.dp, color = Brand.Cyan) }
        Box(Modifier.weight(1f))
        RoundBtn(onClick = { vm.go("settings") }) { AppIcon(Ic.Gear, size = 21.dp) }
    }
}

// ---------------------------------------------------------------------------------------------- live picture
@Composable
private fun PreviewBox(vm: FilteryVm, owner: LifecycleOwner, askPermission: () -> Unit, modifier: Modifier) {
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    Box(
        modifier.background(Color.Black)
            .pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) vm.applyZoom(vm.zoom * zoom) } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = vm.tick                  // reading it here redraws the picture on every new frame
            val b: Bitmap? = vm.frame
            if (b != null && t >= 0) {
                drawIntoCanvas { c -> c.nativeCanvas.drawBitmap(b, null, RectF(0f, 0f, size.width, size.height), paint) }
            }
            if (vm.grid) {
                val lc = Color.White.copy(alpha = 0.45f)
                for (i in 1..2) {
                    drawLine(lc, Offset(size.width * i / 3f, 0f), Offset(size.width * i / 3f, size.height), 1.5f)
                    drawLine(lc, Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), 1.5f)
                }
            }
        }

        if (!vm.camGranted) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🤳", fontSize = 48.sp)
                Text("Filtery needs the camera. Everything stays on your phone.", color = Brand.Text, textAlign = TextAlign.Center, fontSize = 15.sp)
                GradientButton("Allow camera") { askPermission() }
            }
        } else if (vm.cameraError != null) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(vm.cameraError ?: "", color = Brand.Text, textAlign = TextAlign.Center, fontSize = 14.sp)
                GradientButton("Try again") { vm.startCamera(owner) }
            }
        } else if (!vm.hasCamera) {
            Text("Starting the camera…", color = Brand.Dim, modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
        }



        if (vm.fx.face && vm.hasCamera && !vm.faceSeen) {
            Box(Modifier.align(Alignment.Center).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("Show your face 🙂", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (vm.recording) {
            Row(Modifier.align(Alignment.TopCenter).padding(top = 110.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Brand.Red))
                Text(String.format("%d:%02d", vm.recordSeconds / 60, vm.recordSeconds % 60), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (vm.busy) {
            Box(Modifier.align(Alignment.TopStart).padding(top = 70.dp, start = 16.dp)) { CircularProgressIndicator(color = Brand.Pink, strokeWidth = 3.dp, modifier = Modifier.size(24.dp)) }
        }

        // timer number
        Crossfade(targetState = vm.countdown, modifier = Modifier.align(Alignment.Center), label = "countdown") { n ->
            if (n > 0) Text(n.toString(), color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (vm.screenFlash) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.93f)))
    }
}

// ---------------------------------------------------------------------------------------------- filters + shutter
@Composable
private fun Controls(vm: FilteryVm, owner: LifecycleOwner, modifier: Modifier) {
    val scrim = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000), Color(0xE6000000)))
    Column(modifier.fillMaxWidth().background(scrim).padding(top = 10.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // photo / video
        Row(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(2.dp)) {
            for (video in listOf(false, true)) {
                val on = vm.videoMode == video
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) Brand.Main else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                        .clickable { if (!vm.recording) vm.videoMode = video }.padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (video) "VIDEO" else "PHOTO", color = if (on) Color.White else Color(0xCCFFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
        // strength: only for filters where it means something (props are always solid)
        if (vm.fx !== Filters.original && vm.fx.cat != Cat.FACES) {
            Row(Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = vm.intensity, onValueChange = { vm.applyIntensity(it) }, onValueChangeFinished = { vm.commitIntensity() }, valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Brand.Pink, activeTrackColor = Brand.Pink, inactiveTrackColor = Color(0x55FFFFFF)),
                )
                Text("${(vm.intensity * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }
        // categories
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TabPill("★", vm.tab == "fav") { vm.tab = "fav" }
            TabPill("🕒", vm.tab == "recent") { vm.tab = "recent" }
            for (c in Cat.values()) TabPill("${c.icon} ${c.title}", vm.tab == "cat:${c.name}") { vm.tab = "cat:${c.name}" }
        }
        // filters
        val list = vm.visibleFilters()
        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(66.dp), contentAlignment = Alignment.Center) {
                Text(if (vm.tab == "fav") "No favorites yet. Long-press a filter to add it." else "Nothing here yet.", color = Color(0xCCFFFFFF), fontSize = 13.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth().height(60.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                for (f in list) FilterChip(vm, f)
            }
        }
        // buttons
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GalleryThumb(vm)
            Shutter(vm)
            RoundBtn(onClick = { vm.flip(owner) }, size = 52.dp) { AppIcon(Ic.Flip, size = 26.dp) }
        }
    }
}

@Composable
private fun TabPill(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(if (on) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.3f))
            .border(1.dp, if (on) Brand.Pink else Color.White.copy(alpha = 0.15f), RoundedCornerShape(13.dp)).clickable { onClick() }.padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (on) Color.White else Color(0xCCFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun FilterChip(vm: FilteryVm, f: Fx) {
    val sel = vm.fx.id == f.id
    val sc by animateFloatAsState(if (sel) 1.06f else 1f, tween(160), label = "chip")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.width(64.dp).height(60.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(shape).background(if (sel) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.4f))
            .border(if (sel) 2.dp else 1.dp, if (sel) Brand.Main else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.18f)), shape)
            .pointerInput(f.id) { detectTapGestures(onTap = { vm.pick(f) }, onLongPress = { vm.toggleFavorite(f) }) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.icon, fontSize = 22.sp)
            Text(f.name, color = if (sel) Color.White else Color(0xCCFFFFFF), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 3.dp))
        }
        if (vm.isFavorite(f)) Text("★", color = Brand.Orange, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp))
    }
}

@Composable
private fun GalleryThumb(vm: FilteryVm) {
    val shot = vm.lastShot
    val bmp by produceState<Bitmap?>(null, shot, vm.gallery.size) {
        value = if (shot == null) null else withContext(Dispatchers.IO) { Thumbs.load(shot, 160) }
    }
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).clickable { vm.go("gallery") },
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), contentDescription = "Gallery", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else AppIcon(Ic.Gallery, size = 22.dp, color = Brand.Dim)
    }
}

@Composable
private fun Shutter(vm: FilteryVm) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, tween(90), label = "shutter")
    val prog = if (vm.recording) vm.recordSeconds / 60f else 0f
    Box(
        Modifier.size(72.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clickable(interactionSource = src, indication = null) { vm.shutter() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            drawCircle(brush = Brand.Ring, radius = size.minDimension / 2f - w / 2f, style = Stroke(width = w))
            if (vm.recording) {
                drawArc(color = Color.White, startAngle = -90f, sweepAngle = 360f * prog, useCenter = false, style = Stroke(width = w, cap = StrokeCap.Round),
                    topLeft = Offset(w / 2f, w / 2f), size = Size(size.width - w, size.height - w))
            }
            if (vm.recording) {
                val s = size.minDimension * 0.34f
                drawRoundRect(color = Brand.Red, topLeft = Offset((size.width - s) / 2f, (size.height - s) / 2f), size = Size(s, s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.22f, s * 0.22f))
            } else {
                drawCircle(color = if (vm.videoMode) Brand.Red else Color.White, radius = size.minDimension / 2f - w * 2.2f)
            }
        }
        if (vm.countdown > 0) Text("✕", color = Brand.Dim, fontSize = 20.sp)
    }
}

@Composable
internal fun CaptionDialog(vm: FilteryVm, onClose: () -> Unit) {
    var text by remember { mutableStateOf(vm.caption) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Brand.Card,
        title = { Text("Caption", color = Brand.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Text shown at the bottom of your photos and videos.", color = Brand.Dim, fontSize = 13.sp)
                OutlinedTextField(value = text, onValueChange = { text = it.take(40) }, singleLine = true, placeholder = { Text("e.g. Best day ever!") })
            }
        },
        confirmButton = { TextButton(onClick = { vm.applyCaption(text.trim()); onClose() }) { Text("OK", color = Brand.Pink, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { vm.applyCaption(""); onClose() }) { Text("Clear", color = Brand.Dim) } },
    )
}
