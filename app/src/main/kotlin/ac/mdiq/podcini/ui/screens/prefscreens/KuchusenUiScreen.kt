package ac.mdiq.podcini.ui.screens.prefscreens

import ac.mdiq.podcini.R
import ac.mdiq.podcini.ui.compose.KuchusenUi
import ac.mdiq.podcini.utils.Logt
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val TAG = "KuchusenUiScreen"

// Each nesting level is indented hard so the hierarchy reads at a glance.
private val INDENT_STEP = 22.dp
private fun indent(level: Int): Dp = INDENT_STEP * level

private const val SAMPLE = "Aa Bb Cc  0123  白い熊 空中線"

@Composable
fun KuchusenUiScreen() {
    BackHandler(enabled = true) { pfBackStack.removeLastOrNull() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .background(KuchusenUi.backgroundColor).padding(start = 10.dp, end = 10.dp, bottom = 24.dp)) {

        // ---- Live preview (kept at the very top so every change is visible without scrolling) -----
        CatHeading(stringResource(R.string.kuchusen_cat_preview), first = true)
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

@Composable
private fun CatHeading(text: String, first: Boolean = false) {
    Text(text, color = KuchusenUi.accentColor, style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline,
        modifier = Modifier.padding(top = if (first) 8.dp else 20.dp, bottom = 4.dp))
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
