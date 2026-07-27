package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import ir.coffevista.vista_native.core.designsystem.tokens.VistaComponentSize

@Composable
fun <T> VistaNavigationBar(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    icon: @Composable (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.defaultMinSize(minHeight = VistaComponentSize.NavigationBar),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { item ->
            VistaTabItem(
                label = label(item),
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = { icon(item, item == selected) },
            )
        }
    }
}

@Composable
fun RowScope.VistaTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.semantics { this.selected = selected },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}
