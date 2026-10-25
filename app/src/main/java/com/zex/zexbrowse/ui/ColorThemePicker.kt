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
import androidx.compose.ui.geometry.Offset
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
 * 固定 4 列网格：每格等宽，行末不足 4 项时补空位，保证永远对齐。
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
    val items = ColorTheme.entries.toList()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { theme ->
                    val swatch = when (theme) {
                        ColorTheme.DYNAMIC -> MaterialTheme.colorScheme.primary
                        ColorTheme.CUSTOM -> customSeed
                        else -> presetSeeds[theme] ?: MaterialTheme.colorScheme.primary
                    }
                    Box(Modifier.weight(1f)) {
                        ColorThemeOption(
                            label = colorThemeLabel(theme),
                            selected = currentTheme == theme,
                            swatch = swatch,
                            onClick = { if (theme == ColorTheme.CUSTOM) onEditCustom() else onPick(theme) }
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 可拖动取色的色相/饱和度面板 + 明度滑条。
 *
 * 面板横轴为色相、纵轴为饱和度；下方滑条调明度。
 * 这样用户可以直接点/拖选颜色，不必知道颜色编号。
 */
@Composable
private fun ColorPickerPanel(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val density = LocalDensity.current
    var panelWidth by remember { mutableFloatStateOf(1f) }
    var panelHeight by remember { mutableFloatStateOf(1f) }
    val hsv = remember(color) {
        val arr = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255).toInt().coerceIn(0, 255),
            (color.green * 255).toInt().coerceIn(0, 255),
            (color.blue * 255).toInt().coerceIn(0, 255),
            arr
        )
        arr
    }
    var hue by remember(color) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(color) { mutableFloatStateOf(hsv[1]) }
    var value by remember(color) { mutableFloatStateOf(hsv[2].coerceAtLeast(0.15f)) }

    fun emit(h: Float, s: Float, v: Float) {
        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))))
    }

    fun pickAt(pos: Offset) {
        val h = (pos.x / panelWidth).coerceIn(0f, 1f) * 360f
        val s = (pos.y / panelHeight).coerceIn(0f, 1f)
        hue = h
        sat = s
        emit(h, s, value)
    }

    Column {
        // 色相 × 饱和度面板
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent)))
                .onSizeChanged { panelWidth = it.width.toFloat().coerceAtLeast(1f); panelHeight = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { pickAt(it) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> pickAt(change.position) }
                }
        ) {
            // 当前选中位置的小圆点
            val dotX = with(density) { (hue / 360f * panelWidth).toDp() }
            val dotY = with(density) { (sat * panelHeight).toDp() }
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
        // 明度滑条
        Text(
            stringResource(R.string.color_custom_brightness),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(4.dp))
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
                    // 不用嵌套局部函数：detectTapGestures 的闭包里直接算，避免作用域歧义
                    detectTapGestures { offset ->
                        val v = (offset.x / size.width).coerceIn(0f, 1f).coerceAtLeast(0.05f)
                        value = v
                        emit(hue, sat, v)
                    }
                }
                .pointerInput(hue, sat) {
                    detectDragGestures { change, _ ->
                        val v = (change.position.x / size.width).coerceIn(0f, 1f).coerceAtLeast(0.05f)
                        value = v
                        emit(hue, sat, v)
                    }
                }
        )
    }
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
