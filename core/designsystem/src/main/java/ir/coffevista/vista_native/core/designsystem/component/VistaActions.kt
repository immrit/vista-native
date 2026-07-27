package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaComponentSize

enum class VistaButtonVariant { Primary, Secondary, Outline, Text, Destructive }

@Composable
fun VistaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: VistaButtonVariant = VistaButtonVariant.Primary,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedModifier = modifier
        .defaultMinSize(
            minWidth = VistaComponentSize.TouchTarget,
            minHeight = VistaComponentSize.Button,
        )
        .semantics {
            if (loading) stateDescription = "در حال انجام"
        }
    val renderedContent: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = if (variant == VistaButtonVariant.Outline || variant == VistaButtonVariant.Text) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
            )
        } else {
            content()
        }
    }
    when (variant) {
        VistaButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = resolvedModifier,
            enabled = enabled && !loading,
            content = renderedContent,
        )
        VistaButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = resolvedModifier,
            enabled = enabled && !loading,
            content = renderedContent,
        )
        else -> {
            val defaultColor = when (variant) {
                VistaButtonVariant.Primary -> MaterialTheme.colorScheme.primary
                VistaButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary
                VistaButtonVariant.Destructive -> MaterialTheme.colorScheme.error
                else -> error("Handled above")
            }
            Button(
                onClick = onClick,
                modifier = resolvedModifier,
                enabled = enabled && !loading,
                colors = ButtonDefaults.buttonColors(containerColor = containerColor ?: defaultColor),
                content = renderedContent,
            )
        }
    }
}
