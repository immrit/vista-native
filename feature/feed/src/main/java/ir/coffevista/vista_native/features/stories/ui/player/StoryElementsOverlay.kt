package ir.coffevista.vista_native.features.stories.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import kotlin.math.roundToInt

@Composable
fun StoryElementsOverlay(
    elements: List<StoryElement>,
    onPollVote: (optionId: String) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onMentionClick: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        elements.forEach { element ->
            when (element.type) {
                "poll" -> {
                    element.poll?.let { poll ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                        ) {
                            StoryPollWidget(poll = poll, onVote = onPollVote)
                        }
                    }
                }

                "location" -> {
                    element.location?.let { loc ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = VistaBrandColors.Pink,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = loc.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                "link" -> {
                    element.link?.let { link ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .clickable { onLinkClick(link.url) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = VistaBrandColors.Indigo,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = link.title ?: link.url,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                "mention" -> {
                    element.mention?.let { mention ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { onMentionClick(mention.userId) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "@${mention.username}",
                                color = VistaBrandColors.Pink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                "text" -> {
                    element.content?.let { text ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    element.backgroundColor?.let { parseHexColor(it) }
                                        ?: Color.Black.copy(alpha = 0.5f),
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = text,
                                color = element.color?.let { parseHexColor(it) } ?: Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryPollWidget(
    poll: StoryPoll,
    onVote: (optionId: String) -> Unit,
) {
    val totalVotes = poll.totalVotes.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = poll.question,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(14.dp))

        poll.options.forEach { option ->
            val hasVoted = poll.userVotedOptionId != null || option.isVoted
            val percentage = if (hasVoted) (option.votesCount.toFloat() / totalVotes * 100).toInt() else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0))
                    .then(
                        if (!hasVoted) {
                            Modifier.clickable { onVote(option.id) }
                        } else Modifier,
                    ),
            ) {
                if (hasVoted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(fraction = (option.votesCount.toFloat() / totalVotes).coerceIn(0.05f, 1f))
                            .background(
                                if (option.id == poll.userVotedOptionId) VistaBrandColors.Indigo.copy(alpha = 0.35f)
                                else Color.LightGray.copy(alpha = 0.5f),
                            ),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                    )
                    if (hasVoted) {
                        Text(
                            text = "$percentage٪",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color.White
    }
}
