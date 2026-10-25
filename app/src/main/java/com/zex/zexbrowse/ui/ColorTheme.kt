/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.zex.zexbrowse.R

/**
 * 颜色主题预设。
 *
 * 每个预设只定义一组「种子色」，由 buildLightScheme / buildDarkScheme 按 Material 3 的
 * 明暗两套规则推导出完整的 ColorScheme，避免为每个预设手写几十个色值。
 *
 * [DYNAMIC] 与 [CUSTOM] 不在此表内：前者走系统动态取色，
 * 后者由用户选定的主色实时推导。
 *
 * 名称不在枚举里硬编码，而是走字符串资源（见 colorThemeLabel），
 * 否则切换语言后选项名会停在中文本。
 */
enum class ColorTheme {
    DYNAMIC,
    OLIVE,
    VELVET,
    MIST,
    GLACIER,
    DUSK,
    TITANIUM,
    FOREST,
    CUSTOM
}

/** 选项名称的资源 id，按当前语言显示 */
@Composable
fun colorThemeLabel(theme: ColorTheme): String = stringResource(
    when (theme) {
        ColorTheme.DYNAMIC -> R.string.color_theme_dynamic
        ColorTheme.OLIVE -> R.string.color_theme_olive
        ColorTheme.VELVET -> R.string.color_theme_velvet
        ColorTheme.MIST -> R.string.color_theme_mist
        ColorTheme.GLACIER -> R.string.color_theme_glacier
        ColorTheme.DUSK -> R.string.color_theme_dusk
        ColorTheme.TITANIUM -> R.string.color_theme_titanium
        ColorTheme.FOREST -> R.string.color_theme_forest
        ColorTheme.CUSTOM -> R.string.color_theme_custom
    }
)

/**
 * 按种子色生成明亮的 Material 3 配色。
 *
 * 用种子色分别派生主色、次色、三级色与容器色：
 * 主色保持种子色本身的色相与饱和度，容器色取高明度低饱和，保证前景可读。
 */
fun buildLightScheme(seed: Color, neutralSeed: Color? = null) = lightColorScheme(
    primary = seed.adjust(lightness = 0.42f),
    onPrimary = Color.White,
    primaryContainer = seed.adjust(lightness = 0.90f),
    onPrimaryContainer = seed.adjust(lightness = 0.22f),
    secondary = (neutralSeed ?: seed).adjust(lightness = 0.48f),
    onSecondary = Color.White,
    secondaryContainer = (neutralSeed ?: seed).adjust(lightness = 0.92f),
    onSecondaryContainer = (neutralSeed ?: seed).adjust(lightness = 0.24f),
    tertiary = seed.rotate(28f).adjust(lightness = 0.46f),
    onTertiary = Color.White,
    tertiaryContainer = seed.rotate(28f).adjust(lightness = 0.90f),
    onTertiaryContainer = seed.rotate(28f).adjust(lightness = 0.24f),
    surface = seed.mixWithNeutral(0.97f),
    surfaceVariant = seed.mixWithNeutral(0.92f),
    outline = seed.mixWithNeutral(0.55f)
)

/** 同一套种子色的深色变体：降低明度、提高背景对比，保持色相一致 */
fun buildDarkScheme(seed: Color, neutralSeed: Color? = null) = darkColorScheme(
    primary = seed.adjust(lightness = 0.78f),
    onPrimary = seed.adjust(lightness = 0.18f),
    primaryContainer = seed.adjust(lightness = 0.34f),
    onPrimaryContainer = seed.adjust(lightness = 0.90f),
    secondary = (neutralSeed ?: seed).adjust(lightness = 0.74f),
    onSecondary = (neutralSeed ?: seed).adjust(lightness = 0.16f),
    secondaryContainer = (neutralSeed ?: seed).adjust(lightness = 0.30f),
    onSecondaryContainer = (neutralSeed ?: seed).adjust(lightness = 0.90f),
    tertiary = seed.rotate(28f).adjust(lightness = 0.76f),
    onTertiary = seed.rotate(28f).adjust(lightness = 0.18f),
    tertiaryContainer = seed.rotate(28f).adjust(lightness = 0.32f),
    onTertiaryContainer = seed.rotate(28f).adjust(lightness = 0.90f),
    surface = seed.mixWithNeutral(0.08f),
    surfaceVariant = seed.mixWithNeutral(0.14f),
    outline = seed.mixWithNeutral(0.42f)
)

/** 把颜色按目标明度重算 HSV 的 V 分量，保留色相与饱和度 */
private fun Color.adjust(lightness: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv
    )
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0f, 1f), lightness.coerceIn(0f, 1f)))
    return Color(rgb)
}

/** 旋转色相，用于派生三级色，避免主/次/三级同色 */
private fun Color.rotate(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv
    )
    val h = (hsv[0] + degrees) % 360f
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(h, hsv[1], hsv[2]))
    return Color(rgb)
}

/** 把种子色与中性灰混合，用于派生表面色，保留一点色相而不过艳 */
private fun Color.mixWithNeutral(targetLuminance: Float): Color {
    val neutral = Color(targetLuminance, targetLuminance, targetLuminance)
    return Color(
        red * 0.12f + neutral.red * 0.88f,
        green * 0.12f + neutral.green * 0.88f,
        blue * 0.12f + neutral.blue * 0.88f
    )
}

/** 各预设的种子色；DYNAMIC 与 CUSTOM 不在此表 */
val presetSeeds: Map<ColorTheme, Color> = mapOf(
    ColorTheme.OLIVE to Color(0xFF6B7A3A),
    ColorTheme.VELVET to Color(0xFF8E3B5C),
    ColorTheme.MIST to Color(0xFF5B7C99),
    ColorTheme.GLACIER to Color(0xFF3E8FA8),
    ColorTheme.DUSK to Color(0xFF8A6242),
    ColorTheme.TITANIUM to Color(0xFF5F6B73),
    ColorTheme.FOREST to Color(0xFF3D6B4F)
)

/** 预设的中性色基准，让表面色更贴合预设气质（可选，缺省时用种子色） */
val presetNeutrals: Map<ColorTheme, Color> = mapOf(
    ColorTheme.OLIVE to Color(0xFF7A7A6A),
    ColorTheme.VELVET to Color(0xFF7A6A70),
    ColorTheme.MIST to Color(0xFF6A7280),
    ColorTheme.GLACIER to Color(0xFF6A8090),
    ColorTheme.DUSK to Color(0xFF7A7060),
    ColorTheme.TITANIUM to Color(0xFF707070),
    ColorTheme.FOREST to Color(0xFF6A7A6E)
)

/** 自定义主题的默认种子色（取 Material 默认蓝紫，用户可再调整） */
val defaultCustomSeed = Color(0xFF4A5BC4)

/** 解析用户保存的自定义颜色（#RRGGBB 或 #AARRGGBB）；非法时回退默认色 */
fun parseColorOrNull(value: String): Color? = runCatching {
    val hex = value.trim().removePrefix("#")
    val argb = when (hex.length) {
        6 -> 0xFF000000.toInt() or hex.toLong(16).toInt()
        8 -> hex.toLong(16).toInt()
        else -> return null
    }
    Color(argb)
}.getOrNull()

/** 格式化为 #AARRGGBB，与图中展示的写法一致 */
fun Color.toHexString(): String = "#%08X".format(
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
)

/** 该颜色在对应背景上应当用深色还是浅色文字 */
fun Color.prefersDarkContent(): Boolean = luminance() > 0.5f
