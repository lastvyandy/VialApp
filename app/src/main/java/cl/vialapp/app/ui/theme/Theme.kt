package cl.vialapp.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

val VialActionBlue = Color(0xFF2563EB)
val VialActionGreen = Color(0xFF15803D)

private val LightColors = lightColorScheme(
    primary = VialActionBlue, onPrimary = Color.White,
    secondary = Color(0xFF475569), onSecondary = Color.White,
    primaryContainer = Color(0xFFE8F0FF), onPrimaryContainer = Color(0xFF163A85),
    background = Color(0xFFF4F7FB), onBackground = Color(0xFF172033),
    surface = Color.White, onSurface = Color(0xFF172033),
    surfaceVariant = Color(0xFFE8EDF5), onSurfaceVariant = Color(0xFF526176),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    outline = Color(0xFF64748B), outlineVariant = Color(0xFFD8E1EF),
    error = Color(0xFFB42318), onError = Color.White,
    errorContainer = Color(0xFFFFF1F0), onErrorContainer = Color(0xFF912018)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BB9FF), onPrimary = Color(0xFF10234B),
    secondary = Color(0xFFB5C2D5), onSecondary = Color(0xFF172033),
    primaryContainer = Color(0xFF233B68), onPrimaryContainer = Color(0xFFDCE7FF),
    background = Color(0xFF101827), onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1B2738), onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF263449), onSurfaceVariant = Color(0xFFB7C4D6),
    surfaceContainerHigh = Color(0xFF27364B),
    outline = Color(0xFF8A9AAF), outlineVariant = Color(0xFF3B4A60),
    error = Color(0xFFFCA5A5), onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF47252D), onErrorContainer = Color(0xFFFECACA)
)

internal data class Appearance(val dark: Boolean, val setDark: (Boolean) -> Unit)
internal val LocalVialAppearance = staticCompositionLocalOf { Appearance(false) {} }

/** VialApp owns its appearance instead of inheriting Android's dynamic palette. */
@Composable
fun VialAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vialapp_appearance", Context.MODE_PRIVATE) }
    var dark by remember(prefs) { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    val scheme = if (dark) DarkColors else LightColors
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalVialAppearance provides Appearance(dark) { value ->
        dark = value
        prefs.edit().putBoolean("dark_mode", value).apply()
    }) {
        MaterialTheme(colorScheme = scheme, typography = Typography) {
            Surface(modifier = Modifier.fillMaxSize(), color = scheme.background, contentColor = scheme.onBackground) {
                content()
            }
        }
    }
}
