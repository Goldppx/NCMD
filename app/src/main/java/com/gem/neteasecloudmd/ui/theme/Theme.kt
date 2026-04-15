package com.gem.neteasecloudmd.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCFBDFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    surfaceContainerLow = Color(0xFF211F26),
    surfaceContainer = Color(0xFF252329),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

@Composable
fun NeteaseCloudMDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedArgb: Int? = null,
    content: @Composable () -> Unit
) {
    val targetColorScheme = when {
        seedArgb != null -> {
            dynamicColorScheme(
                seedColor = Color(seedArgb),
                isDark = darkTheme,
                contrastLevel = Contrast.Default.value
            )
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = animateColorScheme(targetColorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 550)

    val primary = animateColorAsState(target.primary, animationSpec = animationSpec, label = "primary").value
    val onPrimary = animateColorAsState(target.onPrimary, animationSpec = animationSpec, label = "onPrimary").value
    val primaryContainer = animateColorAsState(target.primaryContainer, animationSpec = animationSpec, label = "primaryContainer").value
    val onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animationSpec = animationSpec, label = "onPrimaryContainer").value
    val inversePrimary = animateColorAsState(target.inversePrimary, animationSpec = animationSpec, label = "inversePrimary").value

    val secondary = animateColorAsState(target.secondary, animationSpec = animationSpec, label = "secondary").value
    val onSecondary = animateColorAsState(target.onSecondary, animationSpec = animationSpec, label = "onSecondary").value
    val secondaryContainer = animateColorAsState(target.secondaryContainer, animationSpec = animationSpec, label = "secondaryContainer").value
    val onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, animationSpec = animationSpec, label = "onSecondaryContainer").value

    val tertiary = animateColorAsState(target.tertiary, animationSpec = animationSpec, label = "tertiary").value
    val onTertiary = animateColorAsState(target.onTertiary, animationSpec = animationSpec, label = "onTertiary").value
    val tertiaryContainer = animateColorAsState(target.tertiaryContainer, animationSpec = animationSpec, label = "tertiaryContainer").value
    val onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, animationSpec = animationSpec, label = "onTertiaryContainer").value

    val background = animateColorAsState(target.background, animationSpec = animationSpec, label = "background").value
    val onBackground = animateColorAsState(target.onBackground, animationSpec = animationSpec, label = "onBackground").value
    val surface = animateColorAsState(target.surface, animationSpec = animationSpec, label = "surface").value
    val onSurface = animateColorAsState(target.onSurface, animationSpec = animationSpec, label = "onSurface").value
    val surfaceVariant = animateColorAsState(target.surfaceVariant, animationSpec = animationSpec, label = "surfaceVariant").value
    val onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animationSpec = animationSpec, label = "onSurfaceVariant").value
    val surfaceTint = animateColorAsState(target.surfaceTint, animationSpec = animationSpec, label = "surfaceTint").value

    val inverseSurface = animateColorAsState(target.inverseSurface, animationSpec = animationSpec, label = "inverseSurface").value
    val inverseOnSurface = animateColorAsState(target.inverseOnSurface, animationSpec = animationSpec, label = "inverseOnSurface").value

    val error = animateColorAsState(target.error, animationSpec = animationSpec, label = "error").value
    val onError = animateColorAsState(target.onError, animationSpec = animationSpec, label = "onError").value
    val errorContainer = animateColorAsState(target.errorContainer, animationSpec = animationSpec, label = "errorContainer").value
    val onErrorContainer = animateColorAsState(target.onErrorContainer, animationSpec = animationSpec, label = "onErrorContainer").value

    val outline = animateColorAsState(target.outline, animationSpec = animationSpec, label = "outline").value
    val outlineVariant = animateColorAsState(target.outlineVariant, animationSpec = animationSpec, label = "outlineVariant").value
    val scrim = animateColorAsState(target.scrim, animationSpec = animationSpec, label = "scrim").value

    val surfaceBright = animateColorAsState(target.surfaceBright, animationSpec = animationSpec, label = "surfaceBright").value
    val surfaceDim = animateColorAsState(target.surfaceDim, animationSpec = animationSpec, label = "surfaceDim").value
    val surfaceContainer = animateColorAsState(target.surfaceContainer, animationSpec = animationSpec, label = "surfaceContainer").value
    val surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, animationSpec = animationSpec, label = "surfaceContainerHigh").value
    val surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, animationSpec = animationSpec, label = "surfaceContainerHighest").value
    val surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, animationSpec = animationSpec, label = "surfaceContainerLow").value
    val surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, animationSpec = animationSpec, label = "surfaceContainerLowest").value

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest
    )
}
