package com.example.filtery.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Playful night-party look: deep indigo, hot pink to orange, with cyan and violet accents. */
object Brand {
    val Bg = Color(0xFF0D0A1E)
    val Card = Color(0xFF1B1540)
    val Card2 = Color(0xFF2A2060)
    val Border = Color(0xFF3B307D)
    val Text = Color(0xFFF8F5FF)
    val Dim = Color(0xFFA9A0D6)
    val Pink = Color(0xFFFF3D8B)
    val Orange = Color(0xFFFFA23D)
    val Cyan = Color(0xFF3DE3FF)
    val Violet = Color(0xFF8B5CFF)
    val Green = Color(0xFF3DDC84)
    val Red = Color(0xFFFF4D4D)

    val Main: Brush get() = Brush.linearGradient(listOf(Pink, Orange))
    val Cool: Brush get() = Brush.linearGradient(listOf(Violet, Cyan))
    val Ring: Brush get() = Brush.sweepGradient(listOf(Pink, Orange, Color(0xFFFFE14D), Green, Cyan, Violet, Pink))
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FilteryTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Brand.Pink, onPrimary = Color.White, secondary = Brand.Cyan,
        background = Brand.Bg, onBackground = Brand.Text, surface = Brand.Card, onSurface = Brand.Text,
        surfaceVariant = Brand.Card2, onSurfaceVariant = Brand.Dim, outline = Brand.Border, error = Brand.Red,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as? Activity)?.window ?: return@SideEffect
            w.statusBarColor = Brand.Bg.toArgb()
            w.navigationBarColor = Brand.Bg.toArgb()
            w.setBackgroundDrawable(ColorDrawable(Brand.Bg.toArgb()))
            WindowCompat.getInsetsController(w, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = scheme, shapes = AppShapes) {
        Surface(modifier = Modifier.fillMaxSize(), color = Brand.Bg, contentColor = Brand.Text, content = content)
    }
}
