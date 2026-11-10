package com.example.filtery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filtery.BuildConfig
import com.example.filtery.Filters
import com.example.filtery.FilteryVm

@Composable
fun SettingsScreen(vm: FilteryVm) {
    BackHandler { vm.go("camera") }
    Column(Modifier.fillMaxSize().background(Brand.Bg)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundBtn(onClick = { vm.go("camera") }) { AppIcon(Ic.Back, size = 22.dp) }
            Text("Settings", color = Brand.Text, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Section("CAMERA") {
                SettingSwitch("Mirror selfies", "Front-camera photos look like the preview (like a mirror).", vm.mirror) { vm.setMirror(it) }
                Text("Photo quality", color = Brand.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Seg(listOf("Standard (~3 MP)", "High (up to 12 MP)"), if (vm.photoHigh) 1 else 0) { vm.setPhotoHigh(it == 1) }
                Text("Live preview quality", color = Brand.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Seg(listOf("Fast", "Balanced", "Sharp"), vm.previewQ) { vm.setPreviewQ(it) }
                Text("Use Fast if the live preview stutters on a heavy filter. It also sets the video size.", color = Brand.Dim, fontSize = 12.sp)
            }
            Section("SAVING") {
                SettingSwitch("Copy to the phone gallery", "Photos go to Pictures/Filtery and videos to Movies/Filtery. They always stay in Filtery's own gallery too.", vm.saveToPhone) { vm.setSaveToPhone(it) }
                SettingSwitch("Record sound in videos", "Uses the microphone while you record.", vm.videoSound) { vm.setVideoSound(it) }
            }
            Section("UPDATES") {
                Text("Updates come from GitHub: ${BuildConfig.UPDATE_REPO}", color = Brand.Dim, fontSize = 12.sp)
                if (vm.updateStatus.isNotBlank()) Text(vm.updateStatus, color = Brand.Text, fontSize = 14.sp)
                if (vm.updateBusy && vm.updateProgress in 1..99) Text("Downloading… ${vm.updateProgress}%", color = Brand.Cyan, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("Check for updates", Modifier.weight(1f)) { vm.checkUpdate() }
                    GradientButton("Install", Modifier.weight(1f), enabled = vm.updateReady) { vm.installUpdate() }
                }
                SettingSwitch("Check automatically", "Looks for a new version when you open Filtery (at most every 6 hours).", vm.autoUpdate) { vm.setAutoUpdate(it) }
                SettingSwitch("Download on Wi-Fi only", "A new version is downloaded once and kept until you press Install.", vm.wifiOnly) { vm.setWifiOnly(it) }
            }
            Section("ABOUT") {
                Text("Filtery ${BuildConfig.VERSION_LABEL}", color = Brand.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${Filters.count} filters that all work on your phone with no internet: face filters (hats, glasses, animals), funny warps, effects, colors, art and frames. Faces are found on the phone too. The internet is used only to check for updates.", color = Brand.Dim, fontSize = 13.sp)
            }
        }
    }
}
