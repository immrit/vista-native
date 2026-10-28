package ir.coffevista.vista_native.features.stories.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import kotlin.math.roundToInt

@Composable
fun StoryElementsOverlay(
    elements: List<StoryElement>,
    isAuthor: Boolean = false,
    onPollVote: (elementId: String, optionIndex: Int) -> Unit = { _, _ -> },
    onQuestionAnswer: (elementId: String, answer: String) -> Unit = { _, _ -> },
    onViewQuestionAnswers: (elementId: String) -> Unit = {},
    onLinkClick: (url: String) -> Unit = {},
    onMentionClick: (userId: String) -> Unit = {},
    onHashtagClick: (tag: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        elements.forEach { element ->
            when (element.type) {
                "question" -> {
                    var showAnswerDialog by remember { mutableStateOf(false) }
                    var answerText by remember { mutableStateOf("") }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = element.content?.ifBlank { null } ?: "از من بپرسید",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (isAuthor) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VistaBrandColors.Indigo)
                                        .clickable { onViewQuestionAnswers(element.id) }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.QuestionAnswer,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "مشاهده پاسخ‌های دریافتی",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .clickable { showAnswerDialog = true }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = "پاسخ خود را بنویسید…",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }

                    if (showAnswerDialog) {
                        AlertDialog(
                            onDismissRequest = { showAnswerDialog = false },
                            containerColor = Color(0xFF2A2A2A),
                            title = {
                                Text(
                                    text = element.content?.ifBlank { null } ?: "پاسخ به سوال",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            text = {
                                OutlinedTextField(
                                    value = answerText,
                                    onValueChange = { answerText = it },
                                    placeholder = { Text("پاسخ خود را اینجا بنویسید…", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (answerText.isNotBlank()) {
                                            onQuestionAnswer(element.id, answerText)
                                            answerText = ""
                                            showAnswerDialog = false
                                        }
                                    },
                                ) {
                                    Text("ارسال", color = VistaBrandColors.Pink)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAnswerDialog = false }) {
                                    Text("انصراف", color = Color.White)
                                }
                            },
                        )
                    }
                }

                "poll" -> {
                    element.poll?.let { poll ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                        ) {
                            StoryPollWidget(
                                poll = poll,
                                onVote = { optionIndex -> onPollVote(element.id, optionIndex) },
                            )
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

                "music" -> {
                    element.music?.let { music ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) },
                        ) {
                            StoryMusicStickerWidget(music = music)
                        }
                    }
                }

                "countdown" -> {
                    val parts = element.content?.split("|").orEmpty()
                    val title = parts.getOrNull(0) ?: "شمارش معکوس"
                    val targetMs = parts.getOrNull(1)?.toLongOrNull() ?: (System.currentTimeMillis() + 86400000L)
                    val remainingMs = (targetMs - System.currentTimeMillis()).coerceAtLeast(0L)
                    val hours = (remainingMs / (1000 * 60 * 60))
                    val minutes = (remainingMs / (1000 * 60)) % 60
                    val seconds = (remainingMs / 1000) % 60
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                CountdownDigitBox("%02d".format(hours), "ساعت")
                                Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                CountdownDigitBox("%02d".format(minutes), "دقیقه")
                                Text(":", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                CountdownDigitBox("%02d".format(seconds), "ثانیه")
                            }
                        }
                    }
                }

                "weather" -> {
                    val parts = element.content?.split("|").orEmpty()
                    val city = parts.getOrNull(0) ?: "تهران"
                    val temp = parts.getOrNull(1) ?: "۲۴°C"
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(temp, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(city, color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }

                "date" -> {
                    val dateText = element.content?.ifBlank { null } ?: "امروز"
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = VistaBrandColors.Pink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dateText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                "hashtag" -> {
                    val tag = element.content.orEmpty()
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(VistaBrandColors.Indigo, VistaBrandColors.Pink),
                                ),
                            )
                            .clickable { onHashtagClick(tag) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text("#$tag", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                "gif" -> {
                    val gifUrl = element.content.orEmpty()
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) },
                    ) {
                        AsyncImage(
                            model = gifUrl,
                            contentDescription = "استیکر گیف",
                            modifier = Modifier.size(120.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                "drawing" -> {
                    DrawingLayer(drawingData = element.content.orEmpty(), modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun StoryPollWidget(
    poll: StoryPoll,
    onVote: (optionIndex: Int) -> Unit,
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

        poll.options.forEachIndexed { index, option ->
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
                            Modifier.clickable { onVote(index) }
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

@Composable
fun StoryMusicStickerWidget(
    music: StoryMusic,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f)),
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = VistaBrandColors.Pink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = music.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = VistaBrandColors.Pink,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CountdownDigitBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF212121))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color(0xFFBDBDBD), fontSize = 10.sp)
    }
}

@Composable
fun DrawingLayer(
    drawingData: String,
    modifier: Modifier = Modifier,
) {
    if (drawingData.isBlank()) return
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        drawingData.split("|").forEach { strokePart ->
            val strokeTokens = strokePart.split(":")
            if (strokeTokens.size >= 3) {
                val color = runCatching { Color(android.graphics.Color.parseColor(strokeTokens[0])) }.getOrDefault(Color.White)
                val strokeWidth = strokeTokens[1].toFloatOrNull() ?: 8f
                val pointsRaw = strokeTokens[2].split(";")
                if (pointsRaw.size > 1) {
                    val path = Path()
                    var isFirst = true
                    pointsRaw.forEach { pointStr ->
                        val coords = pointStr.split(",")
                        if (coords.size == 2) {
                            val x = (coords[0].toFloatOrNull() ?: 0f) * width
                            val y = (coords[1].toFloatOrNull() ?: 0f) * height
                            if (isFirst) {
                                path.moveTo(x, y)
                                isFirst = false
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

