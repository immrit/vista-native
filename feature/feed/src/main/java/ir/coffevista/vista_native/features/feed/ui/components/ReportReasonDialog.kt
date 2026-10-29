package ir.coffevista.vista_native.features.feed.ui.components

import androidx.compose.runtime.Composable
import ir.coffevista.vista_native.core.designsystem.component.DEFAULT_REPORT_REASONS
import ir.coffevista.vista_native.core.designsystem.component.VistaReportReasonDialog

@Composable
fun ReportReasonDialog(
    title: String = "گزارش محتوا",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, additionalDetails: String?) -> Unit,
) {
    VistaReportReasonDialog(
        title = title,
        isSubmitting = isSubmitting,
        onDismiss = onDismiss,
        onSubmit = onSubmit,
    )
}
