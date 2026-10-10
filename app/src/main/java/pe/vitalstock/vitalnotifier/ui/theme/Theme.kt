package pe.vitalstock.vitalnotifier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0D9488)
val TealDark = Color(0xFF115E59)
val TealContainer = Color(0xFFCCFBF1)
val OnTealContainer = Color(0xFF134E4A)

val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenBg = Color(0xFFDCFCE7)
val ErrorRed = Color(0xFFDC2626)
val ErrorRedBg = Color(0xFFFEE2E2)
val WarningOrange = Color(0xFFEA580C)
val WarningOrangeBg = Color(0xFFFFEDD5)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2C),
    primaryContainer = TealDark,
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFF87171)
)

@Composable
fun VitalNotifierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
