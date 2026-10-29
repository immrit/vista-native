package ir.coffevista.vista_native.core.designsystem.component

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
fun VistaReportReasonDialog(
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
                    imageVector = Icons.Default.Warning,
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
                    text = "لطفاً دلیل گزارش خود را انتخاب کنید:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                DEFAULT_REPORT_REASONS.forEach { (label, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) {
                                selectedReason = label
                                errorMessage = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = {
                        if (it.length <= 500) additionalDetails = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("توضیحات تکمیلی (اختیاری)...") },
                    maxLines = 3,
                    enabled = !isSubmitting,
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                    if (selectedReason.isBlank()) {
                        errorMessage = "لطفاً یک دلیل انتخاب کنید"
                        return@Button
                    }
                    val code = DEFAULT_REPORT_REASONS.firstOrNull { it.first == selectedReason }?.second ?: "other"
                    onSubmit(code, additionalDetails.trim().takeIf { it.isNotEmpty() })
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
                    Text("ارسال گزارش", fontWeight = FontWeight.Bold)
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
