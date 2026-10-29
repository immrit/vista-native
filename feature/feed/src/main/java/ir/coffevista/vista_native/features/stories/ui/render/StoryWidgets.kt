package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal data class WidgetPalette(
    val containerBrush: Brush?,
    val containerColor: Color,
    val content: Color,
    val secondary: Color,
    val accent: Color,
    val border: Color?,
) {
    val field: Color get() = content.copy(alpha = if (border != null || containerBrush != null) 0.18f else 0.08f)
}

internal const val WidgetVariantCount = 4

internal fun widgetPalette(variant: Int): WidgetPalette = when (Math.floorMod(variant, WidgetVariantCount)) {
    0 -> WidgetPalette(null, Color.White, Color(0xFF111111), Color(0xFF6B7280), VistaBrandColors.Pink, null)
    1 -> WidgetPalette(null, Color.Black.copy(alpha = 0.62f), Color.White, Color.White.copy(alpha = 0.72f), VistaBrandColors.Pink, null)
    2 -> WidgetPalette(
        Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet, VistaBrandColors.Pink)),
        VistaBrandColors.Indigo, Color.White, Color.White.copy(alpha = 0.82f), Color.White, null,
    )
    else -> WidgetPalette(null, Color.Transparent, Color.White, Color.White.copy(alpha = 0.78f), Color.White, Color.White)
}

private fun Modifier.surface(palette: WidgetPalette, shape: Shape): Modifier {
    var m = this
    m = if (palette.containerBrush != null) m.background(palette.containerBrush, shape) else m.background(palette.containerColor, shape)
    palette.border?.let { m = m.border(2.dp, it, shape) }
    return m
}

private fun baseText(size: Float, weight: FontWeight, color: Color) = TextStyle(
    fontFamily = VistaFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    textDirection = TextDirection.Content,
)

@Composable
private fun WidgetText(
    text: String,
    palette: WidgetPalette,
    size: Float = 17f,
    weight: FontWeight = FontWeight.Bold,
    secondary: Boolean = false,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = baseText(size, weight, if (secondary) palette.secondary else palette.content).copy(textAlign = align),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun StoryPillWidget(
    text: String,
    variant: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: String? = null,
) {
    val palette = widgetPalette(variant)
    Row(
        modifier = modifier
            .surface(palette, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        WidgetText(text, palette)
        if (trailing != null) WidgetText(trailing, palette, size = 13f, weight = FontWeight.Medium, secondary = true)
    }
}

internal val LocationIcon: ImageVector get() = Icons.Default.LocationOn
internal val LinkIcon: ImageVector get() = Icons.Default.Link
internal val DateIcon: ImageVector get() = Icons.Default.CalendarMonth
internal val WeatherIcon: ImageVector get() = Icons.Default.WbSunny

@Composable
private fun rememberTickingNow(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(1_000L)
        value = System.currentTimeMillis()
    }
}

@Composable
internal fun StoryCountdownWidget(
    title: String,
    targetMs: Long,
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val palette = widgetPalette(variant)
    val now by rememberTickingNow()
    val remaining = (targetMs - now).coerceAtLeast(0L)
    val days = remaining / 86_400_000L
    val hours = remaining / 3_600_000L % 24
    val minutes = remaining / 60_000L % 60
    val seconds = remaining / 1_000L % 60
    Column(
        modifier = modifier
            .width(250.dp)
            .surface(palette, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            WidgetText(title, palette, size = 15f, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.height(10.dp))
        if (remaining == 0L) {
            WidgetText("پایان یافت", palette, size = 16f)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (days > 0) CountdownCell(days, "روز", palette)
                CountdownCell(hours, "ساعت", palette)
                CountdownCell(minutes, "دقیقه", palette)
                CountdownCell(seconds, "ثانیه", palette)
            }
        }
    }
}

@Composable
private fun CountdownCell(value: Long, label: String, palette: WidgetPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(palette.field)
                .padding(horizontal = 9.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetText("%02d".format(value).toPersianDigits(), palette, size = 20f, weight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(3.dp))
        WidgetText(label, palette, size = 10f, weight = FontWeight.Medium, secondary = true)
    }
}

@Composable
internal fun StoryQuestionWidget(
    prompt: String,
    variant: Int,
    modifier: Modifier = Modifier,
    isAuthor: Boolean = false,
    onAnswerClick: (() -> Unit)? = null,
    onViewAnswers: (() -> Unit)? = null,
) {
    val palette = widgetPalette(variant)
    val shape = RoundedCornerShape(22.dp)
    Column(modifier = modifier.width(272.dp).surface(palette, shape).clip(shape)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (palette.border != null) Brush.linearGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.14f)))
                    else Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet, VistaBrandColors.Pink)),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetText(prompt, WidgetPalette(null, Color.Unspecified, Color.White, Color.White, Color.White, null), size = 17f, maxLines = 3)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (isAuthor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.field)
                        .then(if (onViewAnswers != null) Modifier.clickable(onClick = onViewAnswers) else Modifier)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    WidgetText("مشاهده پاسخ‌ها", palette, size = 14f)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.field)
                        .then(if (onAnswerClick != null) Modifier.clickable(onClick = onAnswerClick) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    WidgetText("پاسخ خود را بنویسید…", palette, size = 14f, weight = FontWeight.Medium, secondary = true, align = TextAlign.Start)
                }
            }
        }
    }
}

@Composable
internal fun StoryPollWidget(
    poll: StoryPoll,
    variant: Int,
    modifier: Modifier = Modifier,
    isAuthor: Boolean = false,
    onVote: ((optionIndex: Int) -> Unit)? = null,
) {
    val palette = widgetPalette(variant)
    val shape = RoundedCornerShape(22.dp)
    val hasVoted = poll.userVotedOptionId != null || poll.options.any { it.isVoted }
    val showResults = hasVoted || isAuthor
    val total = maxOf(poll.options.sumOf { it.votesCount }, poll.totalVotes, 1)

    Column(
        modifier = modifier
            .width(272.dp)
            .surface(palette, shape)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WidgetText(poll.question, palette, size = 17f, maxLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        val sideBySide = poll.options.size == 2
        val cell: @Composable (Int, Modifier) -> Unit = { index, cellModifier ->
            val option = poll.options[index]
            val fraction = if (showResults) option.votesCount.toFloat() / total else 0f
            val mine = option.isVoted || option.id == poll.userVotedOptionId
            Box(
                modifier = cellModifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.field)
                    .then(if (!showResults && onVote != null) Modifier.clickable { onVote(index) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (showResults) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                            .background(if (mine) palette.accent.copy(alpha = 0.45f) else palette.content.copy(alpha = 0.16f)),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WidgetText(option.text, palette, size = 15f, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp))
                    if (showResults) {
                        WidgetText("${(fraction * 100).roundToInt().toPersianDigits()}٪", palette, size = 12f, weight = FontWeight.Medium, secondary = true)
                    }
                }
            }
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cell(0, Modifier.weight(1f))
                cell(1, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                poll.options.indices.forEach { cell(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
fun StoryMusicStickerWidget(
    music: StoryMusic,
    modifier: Modifier = Modifier,
    variant: Int = 1,
) {
    val palette = widgetPalette(variant)
    Row(
        modifier = modifier
            .width(264.dp)
            .surface(palette, RoundedCornerShape(18.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(11.dp)).background(palette.field),
            contentAlignment = Alignment.Center,
        ) {
            if (!music.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = music.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            WidgetText(music.title, palette, size = 14f, align = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            WidgetText(music.artist, palette, size = 12f, weight = FontWeight.Medium, secondary = true, align = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(palette.field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun StoryGifWidget(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = "استیکر گیف",
        modifier = modifier.size(150.dp),
        contentScale = ContentScale.Fit,
    )
}
