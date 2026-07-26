package ir.coffevista.vista_native.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DeferredDestinationScreen(
    kind: DeferredFeatureKind,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "این مقصد هنوز در نسخه Native آماده نشده است",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "لینک ${kind.persianName()} معتبر است و برای فاز Feature مربوط نگه‌داری شد.",
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack) {
            Text("بازگشت")
        }
    }
}

@Composable
fun DeepLinkFailureScreen(
    reason: DeepLinkFailureReason,
    onBack: () -> Unit,
) {
    val message = when (reason) {
        DeepLinkFailureReason.UNSUPPORTED -> "این لینک توسط ویستا پشتیبانی نمی‌شود."
        DeepLinkFailureReason.INVALID_ARGUMENT -> "اطلاعات این لینک معتبر نیست."
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
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

private fun DeferredFeatureKind.persianName(): String = when (this) {
    DeferredFeatureKind.POST -> "پست"
    DeferredFeatureKind.PROFILE -> "پروفایل"
    DeferredFeatureKind.GROUP -> "گروه"
    DeferredFeatureKind.CHAT -> "گفت‌وگو"
}
