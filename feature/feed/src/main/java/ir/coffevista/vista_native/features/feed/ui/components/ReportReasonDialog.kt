package ir.coffevista.vista_native.features.feed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DEFAULT_REPORT_REASONS = listOf(
    "محتوای نامناسب" to "inappropriate",
    "هرزنگاری" to "spam",
    "توهین آمیز" to "harassment",
    "اسپم" to "spam",
    "محتوای تبلیغاتی" to "promotional",
    "سایر موارد" to "other",
)

@Composable
fun ReportReasonDialog(
    title: String = "گزارش محتوا",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, additionalDetails: String?) -> Unit,
) {
    var selectedReason by remember { mutableStateOf(DEFAULT_REPORT_REASONS.first().first) }
    var additionalDetails by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "لطفاً دلیل گزارش را انتخاب کنید:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                DEFAULT_REPORT_REASONS.forEach { (label, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) {
                                selectedReason = label
                                errorMessage = null
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedReason == label,
                            onClick = {
                                selectedReason = label
                                errorMessage = null
                            },
                            enabled = !isSubmitting,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (selectedReason == "سایر موارد") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = additionalDetails,
                        onValueChange = { additionalDetails = it },
                        placeholder = { Text("توضیحات بیشتر (اختیاری)...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        enabled = !isSubmitting,
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason.isEmpty()) {
                        errorMessage = "لطفاً دلیل گزارش را انتخاب کنید"
                        return@Button
                    }
                    val details = if (selectedReason == "سایر موارد") additionalDetails.trim().ifEmpty { null } else null
                    onSubmit(selectedReason, details)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("ارسال گزارش", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("انصراف")
            }
        },
    )
}
