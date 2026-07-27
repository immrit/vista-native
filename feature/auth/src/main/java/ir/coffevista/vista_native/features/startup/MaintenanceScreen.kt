package ir.coffevista.vista_native.features.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.component.VistaButton

@Composable
fun MaintenanceScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "در حال بروزرسانی ویستا هستیم",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "چند دقیقه دیگر دوباره تلاش کنید.",
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            textAlign = TextAlign.Center,
        )
        VistaButton(onClick = onRetry) {
            Text("بررسی دوباره")
        }
    }
}
