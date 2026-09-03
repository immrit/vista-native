package ir.coffevista.vista_native.features.feed.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun EditPostDialog(
    initialContent: String,
    maxCharLength: Int = 1000,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var content by remember { mutableStateOf(initialContent) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun resolveDirection(text: String): LayoutDirection {
        val persianRegex = Regex("[\u0600-\u06FF]")
        val englishRegex = Regex("[a-zA-Z]")
        val persianCount = persianRegex.findAll(text).count()
        val englishCount = englishRegex.findAll(text).count()
        return if (persianCount >= englishCount) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ویرایش پست",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalLayoutDirection provides resolveDirection(content)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            if (it.length <= maxCharLength) {
                                content = it
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("متن پست را ویرایش کنید...") },
                        supportingText = {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${content.length}/$maxCharLength",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        isError = errorMessage != null,
                        enabled = !isSubmitting,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = content.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "متن پست نمی‌تواند خالی باشد"
                        return@Button
                    }
                    onConfirm(trimmed)
                },
                enabled = !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("لغو")
            }
        },
    )
}
