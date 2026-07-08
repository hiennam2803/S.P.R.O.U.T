package vhdt.sprout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Color0 = Color(0xFF06120A)

private val SproutColorScheme = darkColorScheme(
    primary          = XanhLa,
    onPrimary        = Color0,
    secondary        = XanhDuong,
    background       = NenChinh,
    onBackground     = ChuChinh,
    surface          = NenThe,
    onSurface        = ChuChinh,
    surfaceVariant   = NenTheVien,
    onSurfaceVariant = ChuPhu,
    error            = DoNguyHiem
)

@Composable
fun SproutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SproutColorScheme,
        typography = SproutTypography,
        content = content
    )
}