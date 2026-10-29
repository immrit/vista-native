package ir.coffevista.vista_native.features.stories.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import ir.coffevista.vista_native.features.stories.ui.render.StoryElementLayers
import ir.coffevista.vista_native.features.stories.ui.render.StoryWidgetActions

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
    var answeringQuestion by remember { mutableStateOf<StoryElement?>(null) }

    val actions = remember(
        isAuthor, onPollVote, onViewQuestionAnswers, onLinkClick, onMentionClick, onHashtagClick,
    ) {
        StoryWidgetActions(
            isAuthor = isAuthor,
            onPollVote = onPollVote,
            onQuestionClick = { answeringQuestion = it },
            onViewQuestionAnswers = onViewQuestionAnswers,
            onLinkClick = onLinkClick,
            onMentionClick = onMentionClick,
            onHashtagClick = onHashtagClick,
        )
    }

    StoryStage(modifier = modifier.fillMaxSize()) { stage ->
        StoryElementLayers(elements = elements, stage = stage, actions = actions)
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
}

@Composable
private fun QuestionAnswerDialog(
    prompt: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        title = { Text(text = prompt, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.take(300) },
                placeholder = { Text("پاسخ خود را اینجا بنویسید…", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (answer.isNotBlank()) onSubmit(answer.trim()) }, enabled = answer.isNotBlank()) {
                Text("ارسال", color = VistaBrandColors.Pink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف", color = Color.White) }
        },
    )
}
