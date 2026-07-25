package ac.mdiq.podcini.ui.compose

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.storage.database.appPrefs
import ac.mdiq.podcini.storage.database.upsertBlk
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

private const val TAG = "AppTheme"

val CustomTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    // Add other text styles as needed
)

object CustomTextStyles {
    // Reactive: picks up the fork's chosen font family / scale / weight live. Reading the KuchusenUi
    // state inside this getter makes every call site that uses it during composition recompose on change.
    val titleCustom: TextStyle
        get() = TextStyle(
            fontSize = 18.sp * KuchusenUi.fontScale,
            fontWeight = if (KuchusenUi.fontWeight > 0) FontWeight(KuchusenUi.fontWeight) else FontWeight.Medium,
            fontFamily = KuchusenUi.fontFamily)
}

// App-wide accent border colour & thickness, driven by the 白い熊 空中線 UI page.
val borderColor: Color
    get() = KuchusenUi.borderColor

val borderWidthDp: Dp
    get() = KuchusenUi.borderWidth.dp

val textColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val buttonColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.tertiary

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColors = lightColorScheme().copy(
    tertiary = Color(0xFF4E3511),
    tertiaryContainer = Color(0xFFB6EEEE),
    surface = Color(0xFFFDFCF0),
    onSurface = Color(0xFF2D2E30)
)
private val DarkColors = darkColorScheme().copy(
    tertiary = Color(0xFFE9A43E),
    tertiaryContainer = Color(0xFF0D343E),
    onSurface = Color(0xFFE0D7C1),
)

enum class AppThemes {
    LIGHT, DARK, BLACK, SYSTEM
}

var appTheme: AppThemes
    get() = when (appPrefs.theme) {
        "0" -> AppThemes.LIGHT
        "1" -> AppThemes.DARK
        else -> AppThemes.SYSTEM
    }
    set(theme) {
        val t = when (theme) {
            AppThemes.LIGHT -> "0"
            AppThemes.DARK -> "1"
            else -> "system"
        }
        upsertBlk(appPrefs) { it.theme = t }
    }

private fun TextUnit.scaled(scale: Float): TextUnit = if (isSpecified) this * scale else this

private fun TextStyle.themed(family: FontFamily, scale: Float, weight: Int): TextStyle {
    var s = copy(fontFamily = family)
    if (scale != 1f) s = s.copy(fontSize = s.fontSize.scaled(scale), lineHeight = s.lineHeight.scaled(scale))
    if (weight > 0) s = s.copy(fontWeight = FontWeight(weight))
    return s
}

// Rebuild the whole Material type scale with the fork's font family / size scale / weight applied.
private fun kuchusenTypography(family: FontFamily, scale: Float, weight: Int): Typography {
    val b = Typography()
    return b.copy(
        displayLarge = b.displayLarge.themed(family, scale, weight),
        displayMedium = b.displayMedium.themed(family, scale, weight),
        displaySmall = b.displaySmall.themed(family, scale, weight),
        headlineLarge = b.headlineLarge.themed(family, scale, weight),
        headlineMedium = b.headlineMedium.themed(family, scale, weight),
        headlineSmall = b.headlineSmall.themed(family, scale, weight),
        titleLarge = b.titleLarge.themed(family, scale, weight),
        titleMedium = b.titleMedium.themed(family, scale, weight),
        titleSmall = b.titleSmall.themed(family, scale, weight),
        bodyLarge = b.bodyLarge.themed(family, scale, weight),
        bodyMedium = b.bodyMedium.themed(family, scale, weight),
        bodySmall = b.bodySmall.themed(family, scale, weight),
        labelLarge = b.labelLarge.themed(family, scale, weight),
        labelMedium = b.labelMedium.themed(family, scale, weight),
        labelSmall = b.labelSmall.themed(family, scale, weight),
    )
}

// Corner radii scaled from a single roundness value (default 12 reproduces upstream's 4/8/12/16/28).
private fun kuchusenShapes(r: Int): Shapes {
    fun rc(v: Float) = RoundedCornerShape(v.coerceAtLeast(0f).dp)
    return Shapes(
        extraSmall = rc(r / 3f),
        small = rc(r * 2 / 3f),
        medium = rc(r.toFloat()),
        large = rc(r * 4 / 3f),
        extraLarge = rc(r * 7 / 3f),
    )
}

@Composable
fun PodciniTheme(forceTheme: AppThemes? = null, content: @Composable () -> Unit) {
    val appThemes: AppThemes = if (forceTheme != null) forceTheme else appTheme
    val isDark = when (appThemes) {
        AppThemes.LIGHT -> false
        AppThemes.DARK, AppThemes.BLACK -> true
        AppThemes.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appPrefs.useDynamicThemes -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark && (appThemes == AppThemes.BLACK || appPrefs.themeBlack) -> DarkColors.copy(surface = Color(0xFF000000))
        isDark -> DarkColors
        else -> LightColors
    }
    // Lay the 白い熊 空中線 house colours over whatever base scheme was chosen. This is the single source
    // of truth for background / text / accent, so the app reads black-yellow by default and live-updates
    // the instant any value is changed on the UI page.
    val themedScheme = colorScheme.copy(
        surface = KuchusenUi.backgroundColor,
        background = KuchusenUi.backgroundColor,
        onSurface = KuchusenUi.textColor,
        onBackground = KuchusenUi.textColor,
        onSurfaceVariant = KuchusenUi.secondaryTextColor,
        primary = KuchusenUi.accentColor,
        secondary = KuchusenUi.accentColor,
        tertiary = KuchusenUi.accentColor,
        // Menus and dialogs draw on the surfaceContainer* roles, which the copy() above does not
        // touch — without these they keep Material's elevated grey instead of the house background.
        surfaceContainerLowest = KuchusenUi.backgroundColor,
        surfaceContainerLow = KuchusenUi.backgroundColor,
        surfaceContainer = KuchusenUi.backgroundColor,
        surfaceContainerHigh = KuchusenUi.backgroundColor,
        surfaceContainerHighest = KuchusenUi.backgroundColor,
    )
    MaterialTheme(
        colorScheme = themedScheme,
        typography = kuchusenTypography(KuchusenUi.fontFamily, KuchusenUi.fontScale, KuchusenUi.fontWeight),
        shapes = kuchusenShapes(KuchusenUi.cornerRoundness),
        content = content)
}

fun isLightTheme(): Boolean {
    val curTheme: AppThemes = appTheme
    return when (curTheme) {
        AppThemes.LIGHT -> true
        AppThemes.DARK, AppThemes.BLACK -> false
        AppThemes.SYSTEM -> {
            val uiMode = getAppContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            uiMode != Configuration.UI_MODE_NIGHT_YES
        }
    }
}

fun distinctColorOf(colorA: Color, colorB: Color): Color {
    val hslA = FloatArray(3)
    val hslB = FloatArray(3)
    val argbA = colorA.toArgb()
    val argbB = colorB.toArgb()

    ColorUtils.colorToHSL(argbA, hslA)
    ColorUtils.colorToHSL(argbB, hslB)

    val avgHue = (hslA[0] + hslB[0]) / 2f
    val avgSaturation = (hslA[1] + hslB[1]) / 2f
//    val avgLightness = (hslA[2] + hslB[2]) / 2f

    val targetHue = (avgHue + 180f) % 360f
    val targetSaturation = (1.0f - avgSaturation).coerceIn(0.3f, 0.9f)

    val lumA = ColorUtils.calculateLuminance(argbA)
    val lumB = ColorUtils.calculateLuminance(argbB)
    val avgLuminance = (lumA + lumB) / 2.0

    var currentLightness = if (avgLuminance < 0.5) 0.9f else 0.1f

    val targetColorHSL = floatArrayOf(targetHue, targetSaturation, currentLightness)
    var resultColor = ColorUtils.HSLToColor(targetColorHSL)

    val minContrast = 4.5f
    var attempts = 0

    while (attempts < 10) {
        val contrastA = ColorUtils.calculateContrast(resultColor, argbA)
        val contrastB = ColorUtils.calculateContrast(resultColor, argbB)
        if (contrastA >= minContrast && contrastB >= minContrast) break

        currentLightness = if (avgLuminance < 0.5) (currentLightness + 0.05f).coerceAtMost(1.0f) else (currentLightness - 0.05f).coerceAtLeast(0.0f)

        targetColorHSL[2] = currentLightness
        resultColor = ColorUtils.HSLToColor(targetColorHSL)
        attempts++
    }
    return Color(resultColor)
}

fun complementaryColorOf(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + 180f) % 360f
    hsv[1] = if (hsv[1] < 0.5f) 0.7f else hsv[1]
    hsv[2] = if (hsv[2] > 0.5f) 0.2f else 0.9f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun contrastColorOf(color: Color): Color {
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}
