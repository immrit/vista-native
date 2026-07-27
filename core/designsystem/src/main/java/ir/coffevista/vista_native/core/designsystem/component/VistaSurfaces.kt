package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing

@Composable
fun VistaSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        shadowElevation = shadowElevation,
        content = content,
    )
}

@Composable
fun VistaScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            snackbarHostState?.let { VistaSnackbarHost(it) }
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaTopAppBar(title: String, modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier,
    )
}

@Composable
fun VistaDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun VistaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier)
}

@Composable
fun VistaDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                VistaButton(onClick = onConfirm, variant = VistaButtonVariant.Destructive) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                VistaButton(onClick = onDismiss, variant = VistaButtonVariant.Text) {
                    Text("انصراف")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaBottomSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (visible) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = VistaLayout.ScreenHorizontal,
                        vertical = VistaSpacing.Large,
                    ),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Column(
                    modifier = Modifier.padding(top = VistaSpacing.Large),
                    content = content,
                )
            }
        }
    }
}
