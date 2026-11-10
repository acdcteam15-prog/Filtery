package com.example.filtery.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.filtery.FilteryVm
import com.example.filtery.Saver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GalleryScreen(vm: FilteryVm) {
    BackHandler { if (vm.viewing != null) vm.viewing = null else vm.go("camera") }
    val open = vm.viewing
    if (open != null) Viewer(vm, open) else Grid(vm)
}

@Composable
private fun Grid(vm: FilteryVm) {
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundBtn(onClick = { vm.go("camera") }) { AppIcon(Ic.Back, size = 22.dp) }
            Text("My Filtery photos", color = Brand.Text, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, modifier = Modifier.weight(1f))
            Text("${vm.gallery.size}", color = Brand.Dim, fontSize = 14.sp)
        }
        if (vm.gallery.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📸", fontSize = 56.sp)
                    Text("No photos yet. Go make something funny!", color = Brand.Dim, fontSize = 15.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(vm.gallery, key = { it.absolutePath }) { f -> Cell(f) { vm.viewing = f } }
            }
        }
    }
}

@Composable
private fun Cell(f: File, onClick: () -> Unit) {
    val bmp by produceState<Bitmap?>(null, f) { value = withContext(Dispatchers.IO) { Thumbs.load(f, 360) } }
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Card).clickable { onClick() }, contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (Saver.isVideo(f)) Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) { AppIcon(Ic.Play, size = 18.dp, color = Color.White) }
    }
}

@Composable
private fun Viewer(vm: FilteryVm, f: File) {
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val bmp by produceState<Bitmap?>(null, f) { value = withContext(Dispatchers.IO) { Thumbs.load(f, 1600, keep = false) } }
    val idx = vm.gallery.indexOf(f)
    val video = Saver.isVideo(f)

    fun uri() = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBtn(onClick = { vm.viewing = null }) { AppIcon(Ic.Back, size = 22.dp) }
            Text(if (idx >= 0) "${idx + 1} / ${vm.gallery.size}" else "", color = Brand.Dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
            RoundBtn(onClick = {
                try {
                    val i = Intent(Intent.ACTION_SEND).setType(if (video) "video/mp4" else "image/jpeg")
                        .putExtra(Intent.EXTRA_STREAM, uri()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) { vm.say("Could not share this file.") }
            }) { AppIcon(Ic.Share, size = 20.dp) }
            RoundBtn(onClick = { confirm = true }) { AppIcon(Ic.Trash, size = 20.dp, color = Brand.Red) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val b = bmp
            if (b != null) Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            if (video) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(Color.Black.copy(alpha = 0.6f)).border(2.dp, Color.White, RoundedCornerShape(36.dp)).clickable {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri(), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) { vm.say("No video player found.") }
                    },
                    contentAlignment = Alignment.Center,
                ) { AppIcon(Ic.Play, size = 34.dp, color = Color.White) }
            }
        }
        Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(onClick = { if (idx > 0) vm.viewing = vm.gallery[idx - 1] }, size = 48.dp) { AppIcon(Ic.Back, size = 22.dp, color = if (idx > 0) Brand.Text else Brand.Border) }
            Text(if (vm.saveToPhone) "Also in Pictures/Filtery" else "Only in Filtery", color = Brand.Dim, fontSize = 12.sp)
            RoundBtn(onClick = { if (idx >= 0 && idx < vm.gallery.size - 1) vm.viewing = vm.gallery[idx + 1] }, size = 48.dp) {
                AppIcon(Ic.Back, size = 22.dp, color = if (idx >= 0 && idx < vm.gallery.size - 1) Brand.Text else Brand.Border, modifier = Modifier.graphicsLayer { rotationZ = 180f })
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, containerColor = Brand.Card,
            title = { Text("Delete this ${if (video) "video" else "photo"}?", color = Brand.Text, fontWeight = FontWeight.Bold) },
            text = { Text("It is removed from Filtery. A copy saved in the phone gallery stays there.", color = Brand.Dim, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { confirm = false; vm.delete(f) }) { Text("Delete", color = Brand.Red, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep", color = Brand.Dim) } },
        )
    }
}
