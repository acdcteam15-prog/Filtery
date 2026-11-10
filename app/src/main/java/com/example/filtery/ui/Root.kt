package com.example.filtery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.filtery.FilteryVm

@Composable
fun FilteryRoot(vm: FilteryVm, owner: LifecycleOwner, askPermission: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brand.Bg)) {
        when (vm.screen) {
            "gallery" -> GalleryScreen(vm)
            "settings" -> SettingsScreen(vm)
            else -> CameraScreen(vm, owner, askPermission)
        }
        val msg = vm.toast
        if (msg != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp, start = 24.dp, end = 24.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brand.Card2).border(1.dp, Brand.Pink, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
            ) { Text(msg, color = Brand.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
        }
    }
}
