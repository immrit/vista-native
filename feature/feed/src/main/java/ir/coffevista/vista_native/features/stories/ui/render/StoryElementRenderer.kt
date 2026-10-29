package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.canvas.StageSize
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransform
import ir.coffevista.vista_native.features.stories.ui.canvas.resolvedTransform
import java.net.URI

internal data class StoryWidgetActions(
    val isAuthor: Boolean = false,
    val onPollVote: (elementId: String, optionIndex: Int) -> Unit = { _, _ -> },
    val onQuestionClick: (element: StoryElement) -> Unit = {},
    val onViewQuestionAnswers: (elementId: String) -> Unit = {},
    val onLinkClick: (url: String) -> Unit = {},
    val onMentionClick: (userId: String) -> Unit = {},
    val onHashtagClick: (tag: String) -> Unit = {},
)

internal class ElementVisualState(
    val transform: () -> StoryTransform,
    val extraScale: () -> Float = { 1f },
    val alpha: () -> Float = { 1f },
)

private const val MaxElementWidthFraction = 0.9f

internal fun Modifier.storyPlacement(stage: StageSize, visual: ElementVisualState): Modifier =
    this
        .layout { measurable, _ ->
            val placeable = measurable.measure(
                Constraints(
                    maxWidth = (stage.width * MaxElementWidthFraction).toInt().coerceAtLeast(1),
                    maxHeight = stage.height.toInt().coerceAtLeast(1),
                ),
            )
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .graphicsLayer {
            val t = visual.transform()
            val s = t.scale * visual.extraScale()
            translationX = t.cx * stage.width - size.width / 2f
            translationY = t.cy * stage.height - size.height / 2f
            scaleX = s
            scaleY = s
            rotationZ = t.rotation
            alpha = visual.alpha()
        }

@Composable
internal fun BoxScope.StoryElementLayers(
    elements: List<StoryElement>,
    stage: StageSize,
    actions: StoryWidgetActions? = null,
    visualFor: (StoryElement) -> ElementVisualState = { element ->
        val resolved = element.resolvedTransform()
        ElementVisualState(transform = { resolved })
    },
    onSizeChanged: (id: String, size: IntSize) -> Unit = { _, _ -> },
) {
    elements.filter { it.type == "drawing" }.forEach { drawing ->
        key(drawing.id) {
            val strokes = remember(drawing.content) { StoryDrawingCodec.decode(drawing.content) }
            StoryDrawingLayer(strokes = strokes, modifier = Modifier.fillMaxSize())
        }
    }
    elements.forEach { element ->
        if (element.type == "drawing") return@forEach
        key(element.id) {
            val visual = remember(element, stage) { visualFor(element) }
            Box(
                modifier = Modifier
                    .onSizeChanged { onSizeChanged(element.id, it) }
                    .storyPlacement(stage, visual),
            ) {
                StoryElementContent(element = element, actions = actions)
            }
        }
    }
}

@Composable
internal fun StoryElementContent(
    element: StoryElement,
    actions: StoryWidgetActions?,
) {
    val variant = effectiveVariant(element)
    val interactive = actions != null
    when (element.type) {
        "text" -> StoryRichText(spec = element.toTextSpec())

        "poll" -> element.poll?.let { poll ->
            StoryPollWidget(
                poll = poll,
                variant = variant,
                isAuthor = actions?.isAuthor == true,
                onVote = if (interactive) ({ index -> actions?.onPollVote?.invoke(element.id, index) }) else null,
            )
        }

        "question" -> StoryQuestionWidget(
            prompt = element.content?.ifBlank { null } ?: "از من بپرسید",
            variant = variant,
            isAuthor = actions?.isAuthor == true,
            onAnswerClick = if (interactive) ({ actions?.onQuestionClick?.invoke(element) }) else null,
            onViewAnswers = if (interactive) ({ actions?.onViewQuestionAnswers?.invoke(element.id) }) else null,
        )

        "mention" -> element.mention?.let { mention ->
            StoryPillWidget(
                text = "@${mention.username}",
                variant = variant,
                modifier = clickableIf(interactive) { actions?.onMentionClick?.invoke(mention.userId) },
            )
        }

        "hashtag" -> {
            val tag = element.content.orEmpty()
            StoryPillWidget(
                text = "#$tag",
                variant = variant,
                modifier = clickableIf(interactive) { actions?.onHashtagClick?.invoke(tag) },
            )
        }

        "location" -> element.location?.let { StoryPillWidget(text = it.name, variant = variant, icon = LocationIcon) }

        "link" -> element.link?.let { link ->
            StoryPillWidget(
                text = link.title?.ifBlank { null } ?: displayHost(link.url),
                variant = variant,
                icon = LinkIcon,
                modifier = clickableIf(interactive) { actions?.onLinkClick?.invoke(link.url) },
            )
        }

        "date" -> StoryPillWidget(text = element.content?.ifBlank { null } ?: "امروز", variant = variant, icon = DateIcon)

        "weather" -> {
            val parts = element.content.orEmpty().split("|")
            StoryPillWidget(
                text = parts.getOrNull(1) ?: "۲۴°C",
                trailing = parts.getOrNull(0) ?: "تهران",
                variant = variant,
                icon = WeatherIcon,
            )
        }

        "countdown" -> {
            val parts = element.content.orEmpty().split("|")
            StoryCountdownWidget(
                title = parts.getOrNull(0)?.ifBlank { null } ?: "شمارش معکوس",
                targetMs = parts.getOrNull(1)?.toLongOrNull() ?: (System.currentTimeMillis() + 86_400_000L),
                variant = variant,
            )
        }

        "music" -> element.music?.let { StoryMusicStickerWidget(music = it, variant = variant) }

        "gif" -> StoryGifWidget(url = element.content.orEmpty())

        else -> Unit
    }
}

private fun clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) Modifier.clickable(onClick = onClick) else Modifier

internal fun effectiveVariant(element: StoryElement): Int {
    if (element.schema >= 2) return element.variant
    return when (element.type) {
        "link", "weather", "poll", "question" -> 0
        "hashtag" -> 2
        else -> 1
    }
}

private fun displayHost(url: String): String = runCatching {
    URI(url.trim()).host?.removePrefix("www.")
}.getOrNull()?.takeIf { it.isNotBlank() } ?: url
