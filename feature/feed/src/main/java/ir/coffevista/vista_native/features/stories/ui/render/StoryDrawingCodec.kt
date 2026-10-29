package ir.coffevista.vista_native.features.stories.ui.render

import kotlin.math.max

internal enum class StoryBrush(val code: String) {
    Pen("p"),
    Marker("m"),
    Eraser("e");

    companion object {
        fun fromCode(code: String): StoryBrush? = entries.firstOrNull { it.code == code }
    }
}

internal data class StoryStroke(
    val brush: StoryBrush,
    val argb: Int,
    val width: Float,
    val points: List<Pair<Float, Float>>,
)

internal object StoryDrawingCodec {
    private const val LEGACY_PX_PER_UNIT = 3f
    private const val MAX_STROKES = 400
    private const val MAX_POINTS = 4000

    fun encode(strokes: List<StoryStroke>): String = strokes.joinToString("|") { stroke ->
        val points = stroke.points.joinToString(";") { (x, y) -> "%.4f,%.4f".format(java.util.Locale.US, x, y) }
        "${stroke.brush.code}:#%08X:%.2f:$points".format(java.util.Locale.US, stroke.argb, stroke.width)
    }

    fun decode(raw: String?): List<StoryStroke> {
        if (raw.isNullOrBlank()) return emptyList()
        val result = ArrayList<StoryStroke>()
        for (part in raw.split("|")) {
            if (result.size >= MAX_STROKES) break
            val tokens = part.split(":")
            val isV2 = tokens.size >= 4 && StoryBrush.fromCode(tokens[0]) != null
            val brush: StoryBrush
            val colorToken: String
            val widthToken: String
            val pointsToken: String
            when {
                isV2 -> {
                    brush = StoryBrush.fromCode(tokens[0])!!
                    colorToken = tokens[1]
                    widthToken = tokens[2]
                    pointsToken = tokens[3]
                }
                tokens.size >= 3 -> {
                    brush = StoryBrush.Pen
                    colorToken = tokens[0]
                    widthToken = tokens[1]
                    pointsToken = tokens[2]
                }
                else -> continue
            }
            val points = parsePoints(pointsToken)
            if (points.isEmpty()) continue
            val rawWidth = widthToken.toFloatOrNull() ?: continue
            val width = if (isV2) rawWidth else rawWidth / LEGACY_PX_PER_UNIT
            result += StoryStroke(
                brush = brush,
                argb = parseArgb(colorToken),
                width = max(0.5f, width),
                points = points,
            )
        }
        return result
    }

    private fun parsePoints(token: String): List<Pair<Float, Float>> {
        val out = ArrayList<Pair<Float, Float>>()
        for (pair in token.split(";")) {
            if (out.size >= MAX_POINTS) break
            val c = pair.split(",")
            if (c.size != 2) continue
            val x = c[0].toFloatOrNull() ?: continue
            val y = c[1].toFloatOrNull() ?: continue
            if (x.isFinite() && y.isFinite()) out += x to y
        }
        return out
    }

    private fun parseArgb(token: String): Int {
        val clean = token.trim().removePrefix("#")
        val value = clean.toLongOrNull(16) ?: return WHITE
        return when (clean.length) {
            6 -> (0xFF000000L or value).toInt()
            8 -> value.toInt()
            else -> WHITE
        }
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
}
