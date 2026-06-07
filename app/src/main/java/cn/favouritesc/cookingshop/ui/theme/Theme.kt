package cn.favouritesc.cookingshop.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = OrangeLight,
    onPrimaryContainer = TextPrimary,
    secondary = Secondary,
    onSecondary = TextOnSecondary,
    secondaryContainer = GreenLight,
    onSecondaryContainer = TextPrimary,
    tertiary = Brown80,
    onTertiary = TextPrimary,
    tertiaryContainer = BrownLight,
    onTertiaryContainer = TextPrimary,
    error = Error,
    onError = TextOnPrimary,
    errorContainer = RedLight,
    onErrorContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = OrangeLight,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryVariantDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = Brown80,
    onTertiary = TextPrimary,
    tertiaryContainer = Brown40,
    onTertiaryContainer = TextPrimaryDark,
    error = ErrorDark,
    onError = TextPrimary,
    errorContainer = Red40,
    onErrorContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextSecondaryDark
)

@Composable
fun CookingShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
