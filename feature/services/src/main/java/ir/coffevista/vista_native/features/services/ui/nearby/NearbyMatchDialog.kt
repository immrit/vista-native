package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult

@Composable
fun NearbyMatchDialog(
    matchResult: NearbyLikeResult,
    candidate: NearbyCandidate?,
    onDismiss: () -> Unit,
    onStartChat: (matchId: String, otherUserId: String, username: String, avatarUrl: String) -> Unit,
) {
    val name = matchResult.match?.fullName ?: candidate?.fullName ?: "کاربر"
    val avatar = matchResult.match?.avatarUrl ?: candidate?.avatarUrl ?: ""
    val userId = matchResult.match?.userId ?: candidate?.userId ?: ""
    val matchId = matchResult.matchId

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            color = Color(0xFF1E1E2E).copy(alpha = 0.95f),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Trophy / Heart Header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(VistaBrandColors.Indigo, Color(0xFFEC4899))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "مَچ شدید! 🎉",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 26.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "تو و $name همدیگه رو پسندیدید",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar
                if (avatar.isNotBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = name,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(VistaBrandColors.Indigo, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 40.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Start Chat Button
                Button(
                    onClick = {
                        onDismiss()
                        onStartChat(matchId, userId, name, avatar)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VistaBrandColors.Indigo,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شروع گفتگو",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Continue Exploring
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "ادامه کاوش",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = VistaFontFamily,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}
