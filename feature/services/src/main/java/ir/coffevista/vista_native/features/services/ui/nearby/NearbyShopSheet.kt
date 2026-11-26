package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaCoinIcon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequestPack
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCoinPackage
import ir.coffevista.vista_native.features.services.data.nearby.NearbyWallet
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyShopSheet(
    wallet: NearbyWallet,
    packs: List<NearbyChatRequestPack>,
    coinPackages: List<NearbyCoinPackage>,
    sampleRateLabel: String,
    isLoading: Boolean,
    purchasingId: String?,
    hasPendingVerification: Boolean,
    shopMessage: String?,
    onDismiss: () -> Unit,
    onBuyRequestPack: (packId: String) -> Unit,
    onBuyCoinPackage: (packageId: String) -> Unit,
    onVerifyPendingPayment: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF131420),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFF0543B)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingBag,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "فروشگاه سکه و درخواست چت",
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                            Text(
                                text = sampleRateLabel,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFFBBF24),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wallet summary strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B1E23),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WalletStatChip(
                            label = "موجودی سکه",
                            value = formatFarsiNumber(wallet.coins),
                            leadingIcon = { VistaCoinIcon(size = 22.dp) },
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color.White.copy(alpha = 0.1f)),
                        )
                        WalletStatChip(
                            label = "اعتبار درخواست چت",
                            value = "${formatFarsiNumber(wallet.totalAvailableRequests.toLong())} عدد",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                        )
                    }
                }

                if (hasPendingVerification) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, VistaBrandColors.ViuBlueDeep.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "خرید کافه‌بازار در انتظار تایید",
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                )
                                Text(
                                    text = "در صورت کسر وجه و ثبت نشدن سکه، دکمه بررسی را بزنید",
                                    fontFamily = VistaFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                )
                            }
                            Button(
                                onClick = onVerifyPendingPayment,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.ViuBlueDeep),
                            ) {
                                Text("بررسی خرید", fontFamily = VistaFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (!shopMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = shopMessage,
                                    fontFamily = VistaFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                )
                            }
                            IconButton(onClick = onClearMessage, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1B1E23),
                    contentColor = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "تبدیل سکه به درخواست چت",
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "خرید سکه (کافه‌بازار)",
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading && packs.isEmpty() && coinPackages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = VistaBrandColors.ViuBlueDeep)
                    }
                } else if (selectedTab == 0) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(packs, key = { it.id }) { pack ->
                            val enoughCoins = wallet.coins >= pack.coinCost
                            ChatRequestPackRow(
                                pack = pack,
                                enoughCoins = enoughCoins,
                                isBusy = purchasingId == pack.id,
                                onBuy = {
                                    if (enoughCoins) {
                                        onBuyRequestPack(pack.id)
                                    } else {
                                        selectedTab = 1
                                    }
                                },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(coinPackages, key = { it.id }) { pkg ->
                            val approxRequests = (pkg.totalCoins / wallet.chatRequestCoinCost.coerceAtLeast(1)).toInt()
                            CoinPackageRow(
                                pkg = pkg,
                                approxRequests = approxRequests,
                                isBusy = purchasingId == pkg.id,
                                onBuy = { onBuyCoinPackage(pkg.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletStatChip(
    label: String,
    value: String,
    leadingIcon: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        leadingIcon()
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontFamily = VistaFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text(
                text = value,
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ChatRequestPackRow(
    pack: NearbyChatRequestPack,
    enoughCoins: Boolean,
    isBusy: Boolean,
    onBuy: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (pack.isPopular) VistaBrandColors.ViuBlueDeep.copy(alpha = 0.14f) else Color(0xFF1B1E23),
        border = BorderStroke(
            width = if (pack.isPopular) 1.5.dp else 1.dp,
            color = if (pack.isPopular) VistaBrandColors.ViuBlueDeep else Color.White.copy(alpha = 0.08f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = pack.title,
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                    if (pack.badge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pack.isPopular) Color(0xFFF0543B) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = pack.badge,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pack.isPopular) Color.White else Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("${formatFarsiNumber(pack.requestsCount.toLong())} درخواست مستقیم")
                        if (pack.bonusLabel.isNotBlank()) append(" • ${pack.bonusLabel}")
                    },
                    fontFamily = VistaFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (enoughCoins) {
                Button(
                    onClick = onBuy,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.ViuBlueDeep),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(
                            text = "${formatFarsiNumber(pack.coinCost.toLong())} سکه",
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onBuy,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.7f)),
                ) {
                    Text(
                        text = "نیاز به ${formatFarsiNumber(pack.coinCost.toLong())} سکه",
                        fontFamily = VistaFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFFBBF24),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinPackageRow(
    pkg: NearbyCoinPackage,
    approxRequests: Int,
    isBusy: Boolean,
    onBuy: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1E23),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VistaCoinIcon(size = 20.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${formatFarsiNumber(pkg.totalCoins)} سکه",
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                    )
                    if (pkg.bonus > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "+${formatFarsiNumber(pkg.bonus)} هدیه",
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "کافی برای ارسال حدود $approxRequests درخواست چت",
                    fontFamily = VistaFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onBuy,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        text = "${formatFarsiNumber(pkg.priceToman.toLong())} تومان",
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

fun formatFarsiNumber(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(value)
}
