package ir.coffevista.vista_native.features.profile.settings.about

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class SlideItemData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val features: List<String>,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun VistaAboutSlideshowScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slides = listOf(
        SlideItemData(
            title = "به ویستا خوش آمدید",
            subtitle = "پلتفرم اجتماعی پیشرفته و امن",
            description = "چت، اشتراک‌گذاری، استوری، موزیک",
            icon = Icons.Outlined.RocketLaunch,
            gradientColors = listOf(Color(0xFF2196F3), Color(0xFF21CBF3)),
            features = listOf("چت و پیام‌رسانی پیشرفته", "اشتراک‌گذاری محتوا", "سیستم استوری", "پخش‌کننده موزیک"),
        ),
        SlideItemData(
            title = "امنیت در اولویت",
            subtitle = "رمزگذاری End-to-End",
            description = "حفاظت کامل از اطلاعات شما",
            icon = Icons.Outlined.Security,
            gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
            features = listOf("رمزگذاری End-to-End", "تایید دو مرحله‌ای", "قفل اپلیکیشن", "مسدودسازی کاربران"),
        ),
        SlideItemData(
            title = "تحت ابر قدرتمند",
            subtitle = "زیرساخت ابری پیشرفته",
            description = "۹۹.۹٪ زمان کارکرد تضمین شده",
            icon = Icons.Outlined.Cloud,
            gradientColors = listOf(Color(0xFF7E57C2), Color(0xFFE91E63)),
            features = listOf("زیرساخت ابری پایدار", "پشتیبان‌گیری خودکار", "مقیاس‌پذیری بالا", "CDN سریع جهانی"),
        ),
        SlideItemData(
            title = "عملکرد فوق‌العاده",
            subtitle = "بهینه‌سازی شده برای سرعت",
            description = "کاهش ۸۵٪ مصرف حافظه",
            icon = Icons.Outlined.Speed,
            gradientColors = listOf(Color(0xFFFF9800), Color(0xFFFFC107)),
            features = listOf("کاهش ۸۵٪ مصرف حافظه", "کاهش ۶۰٪ مصرف باتری", "سیستم کش هوشمند", "مدیریت حافظه پیشرفته"),
        ),
        SlideItemData(
            title = "سرعت فوق‌العاده",
            subtitle = "بهینه‌سازی شده برای عملکرد",
            description = "تجربه‌ای روان و سریع",
            icon = Icons.Outlined.Bolt,
            gradientColors = listOf(Color(0xFF607D8B), Color(0xFF90A4AE)),
            features = listOf("کش آفلاین کامل", "همگام‌سازی هوشمند", "دسترسی به پیام‌ها", "مشاهده پست‌ها"),
        ),
        SlideItemData(
            title = "آمار و دستاوردها",
            subtitle = "تایید شده توسط کاربران",
            description = "۱۰۰,۰۰۰+ کاربر فعال",
            icon = Icons.Outlined.EmojiEvents,
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFFF06292)),
            features = listOf("۱۰۰,۰۰۰+ کاربر فعال", "امتیاز ۴.۸ از ۵", "پشتیبانی ۲۴/۷", "جامعه صمیمی و پویا"),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Big Animated Icon Container
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(slide.gradientColors),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(68.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = slide.subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Feature Pill Tags
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        slide.features.forEach { feature ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Close button top corner
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Bottom Navigation & Dots
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width = if (isSelected) 24.dp else 8.dp
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action button
                Button(
                    onClick = {
                        if (pagerState.currentPage < slides.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (pagerState.currentPage < slides.size - 1) "بعدی" else "متوجه شدم",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                    )
                }
            }
        }
    }
}
