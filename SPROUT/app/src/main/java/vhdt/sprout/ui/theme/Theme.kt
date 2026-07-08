package vhdt.sprout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = XanhLa,
    onPrimary        = Color.White,
    secondary        = XanhDuong,
    onSecondary      = Color.White,
    background       = NenChinh,
    onBackground     = ChuChinh,
    surface          = NenThe,
    onSurface        = ChuChinh,
    surfaceVariant   = NenTheVien,
    onSurfaceVariant = ChuPhu,
    error            = DoNguyHiem,
    onError          = Color.White
)

@Composable
fun SproutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,  // Luôn dùng light theme
        typography = SproutTypography,
        content = content
    )
}