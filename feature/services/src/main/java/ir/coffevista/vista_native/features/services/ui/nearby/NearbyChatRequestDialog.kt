package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyWallet

private val QuickIntroSuggestions = listOf(
    "سلام! هم‌شهری هستیم، خوشحال می‌شم آشنا بشیم 👋",
    "پروفایلت برام جالب بود، پایه‌ی گپ هستی؟ ✨",
    "سلام، وقتت بخیر! موضوع مشترکی پیدا می‌کنیم؟ ☕",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NearbyChatRequestDialog(
    target: NearbyCandidate,
    wallet: NearbyWallet,
    isSending: Boolean,
    onDismiss: () -> Unit,
    onSend: (introMessage: String) -> Unit,
    onOpenShop: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var introMessage by remember(target.userId) { mutableStateOf("") }

    val canSend = wallet.freeDailyRequestsRemaining > 0 ||
        wallet.chatRequestCredits > 0 ||
        wallet.coins >= wallet.chatRequestCoinCost

    val costLabel = when {
        wallet.freeDailyRequestsRemaining > 0 ->
            "رایگان از سهمیه امروز (${wallet.freeDailyRequestsRemaining} باقیمانده)"
        wallet.chatRequestCredits > 0 ->
            "کسر ۱ اعتبار درخواست چت (موجودی: ${wallet.chatRequestCredits})"
        wallet.coins >= wallet.chatRequestCoinCost ->
            "کسر ${wallet.chatRequestCoinCost} سکه (موجودی: ${formatFarsiNumber(wallet.coins)})"
        else ->
            "موجودی کافی نیست (نیاز به ${wallet.chatRequestCoinCost} سکه یا خرید بسته)"
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF141522),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 26.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (target.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = target.avatarUrl,
                                contentDescription = target.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VistaBrandColors.Indigo),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = target.fullName.take(1).ifBlank { "؟" },
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ارسال درخواست چت به ${target.fullName}",
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                )
                                if (target.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            if (target.locationLine.isNotBlank()) {
                                Text(
                                    text = target.locationLine,
                                    fontFamily = VistaFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "پیشنهاد شروع گفتگو (اختیاری):",
                    fontFamily = VistaFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickIntroSuggestions.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E2032),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { introMessage = sample },
                        ) {
                            Text(
                                text = sample,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = introMessage,
                    onValueChange = { if (it.length <= 240) introMessage = it },
                    placeholder = {
                        Text(
                            "یک پیام کوتاه و محترمانه بنویس...",
                            fontFamily = VistaFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VistaBrandColors.Indigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${introMessage.length}/۲۴۰",
                        fontFamily = VistaFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (canSend) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (canSend) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (canSend) Icons.Rounded.Forum else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (canSend) Color(0xFF34D399) else Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = costLabel,
                            fontFamily = VistaFontFamily,
                            fontSize = 12.sp,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (canSend) {
                    Button(
                        onClick = { onSend(introMessage.trim()) },
                        enabled = !isSending,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ارسال درخواست چت",
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenShop()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(
                            text = "ورود به فروشگاه سکه و درخواست چت",
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFBBF24),
                        )
                    }
                }
            }
        }
    }
}
