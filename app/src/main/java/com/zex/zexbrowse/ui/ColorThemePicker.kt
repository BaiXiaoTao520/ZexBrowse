/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zex.zexbrowse.R

/** 圆圈选中标记：外圈描边 + 选中时内圈填充 */
@Composable
internal fun ThemeRadioCircle(selected: Boolean, swatch: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch)
            )
        }
    }
}

/** 单个主题选项：圆圈 + 名称 */
@Composable
internal fun ColorThemeOption(label: String, selected: Boolean, swatch: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeRadioCircle(selected, swatch, onClick)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * 颜色主题选择区。
 *
 * 按图中布局分两行铺开，每行四项；自定义项单独弹窗编辑颜色。
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
    val items = ColorTheme.entries.toList()
    // 每行 4 项，与参考图一致
    items.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                        onClick = {
                            if (theme == ColorTheme.CUSTOM) onEditCustom() else onPick(theme)
                        }
                    )
                }
            }
            // 末行不足 4 项时补空位，保持列对齐
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * 自定义颜色编辑弹窗。
 *
 * 用 HEX 输入框而不是完整取色环：环形取色需要额外的绘制与手势代码，
 * 而参考图里也只展示了色板预览与 HEX 值，用户可直接粘贴颜色码。
 */
@Composable
internal fun CustomColorDialog(
    initialHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hex by remember { mutableStateOf(initialHex.ifBlank { defaultCustomSeed.toHexString() }) }
    val parsed = parseColorOrNull(hex)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.color_custom_style), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                // 颜色预览
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parsed ?: Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it },
                    label = { Text(stringResource(R.string.color_custom_hex_label)) },
                    singleLine = true,
                    isError = parsed == null,
                    supportingText = {
                        Text(if (parsed == null) stringResource(R.string.color_custom_invalid) else stringResource(R.string.color_custom_preview))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                // 快捷色板：点击直接填入，省去手动输入
                Text(stringResource(R.string.color_custom_quick), style = MaterialTheme.typography.labelMedium)
                val quick = listOf(0xFF6B7A3A, 0xFF8E3B5C, 0xFF5B7C99, 0xFF3E8FA8, 0xFF8A6242, 0xFF5F6B73, 0xFF3D6B4F)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    quick.forEach { argb ->
                        val c = Color(argb)
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { hex = c.toHexString() }
                        )
                    }
                }
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
