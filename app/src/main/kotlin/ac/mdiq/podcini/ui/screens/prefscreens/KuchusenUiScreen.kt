package ac.mdiq.podcini.ui.screens.prefscreens

import ac.mdiq.podcini.PodciniApp.Companion.forceRestart
import ac.mdiq.podcini.R
import ac.mdiq.podcini.config.automation.AutomationAuth
import ac.mdiq.podcini.config.settings.KuchusenExport
import ac.mdiq.podcini.ui.compose.KuchusenUi
import ac.mdiq.podcini.ui.screens.PopMode
import ac.mdiq.podcini.ui.screens.defaultNavKey
import ac.mdiq.podcini.ui.screens.navTo
import ac.mdiq.podcini.utils.Logs
import ac.mdiq.podcini.utils.Logt
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import kotlin.math.roundToInt

private const val TAG = "KuchusenUiScreen"

// Each nesting level is indented hard so the hierarchy reads at a glance.
private val INDENT_STEP = 22.dp
private fun indent(level: Int): Dp = INDENT_STEP * level

private const val SAMPLE = "Aa Bb Cc  0123  白い熊 空中線"

// Warn-prominent red for export/import status lines (same value as the sister forks).
private val EXIM_WARN = Color(0xFFFF5252)

@Composable
fun KuchusenUiScreen() {
    BackHandler(enabled = true) { pfBackStack.removeLastOrNull() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Fork spec: the export directory is queried when the page opens for the latest export;
    // the Export/Import row summary carries the answer.
    var eximDirName by remember { mutableStateOf<String?>(null) }
    var eximStatus by remember { mutableStateOf("") }
    var eximWarn by remember { mutableStateOf(false) }
    var showExim by remember { mutableStateOf(false) }

    fun refreshEximStatus() {
        scope.launch(Dispatchers.IO) {
            val dir = KuchusenExport.exportDir(context)
            val name = dir?.name ?: KuchusenExport.dirUri(context)?.lastPathSegment
            val (text, warn) = when {
                dir == null -> context.getString(R.string.kuchusen_eim_warn_nodir) to true
                else -> {
                    val newest = KuchusenExport.latestExport(context)
                    if (newest == null) context.getString(R.string.kuchusen_eim_warn_none) to true
                    else context.getString(R.string.kuchusen_eim_last, KuchusenExport.formatTimestamp(newest.lastModified())) to false
                }
            }
            withContext(Dispatchers.Main) {
                eximDirName = name
                eximStatus = text
                eximWarn = warn
            }
        }
    }
    LaunchedEffect(Unit) { refreshEximStatus() }

    if (showExim) EximPanelDialog(
        dirName = eximDirName, status = eximStatus, warn = eximWarn,
        onRefresh = { refreshEximStatus() },
        onDismiss = { showExim = false },
        onCloseChain = {
            // Acknowledging success closes the whole chain: the info dialog and panel are gone,
            // and the UI settings page closes down to the app's main page.
            showExim = false
            pfBackStack.clear()
            pfBackStack.add(PFNav.Portal)
            navTo(defaultNavKey, PopMode.Clear)
        })

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .background(KuchusenUi.backgroundColor).padding(start = 10.dp, end = 10.dp, bottom = 24.dp)) {

        // ---- Export / Import (the Kōjiki flow: first separated section of the page) --------------
        CatHeading(stringResource(R.string.kuchusen_cat_eximport), first = true)
        Column(modifier = Modifier.fillMaxWidth().clickable { showExim = true }
            .padding(start = indent(1), top = 6.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.kuchusen_eim_row_title), color = KuchusenUi.textColor)
            Text(eximStatus, color = if (eximWarn) EXIM_WARN else KuchusenUi.secondaryTextColor, fontSize = 13.sp)
        }
        // Backup automation lives with backup — directly below the rows above, never in a section
        // of its own, so every sister app looks the same.
        AutomationRows(indent(1))

        // ---- Live preview (kept high so every change is visible without scrolling) ---------------
        CatHeading(stringResource(R.string.kuchusen_cat_preview))
        PreviewCard(indent(1))

        // ---- Colours -----------------------------------------------------------------------------
        CatHeading(stringResource(R.string.kuchusen_cat_colors))
        ColorRow(stringResource(R.string.kuchusen_color_background), indent(1), KuchusenUi.backgroundColor) { KuchusenUi.backgroundColor = it }
        ColorRow(stringResource(R.string.kuchusen_color_text), indent(1), KuchusenUi.textColor) { KuchusenUi.textColor = it }
        ColorRow(stringResource(R.string.kuchusen_color_secondary_text), indent(1), KuchusenUi.secondaryTextColor) { KuchusenUi.secondaryTextColor = it }
        ColorRow(stringResource(R.string.kuchusen_color_accent), indent(1), KuchusenUi.accentColor) { KuchusenUi.accentColor = it }
        ColorRow(stringResource(R.string.kuchusen_color_border), indent(1), KuchusenUi.borderColor) { KuchusenUi.borderColor = it }

        // ---- Typography --------------------------------------------------------------------------
        CatHeading(stringResource(R.string.kuchusen_cat_typography))
        FontRow(indent(1))
        SliderRow(stringResource(R.string.kuchusen_font_size), indent(1),
            value = KuchusenUi.fontScale, range = 0.5f..2.0f,
            valueLabel = "${(KuchusenUi.fontScale * 100).roundToInt()} %") { KuchusenUi.fontScale = it }
        SliderRow(stringResource(R.string.kuchusen_font_weight), indent(1),
            value = KuchusenUi.fontWeight.toFloat(), range = 0f..900f, steps = 8,
            valueLabel = if (KuchusenUi.fontWeight == 0) stringResource(R.string.kuchusen_value_default) else KuchusenUi.fontWeight.toString()) {
            KuchusenUi.fontWeight = (it / 100f).roundToInt() * 100
        }

        // ---- Shape & borders ---------------------------------------------------------------------
        CatHeading(stringResource(R.string.kuchusen_cat_shape))
        SliderRow(stringResource(R.string.kuchusen_corner_roundness), indent(1),
            value = KuchusenUi.cornerRoundness.toFloat(), range = 0f..28f,
            valueLabel = "${KuchusenUi.cornerRoundness} dp") { KuchusenUi.cornerRoundness = it.roundToInt() }
        SliderRow(stringResource(R.string.kuchusen_border_thickness), indent(1),
            value = KuchusenUi.borderWidth.toFloat(), range = 0f..8f,
            valueLabel = "${KuchusenUi.borderWidth} dp") { KuchusenUi.borderWidth = it.roundToInt() }

        // ---- Reset -------------------------------------------------------------------------------
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = { KuchusenUi.resetToHouseStyle() }, modifier = Modifier.padding(start = indent(1))) {
            Text(stringResource(R.string.kuchusen_reset), color = KuchusenUi.accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

// kxkb-style section heading: a full-width hairline separator above (except the first section),
// then a big bold heading underlined exactly as wide as its text.
@Composable
fun CatHeading(text: String, first: Boolean = false) {
    if (!first) HorizontalDivider(thickness = Dp.Hairline, color = KuchusenUi.accentColor,
        modifier = Modifier.padding(top = 10.dp))
    Column(modifier = Modifier.padding(top = if (first) 12.dp else 8.dp, bottom = 2.dp).width(IntrinsicSize.Max)) {
        Text(text, color = KuchusenUi.accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().padding(top = 2.dp).height(2.5.dp).background(KuchusenUi.accentColor))
    }
}

// kxkb-style sub-heading: smaller, thinner text-wide underline, no full-width separator.
@Composable
fun SubHeading(text: String, start: Dp = 0.dp) {
    Column(modifier = Modifier.padding(start = start, top = 10.dp, bottom = 2.dp).width(IntrinsicSize.Max)) {
        Text(text, color = KuchusenUi.accentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().padding(top = 2.dp).height(1.5.dp).background(KuchusenUi.accentColor))
    }
}

@Composable
private fun PreviewCard(start: Dp) {
    val shape = RoundedCornerShape(KuchusenUi.cornerRoundness.dp)
    val weight = if (KuchusenUi.fontWeight > 0) FontWeight(KuchusenUi.fontWeight) else FontWeight.Normal
    Box(modifier = Modifier.fillMaxWidth().padding(start = start, top = 2.dp, bottom = 2.dp)
        .clip(shape).border(KuchusenUi.borderWidth.dp, KuchusenUi.borderColor, shape)
        .background(KuchusenUi.backgroundColor).padding(12.dp)) {
        Column {
            Text(SAMPLE, color = KuchusenUi.textColor, fontFamily = KuchusenUi.fontFamily,
                fontWeight = weight, fontSize = 20.sp * KuchusenUi.fontScale)
            Text(stringResource(R.string.kuchusen_preview_secondary), color = KuchusenUi.secondaryTextColor,
                fontFamily = KuchusenUi.fontFamily, fontWeight = weight, fontSize = 14.sp * KuchusenUi.fontScale)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Box(Modifier.size(20.dp).clip(shape).border(KuchusenUi.borderWidth.dp, KuchusenUi.borderColor, shape).background(KuchusenUi.accentColor))
                Text(stringResource(R.string.kuchusen_color_accent), color = KuchusenUi.accentColor,
                    fontFamily = KuchusenUi.fontFamily, fontWeight = weight, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ColorRow(label: String, start: Dp, color: Color, onColor: (Color) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { show = true }.padding(start = start, top = 3.dp, bottom = 3.dp)) {
        Text(label, color = KuchusenUi.textColor, modifier = Modifier.weight(1f))
        ColorSwatch(color, 28.dp)
    }
    if (show) ColorPickerDialog(label, color, onColor) { show = false }
}

@Composable
private fun ColorSwatch(color: Color, sz: Dp) {
    val shape = RoundedCornerShape(4.dp)
    // White underlay so partial alpha is visible.
    Box(Modifier.size(sz).clip(shape).border(1.dp, KuchusenUi.borderColor, shape).background(Color.White)) {
        Box(Modifier.fillMaxSize().background(color))
    }
}

@Composable
private fun SliderRow(label: String, start: Dp, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, valueLabel: String, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = start, top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = KuchusenUi.textColor, modifier = Modifier.weight(1f))
            Text(valueLabel, color = KuchusenUi.secondaryTextColor)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            colors = SliderDefaults.colors(thumbColor = KuchusenUi.accentColor, activeTrackColor = KuchusenUi.accentColor))
    }
}

@Composable
private fun FontRow(start: Dp) {
    var show by remember { mutableStateOf(false) }
    val file = KuchusenUi.fontFamilyFile
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { show = true }.padding(start = start, top = 3.dp, bottom = 3.dp)) {
        Text(stringResource(R.string.kuchusen_font_family), color = KuchusenUi.textColor, modifier = Modifier.weight(1f))
        Text(KuchusenUi.displayNameFor(file), color = KuchusenUi.secondaryTextColor, fontFamily = KuchusenUi.fontFamilyFor(file))
    }
    if (show) FontPickerDialog(onPick = { KuchusenUi.fontFamilyFile = it; show = false }, onDismiss = { show = false })
}

@Composable
private fun ColorPickerDialog(title: String, initial: Color, onColor: (Color) -> Unit, onClose: () -> Unit) {
    var r by remember { mutableIntStateOf((initial.red * 255).roundToInt()) }
    var g by remember { mutableIntStateOf((initial.green * 255).roundToInt()) }
    var b by remember { mutableIntStateOf((initial.blue * 255).roundToInt()) }
    var a by remember { mutableIntStateOf((initial.alpha * 255).roundToInt()) }
    val current = Color(r, g, b, a)

    fun setFrom(c: Color) {
        r = (c.red * 255).roundToInt(); g = (c.green * 255).roundToInt()
        b = (c.blue * 255).roundToInt(); a = (c.alpha * 255).roundToInt()
        onColor(c)            // live preview across the whole app
    }

    AlertDialog(
        modifier = Modifier.border(1.dp, KuchusenUi.borderColor, MaterialTheme.shapes.extraLarge),
        onDismissRequest = { onColor(initial); onClose() },
        title = { Text(title, color = KuchusenUi.textColor) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                // One-click recents, prefilled with previously chosen colours.
                val recents = KuchusenUi.recentColors
                if (recents.isNotEmpty()) {
                    Text(stringResource(R.string.kuchusen_recent_colors), color = KuchusenUi.secondaryTextColor, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recents.forEach { argb ->
                            val c = Color(argb)
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
                                .border(1.dp, KuchusenUi.borderColor, RoundedCornerShape(4.dp))
                                .background(Color.White).clickable { setFrom(c) }) {
                                Box(Modifier.fillMaxSize().background(c))
                            }
                        }
                    }
                }
                // Large live preview swatch + hex.
                Box(Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp).clip(RoundedCornerShape(6.dp))
                    .border(1.dp, KuchusenUi.borderColor, RoundedCornerShape(6.dp)).background(Color.White)) {
                    Box(Modifier.fillMaxSize().background(current))
                }
                Text("#%08X".format(current.toArgb()), color = KuchusenUi.textColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                ChannelSlider("R", r, Color(0xFFE53935)) { r = it; onColor(Color(r, g, b, a)) }
                ChannelSlider("G", g, Color(0xFF43A047)) { g = it; onColor(Color(r, g, b, a)) }
                ChannelSlider("B", b, Color(0xFF1E88E5)) { b = it; onColor(Color(r, g, b, a)) }
                ChannelSlider("A", a, Color(0xFF9E9E9E)) { a = it; onColor(Color(r, g, b, a)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColor(current); KuchusenUi.addRecentColor(current.toArgb()); onClose() }) {
                Text("OK", color = KuchusenUi.accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = { onColor(initial); onClose() }) {
                Text(stringResource(R.string.cancel_label), color = KuchusenUi.accentColor)
            }
        }
    )
}

@Composable
private fun ChannelSlider(name: String, value: Int, tint: Color, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, color = tint, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) }, valueRange = 0f..255f,
            modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint))
        Text(value.toString(), color = KuchusenUi.textColor, modifier = Modifier.width(36.dp))
    }
}

// ---- 保存復元 automation (token-gated headless export, driven by 白い熊 自由作業盤) --------------

private fun hasAllFilesAccess(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

@Composable
private fun AutomationRows(start: Dp) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AutomationAuth.isEnabled(context)) }
    var token by remember { mutableStateOf(AutomationAuth.token(context)) }
    var showRegen by remember { mutableStateOf(false) }
    var filesAccess by remember { mutableStateOf(hasAllFilesAccess()) }
    // The grant screen returns no result — re-read the real state when we come back from it.
    val grantLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        filesAccess = hasAllFilesAccess()
    }

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = start, top = 6.dp, bottom = 2.dp)) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(stringResource(R.string.kuchusen_auto_title), color = KuchusenUi.textColor)
            Text(stringResource(R.string.kuchusen_auto_sum), color = KuchusenUi.secondaryTextColor, fontSize = 13.sp)
        }
        Switch(checked = enabled, onCheckedChange = { on ->
            enabled = on
            AutomationAuth.setEnabled(context, on)
        }, colors = SwitchDefaults.colors(
            checkedThumbColor = KuchusenUi.backgroundColor, checkedTrackColor = KuchusenUi.accentColor,
            uncheckedThumbColor = KuchusenUi.secondaryTextColor, uncheckedTrackColor = KuchusenUi.backgroundColor,
            uncheckedBorderColor = KuchusenUi.accentColor))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = start)) {
        Column(modifier = Modifier.weight(1f).clickable {
            val clip = context.getSystemService(ClipboardManager::class.java)
            clip?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.kuchusen_auto_token), token))
            Toast.makeText(context, R.string.kuchusen_auto_token_copied, Toast.LENGTH_SHORT).show()
        }.padding(top = 4.dp, bottom = 4.dp, end = 8.dp)) {
            Text(stringResource(R.string.kuchusen_auto_token), color = KuchusenUi.textColor)
            Text(AutomationAuth.abbreviated(token), color = KuchusenUi.secondaryTextColor,
                fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.kuchusen_auto_token_hint), color = KuchusenUi.secondaryTextColor, fontSize = 12.sp)
        }
        TextButton(onClick = { showRegen = true }) {
            Text(stringResource(R.string.kuchusen_auto_regen), color = KuchusenUi.accentColor)
        }
    }

    // Without All-files access an automation run cannot honour its own target directory.
    if (!filesAccess) Text(stringResource(R.string.kuchusen_auto_files_warn), color = EXIM_WARN, fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().clickable {
            runCatching {
                grantLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:${context.packageName}".toUri()))
            }.onFailure { grantLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }.padding(start = start, top = 2.dp, bottom = 4.dp))

    if (showRegen) HouseConfirmDialog(
        title = stringResource(R.string.kuchusen_auto_regen_title),
        body = stringResource(R.string.kuchusen_auto_regen_body),
        confirmLabel = stringResource(R.string.kuchusen_auto_regen),
        onConfirm = { token = AutomationAuth.regenerate(context); showRegen = false },
        onDismiss = { showRegen = false })
}

/** The house-style (black, accent-bordered, pill-buttoned) confirmation used across this page. */
@Composable
private fun HouseConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(modifier = Modifier.clip(shape).border(2.dp, KuchusenUi.accentColor, shape)
            .background(KuchusenUi.backgroundColor).padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 16.dp)) {
            Text(title, color = KuchusenUi.accentColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(body, color = KuchusenUi.accentColor, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                PillButton(stringResource(R.string.cancel_label)) { onDismiss() }
                PillButton(confirmLabel) { onConfirm() }
            }
        }
    }
}

// ---- Export / Import (the Kōjiki flow, ArcaneChat pill buttons) ---------------------------------

private class EximInfo(val title: String, val body: String, val isImport: Boolean, val mustRestart: Boolean = false)

/** An ArcaneChat-style round pill: house-background fill, accent stroke, accent text. */
@Composable
private fun PillButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(label, color = KuchusenUi.accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f).clip(shape)
            .border(1.5.dp, KuchusenUi.accentColor, shape).background(KuchusenUi.backgroundColor)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp))
}

/**
 * The black, accent-bordered info dialog: "✓ Export finished" (OK closes the whole chain) or
 * "✓ Import finished" (Later closes the chain, Restart now relaunches the app).
 */
@Composable
private fun EximInfoDialog(info: EximInfo, onCloseChain: () -> Unit, onRestart: () -> Unit) {
    Dialog(onDismissRequest = { }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        val shape = RoundedCornerShape(16.dp)
        Column(modifier = Modifier.clip(shape).border(2.dp, KuchusenUi.accentColor, shape)
            .background(KuchusenUi.backgroundColor).padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 16.dp)) {
            Text(info.title, color = KuchusenUi.accentColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(info.body, color = KuchusenUi.accentColor, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                when {
                    // A restored database is live only after the restart — "Later" would run the
                    // app against a file that is no longer the one it opened.
                    info.mustRestart -> PillButton(stringResource(R.string.kuchusen_eim_restart_now)) { onRestart() }
                    info.isImport -> {
                        PillButton(stringResource(R.string.kuchusen_eim_restart_later)) { onCloseChain() }
                        PillButton(stringResource(R.string.kuchusen_eim_restart_now)) { onRestart() }
                    }
                    else -> PillButton(stringResource(R.string.kuchusen_eim_ok)) { onCloseChain() }
                }
            }
        }
    }
}

@Composable
private fun EximPanelDialog(dirName: String?, status: String, warn: Boolean,
                            onRefresh: () -> Unit, onDismiss: () -> Unit, onCloseChain: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    // While set, overrides the page-level status line inside the panel (progress/failure messages).
    var localStatus by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // Every category starts ticked — exporting everything is the common case.
    val checks = remember { mutableStateMapOf<KuchusenExport.Cat, Boolean>().apply { KuchusenExport.Cat.entries.forEach { put(it, true) } } }
    var selectAll by remember { mutableStateOf(true) }
    var infoDialog by remember { mutableStateOf<EximInfo?>(null) }
    var pendingExportCats by remember { mutableStateOf<Set<KuchusenExport.Cat>>(emptySet()) }
    var pendingExportName by remember { mutableStateOf("") }

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            KuchusenExport.setDirUri(context, uri)
            localStatus = null
            onRefresh()
        }
    }

    fun selectedCats(): Set<KuchusenExport.Cat> = KuchusenExport.Cat.entries.filter { checks[it] == true }.toSet()

    fun runExport(cats: Set<KuchusenExport.Cat>, displayName: String, openOutput: () -> OutputStream) {
        busy = true
        localStatus = context.getString(R.string.kuchusen_eim_exporting) to false
        scope.launch(Dispatchers.IO) {
            try {
                // The same progress the automation path broadcasts, shown in the panel.
                KuchusenExport.export(cats, openOutput) { localStatus = it.text to false }
                withContext(Dispatchers.Main) {
                    localStatus = null
                    onRefresh()
                    infoDialog = EximInfo(context.getString(R.string.kuchusen_eim_export_done_title),
                        context.getString(R.string.kuchusen_eim_export_done_body, cats.size, displayName), isImport = false)
                }
            } catch (e: Exception) {
                Logs(TAG, e, "settings export failed")
                withContext(Dispatchers.Main) {
                    localStatus = context.getString(R.string.kuchusen_eim_export_failed, e.message ?: e.javaClass.simpleName) to true
                }
            } finally { withContext(Dispatchers.Main) { busy = false } }
        }
    }

    val saveAsPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) runExport(pendingExportCats, pendingExportName) {
            context.contentResolver.openOutputStream(uri) ?: throw IOException("could not open $pendingExportName for writing")
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val cats = selectedCats()
            busy = true
            localStatus = context.getString(R.string.kuchusen_eim_importing) to false
            scope.launch(Dispatchers.IO) {
                try {
                    val result = KuchusenExport.import(cats) {
                        context.contentResolver.openInputStream(uri) ?: throw IOException("could not open the import file")
                    }
                    withContext(Dispatchers.Main) {
                        localStatus = null
                        onRefresh()
                        infoDialog = EximInfo(context.getString(R.string.kuchusen_eim_import_done_title),
                            context.getString(R.string.kuchusen_eim_import_done_body, result.summary),
                            isImport = true, mustRestart = result.databaseRestored)
                    }
                } catch (e: Exception) {
                    Logs(TAG, e, "settings import failed")
                    withContext(Dispatchers.Main) {
                        localStatus = context.getString(R.string.kuchusen_eim_import_failed, e.message ?: e.javaClass.simpleName) to true
                    }
                } finally { withContext(Dispatchers.Main) { busy = false } }
            }
        }
    }

    fun onExportClicked() {
        val cats = selectedCats()
        if (cats.isEmpty()) {
            localStatus = context.getString(R.string.kuchusen_eim_none_selected) to true
            return
        }
        val name = KuchusenExport.exportFileName()
        val dir = KuchusenExport.exportDir(context)
        if (dir == null) {
            // No directory configured — fall back to a save-as picker.
            pendingExportCats = cats
            pendingExportName = name
            saveAsPicker.launch(name)
            return
        }
        runExport(cats, name) {
            val file = dir.createFile("application/zip", name) ?: throw IOException("could not create $name in the export directory")
            context.contentResolver.openOutputStream(file.uri) ?: throw IOException("could not open $name for writing")
        }
    }

    fun onImportClicked() {
        val cats = selectedCats()
        if (cats.isEmpty()) {
            localStatus = context.getString(R.string.kuchusen_eim_none_selected) to true
            return
        }
        importPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
    }

    infoDialog?.let { EximInfoDialog(it, onCloseChain = onCloseChain, onRestart = { forceRestart() }) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        val shape = RoundedCornerShape(16.dp)
        val checkboxColors = CheckboxDefaults.colors(checkedColor = KuchusenUi.accentColor,
            checkmarkColor = KuchusenUi.backgroundColor, uncheckedColor = KuchusenUi.accentColor)
        Column(modifier = Modifier.clip(shape).border(2.dp, KuchusenUi.accentColor, shape)
            .background(KuchusenUi.backgroundColor).padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp)
            .verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.kuchusen_eim_title), color = KuchusenUi.accentColor, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.kuchusen_eim_desc), color = KuchusenUi.secondaryTextColor, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp))

            // The persisted export directory — a bordered, clearly-tappable box.
            val dirShape = RoundedCornerShape(10.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(dirShape)
                .border(1.5.dp, KuchusenUi.accentColor, dirShape)
                .clickable(enabled = !busy) { dirPicker.launch(KuchusenExport.dirUri(context)) }
                .padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.kuchusen_eim_dir), color = KuchusenUi.accentColor, fontSize = 12.sp)
                Text(dirName ?: stringResource(R.string.kuchusen_eim_dir_unset), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (dirName == null) EXIM_WARN else KuchusenUi.secondaryTextColor)
            }

            val (statusText, statusWarn) = localStatus ?: (status to warn)
            Text(statusText, color = if (statusWarn) EXIM_WARN else KuchusenUi.secondaryTextColor, fontSize = 14.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))

            HorizontalDivider(thickness = Dp.Hairline, color = KuchusenUi.accentColor)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.kuchusen_eim_select_all), color = KuchusenUi.textColor,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Checkbox(checked = selectAll, colors = checkboxColors, onCheckedChange = { checked ->
                    selectAll = checked
                    KuchusenExport.Cat.entries.forEach { checks[it] = checked }
                })
            }
            KuchusenExport.Cat.entries.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(cat.labelRes), color = KuchusenUi.textColor, modifier = Modifier.weight(1f))
                    Checkbox(checked = checks[cat] == true, colors = checkboxColors,
                        onCheckedChange = { checks[cat] = it })
                }
            }

            HorizontalDivider(thickness = Dp.Hairline, color = KuchusenUi.accentColor, modifier = Modifier.padding(top = 8.dp))

            // ArcaneChat-style action row: round pills, Cancel alone on the left,
            // Import / Export grouped on the right.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                PillButton(stringResource(R.string.cancel_label), enabled = !busy) { onDismiss() }
                Spacer(Modifier.weight(1f))
                PillButton(stringResource(R.string.kuchusen_eim_import_label), enabled = !busy) { onImportClicked() }
                Spacer(Modifier.width(8.dp))
                PillButton(stringResource(R.string.kuchusen_eim_export_label), enabled = !busy) { onExportClicked() }
            }
        }
    }
}

@Composable
private fun FontPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val fonts = remember { KuchusenUi.availableFonts() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = KuchusenUi.importFont(uri)
            if (name != null) onPick(name) else Logt(TAG, "Not a usable .ttf/.otf font")
        }
    }
    AlertDialog(
        modifier = Modifier.border(1.dp, KuchusenUi.borderColor, MaterialTheme.shapes.extraLarge),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.kuchusen_font_family), color = KuchusenUi.textColor) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                fonts.forEachIndexed { i, opt ->
                    if (i > 0) HorizontalDivider()
                    Text(opt.displayName, color = KuchusenUi.textColor, fontFamily = KuchusenUi.fontFamilyFor(opt.fileName),
                        fontSize = 18.sp, modifier = Modifier.fillMaxWidth().clickable { onPick(opt.fileName) }.padding(vertical = 10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Text(stringResource(R.string.kuchusen_import_font), color = KuchusenUi.accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_label), color = KuchusenUi.accentColor) }
        }
    )
}
