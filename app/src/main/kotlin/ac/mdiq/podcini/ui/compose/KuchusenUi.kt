package ac.mdiq.podcini.ui.compose

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.R
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.edit
import java.io.File

/**
 * 白い熊 空中線 — the fork's own live UI/theming store.
 *
 * Backed by a dedicated SharedPreferences file (so we never touch the upstream Realm schema) and
 * exposed through Compose [androidx.compose.runtime.MutableState] so that every read inside a
 * composable — including inside [PodciniTheme] — recomposes the moment a value changes. That is what
 * gives the settings screen its instant, whole-app live preview.
 *
 * The defaults are the house style: black background, pure-yellow text and accents/border. Because
 * the defaults are baked into every `get*` fallback, a fresh install is black-yellow out of the box
 * with no explicit seeding step.
 */

// House palette.
val PALETTE_BLACK = Color(0xFF000000)
val PALETTE_YELLOW = Color(0xFFFFFF00)
val PALETTE_YELLOW_DIM = Color(0xFFB3B300)

// Font family sentinels (anything else is a file name inside the fonts dir).
const val FONT_SYSTEM = ""
const val FONT_MONOSPACE = "@monospace"
private val FONT_EXTENSIONS = setOf("ttf", "otf")

const val MAX_RECENT_COLORS = 8

data class FontOption(val displayName: String, val fileName: String)

object KuchusenUi {
    private const val PREFS_NAME = "kuchusen_ui"

    private const val K_BG = "background_color"
    private const val K_TEXT = "text_color"
    private const val K_TEXT2 = "secondary_text_color"
    private const val K_ACCENT = "accent_color"
    private const val K_BORDER = "border_color"
    private const val K_FONT = "font_family"
    private const val K_FONT_SCALE = "font_scale"
    private const val K_FONT_WEIGHT = "font_weight"
    private const val K_ROUNDNESS = "corner_roundness"
    private const val K_BORDER_WIDTH = "border_width"
    private const val K_RECENTS = "recent_colors"

    // Default house values — also the seed, via the get* fallbacks below.
    private val DEF_BG = PALETTE_BLACK
    private val DEF_TEXT = PALETTE_YELLOW
    private val DEF_TEXT2 = PALETTE_YELLOW_DIM
    private val DEF_ACCENT = PALETTE_YELLOW
    private val DEF_BORDER = PALETTE_YELLOW
    private const val DEF_FONT_SCALE = 1.0f
    private const val DEF_FONT_WEIGHT = 0          // 0 == use the style's default weight
    private const val DEF_ROUNDNESS = 12           // dp; matches upstream's medium shape
    private const val DEF_BORDER_WIDTH = 1         // dp

    private val prefs: SharedPreferences by lazy { getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private var bgState by mutableStateOf(DEF_BG)
    private var textState by mutableStateOf(DEF_TEXT)
    private var text2State by mutableStateOf(DEF_TEXT2)
    private var accentState by mutableStateOf(DEF_ACCENT)
    private var borderState by mutableStateOf(DEF_BORDER)
    private var fontFileState by mutableStateOf(FONT_SYSTEM)
    private var fontScaleState by mutableStateOf(DEF_FONT_SCALE)
    private var fontWeightState by mutableStateOf(DEF_FONT_WEIGHT)
    private var roundnessState by mutableStateOf(DEF_ROUNDNESS)
    private var borderWidthState by mutableStateOf(DEF_BORDER_WIDTH)
    private var recentsState by mutableStateOf<List<Int>>(emptyList())

    init {
        reloadFromPrefs()
    }

    /** (Re-)read every Compose state from the backing prefs — also called after a settings import. */
    fun reloadFromPrefs() {
        bgState = Color(prefs.getInt(K_BG, DEF_BG.toArgb()))
        textState = Color(prefs.getInt(K_TEXT, DEF_TEXT.toArgb()))
        text2State = Color(prefs.getInt(K_TEXT2, DEF_TEXT2.toArgb()))
        accentState = Color(prefs.getInt(K_ACCENT, DEF_ACCENT.toArgb()))
        borderState = Color(prefs.getInt(K_BORDER, DEF_BORDER.toArgb()))
        fontFileState = prefs.getString(K_FONT, FONT_SYSTEM) ?: FONT_SYSTEM
        fontScaleState = prefs.getFloat(K_FONT_SCALE, DEF_FONT_SCALE)
        fontWeightState = prefs.getInt(K_FONT_WEIGHT, DEF_FONT_WEIGHT)
        roundnessState = prefs.getInt(K_ROUNDNESS, DEF_ROUNDNESS)
        borderWidthState = prefs.getInt(K_BORDER_WIDTH, DEF_BORDER_WIDTH)
        recentsState = (prefs.getString(K_RECENTS, "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
    }

    // ---- Settings export/import hooks (KuchusenExport) ------------------------------------------

    /** Snapshot of every stored UI pref, keyed as in the prefs file. */
    fun prefsSnapshot(): Map<String, Any?> = prefs.all

    /** Merge typed values back into the prefs store (never clears), then refresh the live states. */
    fun importPrefValues(values: Map<String, Any>) {
        prefs.edit {
            for ((key, v) in values) {
                when (v) {
                    is Boolean -> putBoolean(key, v)
                    is Int -> putInt(key, v)
                    is Long -> putLong(key, v)
                    is Float -> putFloat(key, v)
                    is String -> putString(key, v)
                }
            }
        }
        reloadFromPrefs()
    }

    /** Every imported font file on disk (for settings export). */
    fun fontFiles(): List<File> =
        fontsDir().listFiles()?.filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS } ?: emptyList()

    /** Write a font file back from a settings import (basename only — no path traversal). */
    fun writeFontFile(name: String, bytes: ByteArray) {
        val safe = File(name).name
        if (safe.substringAfterLast('.', "").lowercase() !in FONT_EXTENSIONS) return
        File(fontsDir(), safe).writeBytes(bytes)
        familyCache.remove(safe)
    }

    var backgroundColor: Color
        get() = bgState
        set(v) { bgState = v; prefs.edit { putInt(K_BG, v.toArgb()) } }

    var textColor: Color
        get() = textState
        set(v) { textState = v; prefs.edit { putInt(K_TEXT, v.toArgb()) } }

    var secondaryTextColor: Color
        get() = text2State
        set(v) { text2State = v; prefs.edit { putInt(K_TEXT2, v.toArgb()) } }

    var accentColor: Color
        get() = accentState
        set(v) { accentState = v; prefs.edit { putInt(K_ACCENT, v.toArgb()) } }

    var borderColor: Color
        get() = borderState
        set(v) { borderState = v; prefs.edit { putInt(K_BORDER, v.toArgb()) } }

    var fontFamilyFile: String
        get() = fontFileState
        set(v) { fontFileState = v; prefs.edit { putString(K_FONT, v) } }

    /** Multiplier on every text size, 0.5..2.0. */
    var fontScale: Float
        get() = fontScaleState
        set(v) { fontScaleState = v; prefs.edit { putFloat(K_FONT_SCALE, v) } }

    /** 0 = leave each style's own weight; otherwise 100..900. */
    var fontWeight: Int
        get() = fontWeightState
        set(v) { fontWeightState = v; prefs.edit { putInt(K_FONT_WEIGHT, v) } }

    /** Corner radius base in dp (0 = fully square). */
    var cornerRoundness: Int
        get() = roundnessState
        set(v) { roundnessState = v; prefs.edit { putInt(K_ROUNDNESS, v) } }

    /** Border thickness in dp (0 = no border). */
    var borderWidth: Int
        get() = borderWidthState
        set(v) { borderWidthState = v; prefs.edit { putInt(K_BORDER_WIDTH, v) } }

    val recentColors: List<Int> get() = recentsState

    fun addRecentColor(argb: Int) {
        val next = (listOf(argb) + recentsState.filter { it != argb }).take(MAX_RECENT_COLORS)
        recentsState = next
        prefs.edit { putString(K_RECENTS, next.joinToString(",")) }
    }

    /** Reset every attribute back to the black-yellow house style. */
    fun resetToHouseStyle() {
        backgroundColor = DEF_BG
        textColor = DEF_TEXT
        secondaryTextColor = DEF_TEXT2
        accentColor = DEF_ACCENT
        borderColor = DEF_BORDER
        fontFamilyFile = FONT_SYSTEM
        fontScale = DEF_FONT_SCALE
        fontWeight = DEF_FONT_WEIGHT
        cornerRoundness = DEF_ROUNDNESS
        borderWidth = DEF_BORDER_WIDTH
    }

    // ---- External fonts -------------------------------------------------------------------------

    private fun fontsDir(): File = File(getAppContext().filesDir, "fonts").apply { if (!exists()) mkdirs() }

    /** System default + monospace + every imported .ttf/.otf, sorted by name. */
    fun availableFonts(): List<FontOption> {
        val ctx = getAppContext()
        val options = mutableListOf(
            FontOption(ctx.getString(R.string.kuchusen_font_system), FONT_SYSTEM),
            FontOption(ctx.getString(R.string.kuchusen_font_monospace), FONT_MONOSPACE),
        )
        fontsDir().listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS }
            ?.sortedBy { it.name.lowercase() }
            ?.forEach { options.add(FontOption(it.nameWithoutExtension, it.name)) }
        return options
    }

    private val familyCache = HashMap<String, FontFamily>()

    /** A Compose [FontFamily] for a font file name (or the system/monospace sentinels). */
    fun fontFamilyFor(fileName: String): FontFamily = when (fileName) {
        FONT_SYSTEM -> FontFamily.Default
        FONT_MONOSPACE -> FontFamily.Monospace
        else -> familyCache.getOrPut(fileName) {
            runCatching { FontFamily(Font(File(fontsDir(), fileName))) }.getOrDefault(FontFamily.Default)
        }
    }

    /** Currently selected app-wide font family (reactive). */
    val fontFamily: FontFamily get() = fontFamilyFor(fontFileState)

    fun displayNameFor(fileName: String): String = when (fileName) {
        FONT_SYSTEM -> getAppContext().getString(R.string.kuchusen_font_system)
        FONT_MONOSPACE -> getAppContext().getString(R.string.kuchusen_font_monospace)
        else -> fileName.substringBeforeLast('.')
    }

    /** Copy a picked font file into our fonts dir. Returns its file name, or null if unusable. */
    fun importFont(uri: Uri): String? {
        val ctx = getAppContext()
        val name = queryFileName(ctx, uri) ?: return null
        if (name.substringAfterLast('.', "").lowercase() !in FONT_EXTENSIONS) return null
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        File(fontsDir(), name).writeBytes(bytes)
        familyCache.remove(name)
        return name
    }

    fun deleteFont(fileName: String): Boolean {
        if (fileName == FONT_SYSTEM || fileName == FONT_MONOSPACE) return false
        familyCache.remove(fileName)
        if (fontFamilyFile == fileName) fontFamilyFile = FONT_SYSTEM
        return File(fontsDir(), fileName).delete()
    }

    private fun queryFileName(ctx: Context, uri: Uri): String? {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
