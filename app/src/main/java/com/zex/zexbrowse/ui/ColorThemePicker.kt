/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zex.zexbrowse.R

/** 圆圈选中标记：外圈描边 + 选中时内圈填充 */
@Composable
internal fun ThemeRadioCircle(selected: Boolean, swatch: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(swatch)
            )
        }
    }
}

/**
 * 单个主题选项：圆圈在上、名称在下，等宽居中。
 *
 * 之前是圆圈与文字横排塞进 weight(1f) 的格子里，长名称（如「动态颜色（Android 12+）」）
 * 会被挤成竖排单字。改为纵向布局并限制 2 行、自动缩排，任何语言都不再挤压错乱。
 */
@Composable
internal fun ColorThemeOption(label: String, selected: Boolean, swatch: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThemeRadioCircle(selected, swatch, onClick)
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            // 长名称自动缩小，避免撑破格子
            fontSize = MaterialTheme.typography.labelMedium.fontSize * 0.95f,
            lineHeight = MaterialTheme.typography.labelMedium.fontSize * 1.15f,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 颜色主题选择区。
 *
 * 固定 4 列等分网格：每格宽度严格相等（weight(1f) + 固定间距），
 * 行末不足 4 项时用等宽空位补齐，保证左右两端始终对齐。
 */
@Composable
internal fun ColorThemeGrid(
    current: String,
    customHex: String,
    onPick: (ColorTheme) -> Unit,
    onEditCustom: () -> Unit
) {
    val currentTheme = ColorTheme.entries.firstOrNull { it.name.equals(current, ignoreCase = true) } ?: ColorTheme.DYNAMIC
    val customSeed = parseColorOrNull(customHex) ?: defaultCustomSeed
    val columns = 4
    val spacing = 8.dp
    val items = ColorTheme.entries.toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // 每格等宽占位；不足 4 项时补空格，保证列宽与行对齐一致
                repeat(columns) { index ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        val theme = row.getOrNull(index)
                        if (theme != null) {
                            val swatch = when (theme) {
                                ColorTheme.DYNAMIC -> MaterialTheme.colorScheme.primary
                                ColorTheme.CUSTOM -> customSeed
                                else -> presetSeeds[theme] ?: MaterialTheme.colorScheme.primary
                            }
                            ColorThemeOption(
                                label = colorThemeLabel(theme),
                                selected = currentTheme == theme,
                                swatch = swatch,
                                onClick = { if (theme == ColorTheme.CUSTOM) onEditCustom() else onPick(theme) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可拖动取色的色相/饱和度面板 + 明度滑条。
 *
 * 面板横轴为色相、纵轴为饱和度；下方滑条调明度。
 *
 * 关键：hue/sat/value 只在首次进入时从 [color] 初始化一次（remember(Unit)），
 * 之后完全由手势驱动。若写成 remember(color)，每次拖动改变颜色都会重建 HSV 并回写，
 * 导致拖到边缘时数值被环回、颜色突然跳变到另一端。
 */
@Composable
private fun ColorPickerPanel(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val density = LocalDensity.current
    var panelWidth by remember { mutableFloatStateOf(1f) }
    var panelHeight by remember { mutableFloatStateOf(1f) }

    // 仅在首次组合时按传入颜色初始化，后续拖动不再被外部值覆盖
    var hue by remember { mutableFloatStateOf(initialHue(color)) }
    var sat by remember { mutableFloatStateOf(initialSat(color)) }
    var value by remember { mutableFloatStateOf(initialValue(color)) }

    Column {
        // 色相 × 饱和度面板
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
                .onSizeChanged {
                    panelWidth = it.width.toFloat().coerceAtLeast(1f)
                    panelHeight = it.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val h = (pos.x / panelWidth).coerceIn(0f, 1f) * 359.99f
                        val s = (pos.y / panelHeight).coerceIn(0f, 1f)
                        hue = h
                        sat = s
                        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, value))))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        // 把坐标夹紧到面板内，否则越界后取模会环回成另一端颜色
                        val x = change.position.x.coerceIn(0f, panelWidth)
                        val y = change.position.y.coerceIn(0f, panelHeight)
                        val h = (x / panelWidth).coerceIn(0f, 1f) * 359.99f
                        val s = (y / panelHeight).coerceIn(0f, 1f)
                        hue = h
                        sat = s
                        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, value))))
                    }
                }
        ) {
            // 当前选中位置的小圆点（按面板实际尺寸定位，避免越界）
            val dotX = with(density) { (hue / 359.99f * panelWidth - 9f.dp.toPx()).toDp() }.coerceAtLeast(0.dp)
            val dotY = with(density) { (sat * panelHeight - 9f.dp.toPx()).toDp() }.coerceAtLeast(0.dp)
            Box(
                Modifier
                    .padding(start = dotX, top = dotY)
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.color_custom_brightness),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(4.dp))
        // 明度滑条
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f))))
                    )
                )
                .pointerInput(hue, sat) {
                    fun apply(x: Float) {
                        val v = (x / size.width.toFloat()).coerceIn(0f, 1f).coerceAtLeast(0.05f)
                        value = v
                        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, v))))
                    }
                    detectTapGestures { apply(it.x) }
                }
                .pointerInput(hue, sat) {
                    fun apply(x: Float) {
                        val v = (x / size.width.toFloat()).coerceIn(0f, 1f).coerceAtLeast(0.05f)
                        value = v
                        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, v))))
                    }
                    detectDragGestures { change, _ -> apply(change.position.x) }
                }
        )
    }
}

private fun initialHue(c: Color): Float = hsvOf(c)[0]
private fun initialSat(c: Color): Float = hsvOf(c)[1]
private fun initialValue(c: Color): Float = hsvOf(c)[2].coerceAtLeast(0.15f)

private fun hsvOf(c: Color): FloatArray {
    val arr = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (c.red * 255).toInt().coerceIn(0, 255),
        (c.green * 255).toInt().coerceIn(0, 255),
        (c.blue * 255).toInt().coerceIn(0, 255),
        arr
    )
    return arr
}

/**
 * 自定义颜色弹窗。
 *
 * 底部为取色面板（可直接点按/拖动），同时保留 HEX 输入框供精确输入。
 * 两者双向同步：拖色板会更新 HEX，改 HEX 也会更新色板。
 */
@Composable
internal fun CustomColorDialog(
    initialHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(parseColorOrNull(initialHex) ?: defaultCustomSeed) }
    var hex by remember { mutableStateOf(current.toHexString()) }
    // 用户手动编辑 HEX 时不要被色板反向覆盖
    var editingHex by remember { mutableStateOf(false) }
    val parsed = parseColorOrNull(hex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.color_custom_pick), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                ColorPickerPanel(color = current) { picked ->
                    current = picked
                    if (!editingHex) hex = picked.toHexString()
                }
                // 预览
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(current)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { editingHex = true; hex = it; parseColorOrNull(it)?.let { c -> current = c } },
                    label = { Text(stringResource(R.string.color_custom_hex_label)) },
                    singleLine = true,
                    isError = parsed == null,
                    supportingText = {
                        Text(if (parsed == null) stringResource(R.string.color_custom_invalid) else stringResource(R.string.color_custom_preview))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = { onConfirm((parsed ?: defaultCustomSeed).toHexString()) }
            ) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.s071)) } }
    )
}
