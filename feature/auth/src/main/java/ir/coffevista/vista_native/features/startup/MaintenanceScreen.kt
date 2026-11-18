package ir.coffevista.vista_native.features.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import kotlinx.coroutines.delay

@Composable
fun MaintenanceScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    autoRetryIntervalMillis: Long = 10_000L,
) {
    val remoteConfig by SystemGateMonitor.remoteConfig.collectAsState()
    val copy = remoteConfig?.copyOverrides
    val title = copy?.maintenanceTitle?.takeIf { it.isNotBlank() } ?: "در حال بروزرسانی ویستا هستیم"
    val message = copy?.maintenanceMessage?.takeIf { it.isNotBlank() } ?: "چند دقیقه دیگر دوباره تلاش کنید."

    LaunchedEffect(onRetry, autoRetryIntervalMillis) {
        if (autoRetryIntervalMillis > 0L) {
            while (true) {
                delay(autoRetryIntervalMillis)
                onRetry()
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            textAlign = TextAlign.Center,
        )
        VistaButton(onClick = onRetry) {
            Text("بررسی دوباره")
        }
    }
}
