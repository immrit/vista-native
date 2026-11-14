package ir.coffevista.vista_native.features.profile.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge

private data class TickTierSpec(
    val title: String,
    val roleLabel: String,
    val description: String,
    val verificationType: String?,
    val isVerified: Boolean,
    val accentColor: Color,
    val searchRankBadge: String,
    val perks: List<Pair<String, Boolean>>,
    val showUpgradeAction: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickGuideScreen(
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tiers = listOf(
        TickTierSpec(
            title = "تیک آبی",
            roleLabel = "مدیران و ناظران رسمی ویستا",
            description = "این نشان مختص ناظران، تیم امنیت و مدیران رسمی پلتفرم ویستا است و بالاترین سطح دسترسی و اولویت را در برنامه دارد.",
            verificationType = "blueTick",
            isVerified = true,
            accentColor = Color(0xFF0095F6),
            searchRankBadge = "رتبه اول جستجو",
            perks = listOf(
                "حداکثر طول پست: ۱۵۰۰ کاراکتر" to true,
                "ویرایش پست و کامنت" to true,
                "استوری بیش از ۲۴ ساعت (۴۸ ساعته)" to true,
                "ارسال فایل و کلیپ تا ۱۰۰ مگابایت" to true,
                "انتشار موزیک در پست‌ها" to true,
                "ویدیو در پست تا ۵ دقیقه" to true,
                "کنترل باز و بسته بودن تعداد لایک و کامنت" to true,
                "بدون نمایش تبلیغات" to true,
            ),
        ),
        TickTierSpec(
            title = "تیک مشکی",
            roleLabel = "افراد معروف، هنرمندان و برندهای معتبر",
            description = "جایگاه این تیک مشابه تیک آبی چهره‌های سرشناس در اینستاگرام است و به حساب‌های شناخته‌شده و رسمی اختصاص داده می‌شود.",
            verificationType = "blackTick",
            isVerified = true,
            accentColor = Color(0xFF334155),
            searchRankBadge = "رتبه دوم جستجو",
            perks = listOf(
                "حداکثر طول پست: ۵۰۰ کاراکتر" to true,
                "ویرایش پست و کامنت" to true,
                "استوری بیش از ۲۴ ساعت (۴۸ ساعته)" to true,
                "ارسال فایل و کلیپ تا ۵۰ مگابایت" to true,
                "انتشار موزیک در پست‌ها" to true,
                "ویدیو در پست تا ۲ دقیقه" to true,
                "کنترل باز و بسته بودن تعداد لایک و کامنت" to true,
                "بدون نمایش تبلیغات" to true,
            ),
        ),
        TickTierSpec(
            title = "تیک طلایی (ویستا پریمیوم)",
            roleLabel = "کاربران اشتراکی پریمیوم",
            description = "با خرید اشتراک ویستا پریمیوم فعال می‌شود و امکانات ویژه تولید محتوا، اولویت جستجو و تجربه بدون تبلیغ را در اختیار شما می‌گذارد.",
            verificationType = "goldTick",
            isVerified = true,
            accentColor = Color(0xFFF59E0B),
            searchRankBadge = "رتبه سوم جستجو",
            perks = listOf(
                "حداکثر طول پست: ۱۰۰۰ کاراکتر" to true,
                "ویرایش پست و کامنت" to true,
                "استوری بیش از ۲۴ ساعت (۴۸ ساعته)" to true,
                "ارسال فایل و کلیپ تا ۵۰ مگابایت" to true,
                "انتشار موزیک در پست‌ها" to true,
                "ویدیو در پست تا ۲ دقیقه" to true,
                "کنترل باز و بسته بودن تعداد لایک و کامنت" to true,
                "بدون نمایش تبلیغات" to true,
            ),
            showUpgradeAction = true,
        ),
        TickTierSpec(
            title = "کاربر عادی (بدون تیک)",
            roleLabel = "دسترسی استاندارد ویستا",
            description = "حساب استاندارد برای استفاده روزمره از امکانات اصلی ویستا با محدودیت‌های پایه.",
            verificationType = null,
            isVerified = false,
            accentColor = Color(0xFF64748B),
            searchRankBadge = "بدون اولویت جستجو",
            perks = listOf(
                "حداکثر طول پست: ۳۰۰ کاراکتر" to true,
                "ارسال فایل و کلیپ تا ۱۰ مگابایت" to true,
                "ویدیو در پست تا ۱ دقیقه" to true,
                "استوری حداکثر ۲۴ ساعته" to true,
                "ویرایش پست و کامنت" to false,
                "انتشار موزیک در پست‌ها" to false,
                "کنترل نمایش تعداد لایک و کامنت" to false,
                "نمایش تبلیغات در صورت فعال بودن" to false,
            ),
        ),
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "قوانین تیک‌ها و جایگاه کاربران",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "در ویستا ۴ سطح کاربری وجود دارد. هر رنگ تیک نشان‌دهنده جایگاه، میزان دسترسی و اولویت نمایش کاربر در پلتفرم است:",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp),
                    )
                }

                tiers.forEach { spec ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = spec.accentColor.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(18.dp),
                            ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(spec.accentColor.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (spec.isVerified) {
                                            VistaVerificationBadge(
                                                isVerified = true,
                                                verificationType = spec.verificationType,
                                                size = 22.dp,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = spec.accentColor,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = spec.title,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = spec.roleLabel,
                                            fontSize = 12.sp,
                                            color = spec.accentColor,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = spec.accentColor.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = spec.searchRankBadge,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = spec.accentColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = spec.description,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                spec.perks.forEach { (label, allowed) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (allowed) Icons.Outlined.CheckCircle else Icons.Outlined.Close,
                                            contentDescription = null,
                                            tint = if (allowed) Color(0xFF16A34A) else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }

                            if (spec.showUpgradeAction) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onUpgradeClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "خرید اشتراک ویستا پریمیوم (تیک طلایی)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
