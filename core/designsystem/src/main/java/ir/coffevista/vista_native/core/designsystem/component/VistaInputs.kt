package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors

@Composable
fun VistaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val (bufferedValue, onBufferedChange) = rememberBufferedTextState(
        externalValue = value,
        onValueChange = onValueChange,
    )
    val accentBlue = MaterialTheme.vistaColors.viuBlue
    val resolvedStyle = if (textStyle == TextStyle.Default) {
        MaterialTheme.typography.bodyLarge
    } else {
        textStyle
    }
    OutlinedTextField(
        value = bufferedValue,
        onValueChange = onBufferedChange,
        modifier = modifier.semantics {
            errorMessage?.let { error(it) }
        },
        shape = RoundedCornerShape(14.dp),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (errorMessage ?: supportingText)?.let { message ->
            { Text(message) }
        },
        isError = errorMessage != null,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = resolvedStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            focusedBorderColor = accentBlue,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = accentBlue,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = accentBlue,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
