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
 * 每个预设只定义一组「种子色」，由 buildLightScheme / buildDarkScheme 推导出完整的
 * ColorScheme。所有字段都会按种子色的色相重新生成，不留给 Material3 默认值，
 * 否则默认的紫色系会混进来，浅色下卡片显得发灰发暗。
 *
 * 名称走字符串资源（见 colorThemeLabel），否则切换语言后选项名会停在中文本。
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
 * 浅色配色。
 *
 * 表面色阶（surface / surfaceContainer*）统一由种子色相 + 极低饱和度生成：
 * 卡片用 surfaceContainerLow，保证比背景略深一点点而不显脏；
 * 文字色 onSurface 压到 0.10，保证白底上对比充足。
 */
fun buildLightScheme(seed: Color, neutralSeed: Color? = null): androidx.compose.material3.ColorScheme {
    val neutral = neutralSeed ?: seed
    val primary = seed.tone(0.42f)
    val primaryContainer = seed.tone(0.90f)
    val secondary = neutral.tone(0.46f)
    val secondaryContainer = neutral.tone(0.92f)
    val tertiary = seed.rotate(32f).tone(0.45f)
    val tertiaryContainer = seed.rotate(32f).tone(0.90f)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = seed.tone(0.20f),
        inversePrimary = seed.tone(0.80f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = neutral.tone(0.20f),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = seed.rotate(32f).tone(0.20f),
        // 背景与表面：几乎纯白，只带一丝种子色调
        background = neutral.tint(0.99f, 0.04f),
        onBackground = neutral.tone(0.10f),
        surface = neutral.tint(0.99f, 0.04f),
        onSurface = neutral.tone(0.10f),
        surfaceVariant = neutral.tint(0.93f, 0.08f),
        onSurfaceVariant = neutral.tone(0.32f),
        // 容器色阶：卡片/菜单/对话框分别取不同层级，避免整体发暗
        surfaceDim = neutral.tint(0.87f, 0.08f),
        surfaceBright = neutral.tint(1.0f, 0.02f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = neutral.tint(0.968f, 0.06f),
        surfaceContainer = neutral.tint(0.948f, 0.08f),
        surfaceContainerHigh = neutral.tint(0.925f, 0.09f),
        surfaceContainerHighest = neutral.tint(0.90f, 0.10f),
        inverseSurface = neutral.tone(0.20f),
        inverseOnSurface = neutral.tone(0.95f),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = neutral.tint(0.52f, 0.14f),
        outlineVariant = neutral.tint(0.80f, 0.10f),
        scrim = Color.Black,
        surfaceTint = primary
    )
}

/** 深色配色：同一色相下压低明度，容器色阶逐级抬升以区分层次 */
fun buildDarkScheme(seed: Color, neutralSeed: Color? = null): androidx.compose.material3.ColorScheme {
    val neutral = neutralSeed ?: seed
    val primary = seed.tone(0.80f)
    val primaryContainer = seed.tone(0.32f)
    val secondary = neutral.tone(0.76f)
    val secondaryContainer = neutral.tone(0.28f)
    val tertiary = seed.rotate(32f).tone(0.78f)
    val tertiaryContainer = seed.rotate(32f).tone(0.30f)
    return darkColorScheme(
        primary = primary,
        onPrimary = seed.tone(0.18f),
        primaryContainer = primaryContainer,
        onPrimaryContainer = seed.tone(0.90f),
        inversePrimary = seed.tone(0.40f),
        secondary = secondary,
        onSecondary = neutral.tone(0.16f),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = neutral.tone(0.90f),
        tertiary = tertiary,
        onTertiary = seed.rotate(32f).tone(0.16f),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = seed.rotate(32f).tone(0.90f),
        background = neutral.tint(0.07f, 0.06f),
        onBackground = neutral.tone(0.92f),
        surface = neutral.tint(0.07f, 0.06f),
        onSurface = neutral.tone(0.92f),
        surfaceVariant = neutral.tint(0.16f, 0.10f),
        onSurfaceVariant = neutral.tone(0.78f),
        surfaceDim = neutral.tint(0.06f, 0.06f),
        surfaceBright = neutral.tint(0.26f, 0.08f),
        surfaceContainerLowest = neutral.tint(0.05f, 0.05f),
        surfaceContainerLow = neutral.tint(0.10f, 0.07f),
        surfaceContainer = neutral.tint(0.13f, 0.08f),
        surfaceContainerHigh = neutral.tint(0.18f, 0.09f),
        surfaceContainerHighest = neutral.tint(0.22f, 0.10f),
        inverseSurface = neutral.tone(0.92f),
        inverseOnSurface = neutral.tone(0.12f),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        outline = neutral.tint(0.45f, 0.12f),
        outlineVariant = neutral.tint(0.28f, 0.10f),
        scrim = Color.Black,
        surfaceTint = primary
    )
}

/** 取 HSV 并写入目标明度，保留色相与饱和度 */
private fun Color.tone(lightness: Float): Color {
    val hsv = toHsv()
    return fromHsv(hsv[0], hsv[1], lightness)
}

/**
 * 生成中性表面色：保留种子色的色相，把饱和度压到 [saturation]，
 * 明度设为 [lightness]。这样背景/卡片带一点主题色气息而不显脏。
 */
private fun Color.tint(lightness: Float, saturation: Float): Color {
    val hsv = toHsv()
    return fromHsv(hsv[0], hsv[1] * saturation, lightness)
}

/** 旋转色相，用于派生三级色，避免主/次/三级同色 */
private fun Color.rotate(degrees: Float): Color {
    val hsv = toHsv()
    return fromHsv((hsv[0] + degrees) % 360f, hsv[1], hsv[2])
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt().coerceIn(0, 255),
        (green * 255).toInt().coerceIn(0, 255),
        (blue * 255).toInt().coerceIn(0, 255),
        hsv
    )
    return hsv
}

private fun fromHsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

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

/** 预设的中性色基准，让表面色更贴合预设气质（缺省时用种子色） */
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

/** 解析用户保存的自定义颜色（#RRGGBB 或 #AARRGGBB）；非法时回退 null */
fun parseColorOrNull(value: String): Color? = runCatching {
    val hex = value.trim().removePrefix("#")
    val argb = when (hex.length) {
        6 -> 0xFF000000.toInt() or hex.toLong(16).toInt()
        8 -> hex.toLong(16).toInt()
        else -> return null
    }
    Color(argb)
}.getOrNull()

/** 格式化为 #AARRGGBB，与参考图展示的写法一致 */
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
