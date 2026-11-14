package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalNavigateToPremium = staticCompositionLocalOf<(() -> Unit)?> { null }

private data class ShowcaseFeatureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaPremiumBottomSheet(
    feature: String? = null,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit = {},
) {
    val navFallback = LocalNavigateToPremium.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F8DFD),
            Color(0xFF8B5CF6),
            Color(0xFFD946EF),
        ),
    )
    val ctaBlue = Color(0xFF2589F5)
    val badgeViolet = Color(0xFF9E5CF6)

    val showcaseFeatures = listOf(
        ShowcaseFeatureItem(
            icon = Icons.Default.Star,
            title = "نشان تایید طلایی اختصاصی",
            subtitle = "تیک طلایی کنار نام شما در پروفایل، پست‌ها، نظرات و چت‌ها نمایش داده می‌شود",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.CheckCircle,
            title = "بدون تبلیغات!",
            subtitle = "حذف کامل تبلیغات در تمام بخش‌های برنامه برای تجربه‌ای روان و بدون مزاحمت",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.PlayArrow,
            title = "انتشار موسیقی و ویدیوهای ۲ دقیقه‌ای",
            subtitle = "افزودن موزیک روی پست‌ها، برش حرفه‌ای صدا و انتشار ویدیو تا ۲ دقیقه",
            badge = "ویژه",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.DateRange,
            title = "استوری‌های ۴۸ ساعته",
            subtitle = "ماندگاری استوری‌های شما به جای ۲۴ ساعت تا ۴۸ ساعت کامل",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.Edit,
            title = "ارسال تا ۱۰۰۰ کاراکتر + ویرایش پست و کامنت",
            subtitle = "نوشتن پست‌های طولانی‌تر و ویرایش آزادانه متن پست‌ها و کامنت‌های خودتان",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.Lock,
            title = "کنترل نمایش لایک و کامنت + آپلود ۵۰ مگابایت",
            subtitle = "مخفی‌سازی تعداد لایک و کامنت و ارسال فایل و کلیپ تا ۵۰ مگابایت در پست و چت",
        ),
        ShowcaseFeatureItem(
            icon = Icons.Default.Search,
            title = "اولویت نمایش در نتایج جستجو",
            subtitle = "قرار گرفتن پروفایل شما در جایگاه بالاتر هنگام جستجوی کاربران",
            badge = "جدید",
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gradient Hero Banner inspired by Pro showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(heroGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val patternPoints = listOf(
                            Offset(size.width * 0.12f, size.height * 0.25f) to 16f,
                            Offset(size.width * 0.24f, size.height * 0.72f) to 12f,
                            Offset(size.width * 0.85f, size.height * 0.24f) to 18f,
                            Offset(size.width * 0.76f, size.height * 0.74f) to 14f,
                            Offset(size.width * 0.35f, size.height * 0.20f) to 10f,
                            Offset(size.width * 0.65f, size.height * 0.18f) to 10f,
                            Offset(size.width * 0.16f, size.height * 0.52f) to 8f,
                            Offset(size.width * 0.90f, size.height * 0.54f) to 9f,
                        )
                        patternPoints.forEach { (center, radius) ->
                            drawCircle(
                                color = Color.White.copy(alpha = 0.16f),
                                radius = radius,
                                center = center,
                            )
                        }
                        drawCircle(
                            color = Color.White.copy(alpha = 0.24f),
                            radius = 96f,
                            center = Offset(size.width / 2f, size.height * 0.44f),
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White.copy(alpha = 0.9f),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4F46E5).copy(alpha = 0.82f),
                        ) {
                            Text(
                                text = "ویستا پریمیوم • تیک طلایی",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!feature.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeViolet.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "برای دسترسی به «$feature» می‌توانید اشتراک ویستا پریمیوم تهیه کنید.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "حساب «ویستا پریمیوم» چه امکاناتی دارد؟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    showcaseFeatures.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(badgeViolet.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (item.badge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ) {
                                            Text(
                                                text = item.badge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (index < showcaseFeatures.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                modifier = Modifier.padding(start = 50.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onDismiss()
                        navFallback?.invoke() ?: onUpgradeClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ctaBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "خرید اشتراک پریمیوم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun VistaPremiumPromptDialog(
    feature: String = "ویرایش پست",
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    VistaPremiumBottomSheet(
        feature = feature,
        onDismiss = onDismiss,
        onUpgradeClick = onUpgradeClick,
    )
}
