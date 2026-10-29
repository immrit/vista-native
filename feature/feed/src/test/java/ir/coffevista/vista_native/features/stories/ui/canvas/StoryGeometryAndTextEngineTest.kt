package ir.coffevista.vista_native.features.stories.ui.canvas

import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.render.BadgeCommand
import ir.coffevista.vista_native.features.stories.ui.render.LineRect
import ir.coffevista.vista_native.features.stories.ui.render.StoryBrush
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingCodec
import ir.coffevista.vista_native.features.stories.ui.render.StoryFontStyle
import ir.coffevista.vista_native.features.stories.ui.render.StoryLineBadge
import ir.coffevista.vista_native.features.stories.ui.render.StoryStroke
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextAlign
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextBackground
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextSpec
import ir.coffevista.vista_native.features.stories.ui.render.applyTo
import ir.coffevista.vista_native.features.stories.ui.render.toTextSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryGeometryAndTextEngineTest {

    @Test
    fun `legacy story element with legacy offsets maps correctly to stage transform`() {
        val legacy = StoryElement(
            id = "legacy-1",
            type = "text",
            content = "سلام ویستا",
            x = 0f,
            y = 0f,
        )
        val transform = LegacyStoryMapper.resolve(legacy)
        assertEquals(0.5f, transform.cx, 0.001f)
        assertEquals(0.5f, transform.cy, 0.001f)
        assertEquals(1f, transform.scale, 0.001f)
        assertEquals(0f, transform.rotation, 0.001f)
    }

    @Test
    fun `v2 story element with normalized coordinates takes precedence over legacy fields`() {
        val v2Element = StoryElement(
            id = "v2-1",
            type = "text",
            content = "استوری مدرن",
            schema = 2,
            xNorm = 0.25f,
            yNorm = 0.75f,
            scale = 1.4f,
            rotation = 15f,
            x = -0.8f,
            y = 0.4f,
        )
        val transform = LegacyStoryMapper.resolve(v2Element)
        assertEquals(0.25f, transform.cx, 0.001f)
        assertEquals(0.75f, transform.cy, 0.001f)
        assertEquals(1.4f, transform.scale, 0.001f)
        assertEquals(15f, transform.rotation, 0.001f)
    }

    @Test
    fun `withTransform updates element and computes backward-compatible legacy coordinates`() {
        val element = StoryElement(id = "e1", type = "text", content = "تست")
        val updated = element.withTransform(StoryTransform(cx = 0.6f, cy = 0.4f, scale = 1.2f, rotation = 45f))

        assertEquals(2, updated.schema)
        assertEquals(0.6f, updated.xNorm ?: 0f, 0.001f)
        assertEquals(0.4f, updated.yNorm ?: 0f, 0.001f)
        assertEquals(1.2f, updated.scale, 0.001f)
        assertEquals(45f, updated.rotation, 0.001f)
        assertTrue(updated.x.isFinite())
        assertTrue(updated.y.isFinite())
    }

    @Test
    fun `angle normalization keeps degrees between -180 and 180`() {
        assertEquals(0f, StoryTransformMath.normalizeAngle(0f), 0.001f)
        assertEquals(90f, StoryTransformMath.normalizeAngle(90f), 0.001f)
        assertEquals(-90f, StoryTransformMath.normalizeAngle(270f), 0.001f)
        assertEquals(180f, StoryTransformMath.normalizeAngle(180f), 0.001f)
        assertEquals(180f, StoryTransformMath.normalizeAngle(-180f), 0.001f)
        assertEquals(45f, StoryTransformMath.normalizeAngle(405f), 0.001f)
        assertEquals(-45f, StoryTransformMath.normalizeAngle(-405f), 0.001f)
    }

    @Test
    fun `magnetic snap engine snaps to center within threshold`() {
        val stage = StageSize(width = 1080f, height = 1920f)
        val snapEngine = StorySnapEngine(stage)

        // Raw position slightly off center (538px vs 540px center)
        val raw = StoryTransform(cx = 538f / 1080f, cy = 962f / 1920f)
        val result = snapEngine.snap(
            raw = raw,
            halfW = 100f,
            halfH = 50f,
            previous = SnapState.None,
        )

        assertEquals(0.5f, result.transform.cx, 0.001f)
        assertEquals(0.5f, result.transform.cy, 0.001f)
        assertEquals(SnapTarget.Center, result.state.x)
        assertEquals(SnapTarget.Center, result.state.y)
        assertEquals(1, result.guides.vertical.size)
        assertEquals(1, result.guides.horizontal.size)
    }

    @Test
    fun `rotation detent snaps to 0, 45, 90, 180 degrees`() {
        val stage = StageSize(width = 1080f, height = 1920f)
        val snapEngine = StorySnapEngine(stage)

        val raw = StoryTransform(cx = 0.2f, cy = 0.2f, rotation = 2.5f)
        val result = snapEngine.snap(
            raw = raw,
            halfW = 100f,
            halfH = 50f,
            previous = SnapState.None,
        )

        assertEquals(0f, result.transform.rotation, 0.001f)
        assertEquals(0, result.state.rotationDetent)
        assertTrue(result.guides.rotationLocked)
    }

    @Test
    fun `multi-line badge builds closed path commands for text lines`() {
        val lines = listOf(
            LineRect(left = 20f, top = 10f, right = 200f, bottom = 60f),
            LineRect(left = 15f, top = 60f, right = 260f, bottom = 110f),
            LineRect(left = 30f, top = 110f, right = 180f, bottom = 160f),
        )
        val commands = StoryLineBadge.build(lines, radius = 16f)
        assertFalse(commands.isEmpty())
        assertTrue(commands.first() is BadgeCommand.MoveTo)
        assertTrue(commands.last() is BadgeCommand.Close)
    }

    @Test
    fun `drawing codec roundtrips strokes accurately`() {
        val strokes = listOf(
            StoryStroke(
                brush = StoryBrush.Pen,
                argb = 0xFFFF0055.toInt(),
                width = 8f,
                points = listOf(0.1f to 0.1f, 0.2f to 0.3f, 0.5f to 0.8f),
            ),
            StoryStroke(
                brush = StoryBrush.Marker,
                argb = 0xFF00FF88.toInt(),
                width = 16f,
                points = listOf(0.3f to 0.3f, 0.7f to 0.7f),
            ),
        )
        val encoded = StoryDrawingCodec.encode(strokes)
        assertTrue(encoded.startsWith("p:#FFFF0055:8.00:"))
        assertTrue(encoded.contains("|m:#FF00FF88:16.00:"))

        val decoded = StoryDrawingCodec.decode(encoded)
        assertEquals(2, decoded.size)
        assertEquals(StoryBrush.Pen, decoded[0].brush)
        assertEquals(0xFFFF0055.toInt(), decoded[0].argb)
        assertEquals(8f, decoded[0].width, 0.01f)
        assertEquals(3, decoded[0].points.size)

        assertEquals(StoryBrush.Marker, decoded[1].brush)
        assertEquals(0xFF00FF88.toInt(), decoded[1].argb)
        assertEquals(16f, decoded[1].width, 0.01f)
        assertEquals(2, decoded[1].points.size)
    }

    @Test
    fun `drawing codec handles legacy format without crashing`() {
        val legacy = "#FFFFFFFF:12.0:0.1,0.2;0.3,0.4"
        val decoded = StoryDrawingCodec.decode(legacy)
        assertEquals(1, decoded.size)
        assertEquals(StoryBrush.Pen, decoded[0].brush)
        assertEquals(4f, decoded[0].width, 0.01f) // scaled by legacy factor
        assertEquals(2, decoded[0].points.size)
    }

    @Test
    fun `story text spec converts to and from StoryElement without loss`() {
        val spec = StoryTextSpec(
            text = "متن نمونه برای استوری",
            fontSize = 32f,
            fontStyle = StoryFontStyle.Neon,
            background = StoryTextBackground.Solid,
            align = StoryTextAlign.Center,
        )
        val baseElem = StoryElement(id = "text-1", type = "text")
        val applied = spec.applyTo(baseElem)

        assertEquals("text", applied.type)
        assertEquals("متن نمونه برای استوری", applied.content)
        assertEquals(32f, applied.fontSize ?: 0f, 0.001f)
        assertEquals("neon", applied.textStyle)
        assertEquals("solid", applied.textBackground)
        assertEquals("center", applied.textAlign)

        val restoredSpec = applied.toTextSpec()
        assertEquals(spec.text, restoredSpec.text)
        assertEquals(spec.fontSize, restoredSpec.fontSize, 0.001f)
        assertEquals(spec.fontStyle, restoredSpec.fontStyle)
        assertEquals(spec.background, restoredSpec.background)
        assertEquals(spec.align, restoredSpec.align)
    }
}
