package com.example.filtery.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filtery.Saver
import java.io.File

/** Round tappable button used all over the camera screen. */
@Composable
fun RoundBtn(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 40.dp, content: @Composable () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape).background(Brand.Card).border(1.dp, Brand.Border, CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Brand.Main else androidx.compose.ui.graphics.SolidColor(Brand.Card2))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) androidx.compose.ui.graphics.Color.White else Brand.Dim, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center) }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(Brand.Card2).border(1.dp, Brand.Border, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Brand.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center) }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp).clip(RoundedCornerShape(20.dp)).background(Brand.Card)
            .border(1.dp, Brand.Border, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = Brand.Pink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        content()
    }
}

@Composable
fun SettingSwitch(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Brand.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotBlank()) Text(sub, color = Brand.Dim, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Brand.Pink, checkedThumbColor = androidx.compose.ui.graphics.Color.White, uncheckedTrackColor = Brand.Card2, uncheckedThumbColor = Brand.Dim, uncheckedBorderColor = Brand.Border),
        )
    }
}

/** A row of choices; the chosen one is lit. */
@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Card2).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for ((i, o) in options.withIndex()) {
            val on = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (on) Brand.Main else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent))
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(o, color = if (on) androidx.compose.ui.graphics.Color.White else Brand.Dim, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

/** Small pictures of gallery files, kept in memory so the grid scrolls smoothly. */
object Thumbs {
    private val cache = LruCache<String, Bitmap>(60)

    fun load(f: File, maxLong: Int, keep: Boolean = true): Bitmap? {
        val key = f.path + "|" + maxLong + "|" + f.lastModified()
        if (keep) cache.get(key)?.let { return it }
        val bmp: Bitmap? = try {
            if (Saver.isVideo(f)) {
                val fr = Saver.videoFrame(f)
                if (fr == null) null else {
                    val sc = maxLong.toFloat() / maxOf(fr.width, fr.height)
                    if (sc < 1f) Bitmap.createScaledBitmap(fr, (fr.width * sc).toInt().coerceAtLeast(1), (fr.height * sc).toInt().coerceAtLeast(1), true) else fr
                }
            } else {
                val o = BitmapFactory.Options(); o.inJustDecodeBounds = true
                BitmapFactory.decodeFile(f.absolutePath, o)
                var sample = 1
                while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= maxLong) sample *= 2
                val d = BitmapFactory.Options(); d.inSampleSize = sample
                BitmapFactory.decodeFile(f.absolutePath, d)
            }
        } catch (e: Throwable) { null }
        if (bmp != null && keep) cache.put(key, bmp)
        return bmp
    }
}
