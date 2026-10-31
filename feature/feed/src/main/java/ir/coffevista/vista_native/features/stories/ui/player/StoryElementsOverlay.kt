package ir.coffevista.vista_native.features.stories.ui.player

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.data.StorySecurityGuard
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryLocation
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import ir.coffevista.vista_native.features.stories.ui.render.StoryElementLayers
import ir.coffevista.vista_native.features.stories.ui.render.StoryWidgetActions
import kotlinx.coroutines.delay

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
    onReactionClick: (emoji: String) -> Unit = {},
    onDialogVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var answeringQuestion by remember { mutableStateOf<StoryElement?>(null) }
    var activeLocation by remember { mutableStateOf<StoryLocation?>(null) }
    var pendingExternalLink by remember { mutableStateOf<String?>(null) }
    var activeReactionBurst by remember { mutableStateOf<String?>(null) }

    val anyModalOpen = answeringQuestion != null || activeLocation != null || pendingExternalLink != null
    LaunchedEffect(anyModalOpen) {
        onDialogVisibilityChanged(anyModalOpen)
    }

    LaunchedEffect(activeLocation) {
        if (activeLocation != null) {
            delay(4000L)
            activeLocation = null
        }
    }

    LaunchedEffect(activeReactionBurst) {
        if (activeReactionBurst != null) {
            delay(1100L)
            activeReactionBurst = null
        }
    }

    val actions = remember(
        isAuthor,
        onPollVote,
        onViewQuestionAnswers,
        onMentionClick,
        onHashtagClick,
        onReactionClick,
    ) {
        StoryWidgetActions(
            isAuthor = isAuthor,
            onPollVote = onPollVote,
            onQuestionClick = { answeringQuestion = it },
            onViewQuestionAnswers = onViewQuestionAnswers,
            onLinkClick = { rawUrl ->
                val safeUrl = StorySecurityGuard.normalizeSafeHttpUrl(rawUrl)
                if (safeUrl != null) {
                    pendingExternalLink = safeUrl
                }
            },
            onMentionClick = onMentionClick,
            onHashtagClick = onHashtagClick,
            onLocationClick = { loc -> activeLocation = loc },
            onReactionClick = { el ->
                val emoji = el.content?.ifBlank { null } ?: "❤️"
                activeReactionBurst = emoji
                onReactionClick(emoji)
            },
        )
    }

    StoryStage(modifier = modifier.fillMaxSize()) { stage ->
        StoryElementLayers(elements = elements, stage = stage, actions = actions)

        // Telegram StoryMediaAreasView Location Card & Map Launcher Overlay
        AnimatedVisibility(
            visible = activeLocation != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp),
        ) {
            val loc = activeLocation
            if (loc != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xF0151620))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                            .clickable {
                                val lat = loc.latitude
                                val lon = loc.longitude
                                val geoUri = if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                                    Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(loc.name)})")
                                } else {
                                    Uri.parse("geo:0,0?q=${Uri.encode(loc.name)}")
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                                activeLocation = null
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFFF4B55),
                            modifier = Modifier.size(19.dp),
                        )
                        Column {
                            Text(
                                text = loc.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = loc.address?.takeIf { it.isNotBlank() } ?: "مشاهده روی نقشه ←",
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeReactionBurst != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = activeReactionBurst.orEmpty(),
                fontSize = 76.sp,
            )
        }
    }

    answeringQuestion?.let { question ->
        QuestionAnswerDialog(
            prompt = question.content?.ifBlank { null } ?: "پاسخ به سوال",
            onDismiss = { answeringQuestion = null },
            onSubmit = { answer ->
                onQuestionAnswer(question.id, answer)
                answeringQuestion = null
            },
        )
    }

    pendingExternalLink?.let { safeUrl ->
        ExternalLinkConfirmationDialog(
            url = safeUrl,
            onDismiss = { pendingExternalLink = null },
            onConfirmOpen = {
                pendingExternalLink = null
                val openedExternally = runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    true
                }.getOrDefault(false)
                if (!openedExternally) {
                    onLinkClick(safeUrl)
                }
            },
        )
    }
}

@Composable
private fun ExternalLinkConfirmationDialog(
    url: String,
    onDismiss: () -> Unit,
    onConfirmOpen: () -> Unit,
) {
    val host = remember(url) { StorySecurityGuard.extractDisplayHost(url) }
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF5151722))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2481CC).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "باز کردن پیوند خارجی",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (host.isNotBlank()) {
                            Text(
                                text = host,
                                color = Color(0xFF64B5F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Text(
                    text = "آیا مایل به باز کردن این نشانی وب خارج از ویستا هستید؟",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = url,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = Color.White.copy(alpha = 0.72f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmOpen,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2481CC),
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text("باز کردن پیوند", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionAnswerDialog(
    prompt: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF5151722))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = prompt,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.take(300) },
                    placeholder = {
                        Text(
                            text = "پاسخ خود را بنویسید…",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 13.sp,
                        )
                    },
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = VistaBrandColors.Indigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = Color.White.copy(alpha = 0.72f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (answer.isNotBlank()) onSubmit(answer.trim()) },
                        enabled = answer.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VistaBrandColors.Indigo,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text("ارسال پاسخ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
