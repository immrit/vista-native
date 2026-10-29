package ir.coffevista.vista_native.features.feed.ui.components

import androidx.compose.runtime.Composable
import ir.coffevista.vista_native.core.designsystem.component.VistaEditPostDialog

@Composable
fun EditPostDialog(
    initialContent: String,
    maxCharLength: Int = 1000,
    isSubmitting: Boolean = false,
    title: String = "ویرایش پست",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    VistaEditPostDialog(
        initialContent = initialContent,
        maxCharLength = maxCharLength,
        isSubmitting = isSubmitting,
        title = title,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
