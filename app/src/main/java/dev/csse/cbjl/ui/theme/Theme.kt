package dev.csse.cbjl.slo_n_study.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeMocha,
    secondary = WarmGray,

    background = Color(0xFF1F2126),   // softer dark background
    surface = Color(0xFF2A2C33),      // card color

    onPrimary = Color.White,
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6)
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeMocha,
    secondary = WarmGray,

    background = CoffeeCream,
    surface = Color.White,

    onPrimary = Color.White,
    onBackground = CoffeeMocha,
    onSurface = CoffeeMocha
)

@Composable
fun Slo_n_studyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}