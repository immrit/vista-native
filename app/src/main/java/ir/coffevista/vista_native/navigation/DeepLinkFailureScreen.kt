package ir.coffevista.vista_native.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DeepLinkFailureScreen(
    reason: DeepLinkFailureReason,
    onBack: () -> Unit,
) {
    val message = when (reason) {
        DeepLinkFailureReason.UNSUPPORTED ->
            "این لینک توسط ویستا پشتیبانی نمی‌شود."
        DeepLinkFailureReason.INVALID_ARGUMENT ->
            "اطلاعات این لینک معتبر نیست."
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("بازگشت")
        }
    }
}
