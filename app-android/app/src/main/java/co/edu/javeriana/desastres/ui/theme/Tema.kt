package co.edu.javeriana.desastres.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores por nivel de afectación; los mismos del visor web. */
fun colorNivel(nivel: Int): Color = when (nivel) {
    1 -> Color(0xFF2E7D32)
    2 -> Color(0xFF9E9D24)
    3 -> Color(0xFFF9A825)
    4 -> Color(0xFFEF6C00)
    else -> Color(0xFFC62828)
}

private val Claro = lightColorScheme(
    primary = Color(0xFFB71C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775651),
    tertiary = Color(0xFF2E7D32),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFF930005),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB6),
    tertiary = Color(0xFF81C784),
)

@Composable
fun TemaDesastres(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = content)
}
