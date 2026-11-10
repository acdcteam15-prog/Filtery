package com.example.filtery

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.filtery.ui.FilteryRoot
import com.example.filtery.ui.FilteryTheme

class MainActivity : AppCompatActivity() {
    private val vm: FilteryVm by viewModels()

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.camGranted = has(Manifest.permission.CAMERA)
    }

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun askPermissions() {
        val want = ArrayList<String>()
        if (!has(Manifest.permission.CAMERA)) want.add(Manifest.permission.CAMERA)
        if (!has(Manifest.permission.RECORD_AUDIO)) want.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT < 29 && !has(Manifest.permission.WRITE_EXTERNAL_STORAGE)) want.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (want.isEmpty()) vm.camGranted = true else permissions.launch(want.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vm.camGranted = has(Manifest.permission.CAMERA)
        setContent {
            FilteryTheme {
                FilteryRoot(vm, this, ::askPermissions)
            }
        }
        if (savedInstanceState == null) {
            if (!vm.camGranted) askPermissions()
            vm.autoCheckUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.camGranted = has(Manifest.permission.CAMERA)
        vm.refreshUpdateState()
    }
}
