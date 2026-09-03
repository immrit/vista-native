package ir.coffevista.vista_native.features.startup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BannedScreen(
    reasonFa: String? = null,
    onContactSupport: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onExit?.invoke()
    }

    val backgroundColor = Color(0xFFB71C1C) // Colors.red.shade900
    val contentColor = Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .safeDrawingPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "دسترسی مسدود شد",
                tint = contentColor,
                modifier = Modifier.size(100.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "دسترسی مسدود شد",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = reasonFa?.takeIf(String::isNotBlank)
                    ?: "دستگاه شما به دلیل تخلف از قوانین و استفاده غیرمجاز از شبکه مسدود شده است.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onContactSupport?.invoke() ?: onExit?.invoke() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = backgroundColor,
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
            ) {
                Text(
                    text = "تماس با پشتیبانی",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = backgroundColor,
                )
            }
        }
    }
}
