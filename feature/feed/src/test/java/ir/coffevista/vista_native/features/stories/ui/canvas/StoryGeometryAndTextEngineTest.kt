package ir.coffevista.vista_native.features.stories.ui.canvas

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ir.coffevista.vista_native.features.stories.data.StoryDynamicContextService
import ir.coffevista.vista_native.features.stories.data.StoryLiveWeather
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryLocation
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerDto
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
import kotlinx.serialization.json.Json
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

    @Test
    fun `trash zone magnetic pull and scale interpolation match Telegram EntityView spec`() {
        val stage = StageSize(width = 1080f, height = 1920f)
        val trashZone = StoryTrashZone(stage)

        // Far away from trash zone -> no hover, proximity = 0f, scale multiplier = 1f
        assertFalse(trashZone.isHovered(540f, 500f))
        assertEquals(0f, trashZone.proximity(540f, 500f), 0.001f)
        assertEquals(1f, StoryTrashZone.interpolateScaleMultiplier(0f), 0.001f)

        // Exactly at trash center -> maximum proximity (1.0), scale down to ~0.30x
        assertTrue(trashZone.isHovered(trashZone.centerX, trashZone.centerY))
        assertEquals(1f, trashZone.proximity(trashZone.centerX, trashZone.centerY), 0.01f)
        assertEquals(0.30f, StoryTrashZone.interpolateScaleMultiplier(1f), 0.01f)

        // Inside magnetic field -> pulled closer toward trash center
        val nearTransform = StoryTransform(
            cx = (trashZone.centerX + 40f) / stage.width,
            cy = (trashZone.centerY - 40f) / stage.height,
            scale = 1f,
            rotation = 0f,
        )
        val pulled = trashZone.applyMagneticPull(nearTransform, trashProgress = 1f)
        assertTrue(pulled.cx < nearTransform.cx)
        assertTrue(pulled.cy > nearTransform.cy)
    }

    @Test
    fun `export matrix and normalized media area map screen coordinates to export space`() {
        val stage = StageSize(width = 540f, height = 960f)
        val transform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1.5f, rotation = 0f)
        val affine = StoryExportMatrix.computeAffineValues(
            transform = transform,
            entityWidthPx = 0f,
            entityHeightPx = 0f,
            uiStage = stage,
            exportWidth = 1080f,
            exportHeight = 1920f,
        )
        assertEquals(9, affine.size)
        // 1080/540 = 2.0 viewport-to-export ratio * 1.5 entity scale = 3.0 scaleX/scaleY at 0 deg
        assertEquals(3.0f, affine[0], 0.001f)
        assertEquals(3.0f, affine[4], 0.001f)
        assertEquals(540f, affine[2], 0.01f)
        assertEquals(960f, affine[5], 0.01f)

        val mediaArea = StoryExportMatrix.toNormalizedMediaArea(
            elementId = "loc-1",
            type = "location",
            transform = StoryTransform(cx = 0.5f, cy = 0.25f, scale = 1f, rotation = 15f),
            entityWidthPx = 270f,
            entityHeightPx = 96f,
            cornerRadiusPx = 27f,
            uiStage = stage,
        )
        assertEquals(50f, mediaArea.xPercent, 0.01f)
        assertEquals(25f, mediaArea.yPercent, 0.01f)
        assertEquals(50f, mediaArea.widthPercent, 0.01f)
        assertEquals(10f, mediaArea.heightPercent, 0.01f)
        assertEquals(15f, mediaArea.rotationDeg, 0.01f)
    }

    @Test
    fun `neon brush roundtrips through StoryDrawingCodec`() {
        val strokes = listOf(
            StoryStroke(
                brush = StoryBrush.Neon,
                argb = 0xFF00E5FF.toInt(),
                width = 14f,
                points = listOf(0.2f to 0.2f, 0.8f to 0.8f),
            ),
        )
        val encoded = StoryDrawingCodec.encode(strokes)
        assertTrue(encoded.startsWith("n:#FF00E5FF:14.00:"))
        val decoded = StoryDrawingCodec.decode(encoded)
        assertEquals(1, decoded.size)
        assertEquals(StoryBrush.Neon, decoded.first().brush)
    }

    @Test
    fun `compose sRGB Color toArgb preserves full alpha and channels`() {
        assertEquals(0xFFFFFFFF.toInt(), Color.White.toArgb())
        assertEquals(0xFFFF0055.toInt(), Color(0xFFFF0055).toArgb())
        assertEquals(0xFF0B0C10.toInt(), Color(0xFF0B0C10).toArgb())
    }

    @Test
    fun `story question answer dto resolves both flat backend fields and nested profile`() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "id": "ans_1",
              "story_id": "st_1",
              "element_id": "el_1",
              "answerer_id": "u_42",
              "answerer_username": "sara_dev",
              "answerer_avatar_url": "https://cdn.vista.ir/sara.jpg",
              "answer": "عالی بود",
              "created_at": "2026-10-30T10:00:00Z"
            }
        """.trimIndent()
        val dto = json.decodeFromString(StoryQuestionAnswerDto.serializer(), payload)
        assertEquals("sara_dev", dto.resolvedUsername)
        assertEquals("https://cdn.vista.ir/sara.jpg", dto.resolvedAvatarUrl)
        assertEquals("عالی بود", dto.answer)
    }

    @Test
    fun `create story request serializes music_title and v2 widget variants`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val req = CreateStoryRequestDto(
            mediaUrl = "https://cdn.vista.ir/story.jpg",
            mediaType = "image",
            musicUrl = "https://cdn.vista.ir/song.mp3",
            musicTitle = "آهنگ شب — خواننده",
            interactiveElements = listOf(
                StoryElement(
                    id = "w1",
                    type = "weather",
                    content = "تهران|۲۴°",
                    schema = 2,
                    xNorm = 0.5f,
                    yNorm = 0.3f,
                    variant = 3,
                ),
            ),
        )
        val encoded = json.encodeToString(CreateStoryRequestDto.serializer(), req)
        assertTrue(encoded.contains("\"music_title\":\"آهنگ شب — خواننده\""))
        assertTrue(encoded.contains("\"variant\":3"))
        assertTrue(encoded.contains("\"schema\":2"))
    }

    @Test
    fun `dynamic WMO weather codes map accurately to Persian conditions and day-night emojis`() {
        val clearDay = StoryDynamicContextService.mapWmoWeather(code = 0, isDay = true)
        assertEquals("آفتابی", clearDay.first)
        assertEquals("☀️", clearDay.second)

        val clearNight = StoryDynamicContextService.mapWmoWeather(code = 0, isDay = false)
        assertEquals("صاف شبانه", clearNight.first)
        assertEquals("🌙", clearNight.second)

        val rain = StoryDynamicContextService.mapWmoWeather(code = 63, isDay = true)
        assertEquals("بارانی", rain.first)
        assertEquals("🌧️", rain.second)

        val snow = StoryDynamicContextService.mapWmoWeather(code = 75, isDay = true)
        assertEquals("برفی", snow.first)
        assertEquals("❄️", snow.second)

        val storm = StoryDynamicContextService.mapWmoWeather(code = 95, isDay = false)
        assertEquals("رعدوبرق", storm.first)
        assertEquals("⛈️", storm.second)
    }

    @Test
    fun `live weather formats Celsius and Fahrenheit with Persian digits`() {
        val weather = StoryLiveWeather(
            city = "تهران",
            latitude = 35.6892,
            longitude = 51.3890,
            tempC = 25,
            feelsLikeC = 24,
            weatherCode = 0,
            isDay = true,
            conditionLabel = "آفتابی",
            conditionEmoji = "☀️",
        )
        assertEquals("۲۵°C", weather.formattedTemperature(celsius = true))
        assertEquals("۷۷°F", weather.formattedTemperature(celsius = false))

        val freezing = weather.copy(tempC = -3)
        assertEquals("−۳°C", freezing.formattedTemperature(celsius = true))
    }

    @Test
    fun `dynamic location coordinates and trimmed music chorus roundtrip through JSON`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val locElem = StoryElement(
            id = "loc_gps_1",
            type = "location",
            content = "ولنجک، تهران",
            location = StoryLocation(
                name = "ولنجک، تهران",
                latitude = 35.8042,
                longitude = 51.4018,
                address = "منطقه ۱، تهران",
            ),
            variant = 1,
        )
        val musicElem = StoryElement(
            id = "mus_local_1",
            type = "music",
            music = StoryMusic(
                musicUrl = "https://cdn.vista.ir/music/u1/chorus.m4a",
                title = "قطعه انتخابی من",
                artist = "کاربر ویستا",
                startMs = 15_000,
                endMs = 45_000,
            ),
            variant = 0,
        )

        val locDecoded = json.decodeFromString(
            StoryElement.serializer(),
            json.encodeToString(StoryElement.serializer(), locElem),
        )
        assertNotNull(locDecoded.location)
        assertEquals(35.8042, locDecoded.location?.latitude ?: 0.0, 0.0001)
        assertEquals(51.4018, locDecoded.location?.longitude ?: 0.0, 0.0001)
        assertEquals("منطقه ۱، تهران", locDecoded.location?.address)

        val musicDecoded = json.decodeFromString(
            StoryElement.serializer(),
            json.encodeToString(StoryElement.serializer(), musicElem),
        )
        assertNotNull(musicDecoded.music)
        assertEquals(15_000, musicDecoded.music?.startMs)
        assertEquals(45_000, musicDecoded.music?.endMs)
    }
}
