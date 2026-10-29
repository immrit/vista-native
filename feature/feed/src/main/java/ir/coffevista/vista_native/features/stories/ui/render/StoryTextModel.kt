package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.features.stories.domain.StoryElement

internal enum class StoryFontStyle(
    val id: String,
    val label: String,
    val family: FontFamily,
    val weight: FontWeight,
    val uppercaseLatin: Boolean = false,
    val letterSpacingEm: Float = 0f,
) {
    Modern("modern", "مدرن", VistaFontFamily, FontWeight.SemiBold),
    Classic("classic", "کلاسیک", FontFamily.Serif, FontWeight.Bold),
    Neon("neon", "نئون", VistaFontFamily, FontWeight.Medium, letterSpacingEm = 0.02f),
    Typewriter("typewriter", "ماشین‌تحریر", FontFamily.Monospace, FontWeight.Medium),
    Strong("strong", "قوی", VistaFontFamily, FontWeight.Black, uppercaseLatin = true, letterSpacingEm = 0.02f),
    Marker("marker", "ماژیک", FontFamily.Cursive, FontWeight.Bold);

    companion object {
        fun fromId(id: String?): StoryFontStyle = entries.firstOrNull { it.id == id } ?: Modern
    }
}

internal enum class StoryTextBackground(val id: String) {
    None("none"),
    Solid("solid"),
    Soft("soft"),
    Outline("outline");

    fun next(): StoryTextBackground = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): StoryTextBackground? = entries.firstOrNull { it.id == id }
    }
}

internal enum class StoryTextAlign(val id: String) {
    Left("left"), Center("center"), Right("right");

    fun next(): StoryTextAlign = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): StoryTextAlign = entries.firstOrNull { it.id == id } ?: Center
    }
}

internal enum class StoryTextGradient(val id: String, val colors: List<Color>, val radial: Boolean) {
    Sunset("sunset", listOf(Color(0xFFFFC371), Color(0xFFFF5F6D)), false),
    Ocean("ocean", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)), false),
    Aurora("aurora", listOf(Color(0xFF00F5A0), Color(0xFF00D9F5), Color(0xFF7B61FF)), false),
    Gold("gold", listOf(Color(0xFFF7971E), Color(0xFFFFD200)), false),
    Vista("vista", listOf(Color(0xFF6366F1), Color(0xFFEC4899)), false),
    Candy("candy", listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4), Color(0xFFA18CD1)), false),
    Fire("fire", listOf(Color(0xFFFFF176), Color(0xFFFF7043), Color(0xFFD81B60)), true),
    Lagoon("lagoon", listOf(Color(0xFFE0FFFF), Color(0xFF4DD0E1), Color(0xFF3949AB)), true);

    fun brush(): Brush = if (radial) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader = RadialGradientShader(
                center = size.center,
                radius = (maxOf(size.width, size.height) / 2f).coerceAtLeast(1f),
                colors = colors,
            )
        }
    } else {
        Brush.linearGradient(colors)
    }

    companion object {
        fun fromId(id: String?): StoryTextGradient? = entries.firstOrNull { it.id == id }
    }
}

internal object StoryPalette {
    val presets: List<Color> = listOf(
        Color.White,
        Color.Black,
        Color(0xFFFF3B30),
        Color(0xFFFF9500),
        Color(0xFFFFCC00),
        Color(0xFF34C759),
        Color(0xFF00C7BE),
        Color(0xFF32ADE6),
        Color(0xFF007AFF),
        Color(0xFF5856D6),
        Color(0xFFAF52DE),
        Color(0xFFFF2D55),
        Color(0xFFA2845E),
        Color(0xFF8E8E93),
    )

    fun contrastOf(color: Color): Color = if (color.luminance() > 0.55f) Color.Black else Color.White
}

@Immutable
internal data class StoryTextSpec(
    val text: String,
    val fontSize: Float = DefaultFontSize,
    val fontStyle: StoryFontStyle = StoryFontStyle.Modern,
    val background: StoryTextBackground = StoryTextBackground.None,
    val align: StoryTextAlign = StoryTextAlign.Center,
    val color: Color = Color.White,
    val backgroundColor: Color = DefaultSoftBackground,
    val gradient: StoryTextGradient? = null,
) {
    companion object {
        const val DefaultFontSize = 28f
        const val MinFontSize = 14f
        const val MaxFontSize = 64f
        const val MaxLength = 500
        val DefaultSoftBackground = Color.Black.copy(alpha = 0.55f)
    }
}

internal fun StoryElement.toTextSpec(): StoryTextSpec = StoryTextSpec(
    text = content.orEmpty(),
    fontSize = (fontSize ?: StoryTextSpec.DefaultFontSize).coerceIn(StoryTextSpec.MinFontSize, StoryTextSpec.MaxFontSize),
    fontStyle = StoryFontStyle.fromId(textStyle),
    background = StoryTextBackground.fromId(textBackground) ?: StoryTextBackground.Soft,
    align = StoryTextAlign.fromId(textAlign),
    color = parseStoryColor(color, Color.White),
    backgroundColor = parseStoryColor(backgroundColor, StoryTextSpec.DefaultSoftBackground),
    gradient = StoryTextGradient.fromId(textGradient),
)

internal fun StoryTextSpec.applyTo(element: StoryElement): StoryElement = element.copy(
    type = "text",
    content = text,
    fontSize = fontSize,
    textStyle = fontStyle.id,
    textBackground = background.id,
    textAlign = align.id,
    color = color.toStoryHex(),
    backgroundColor = backgroundColor.toStoryHex(),
    textGradient = gradient?.id,
)

internal fun parseStoryColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    val clean = hex.trim().removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color((0xFF000000L or value).toInt())
        8 -> Color(value.toInt())
        else -> fallback
    }
}

internal fun Color.toStoryHex(): String = "#%08X".format(toArgb())
