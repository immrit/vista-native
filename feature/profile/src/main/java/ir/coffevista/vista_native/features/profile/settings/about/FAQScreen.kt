package ir.coffevista.vista_native.features.profile.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection

data class FaqItem(
    val id: String,
    val question: String,
    val answer: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyFAQScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val accountFaqs = listOf(
        FaqItem(
            "1",
            "چگونه اطلاعات پروفایل خود را ویرایش کنم؟",
            "از صفحه تنظیمات روی کارت پروفایل خود در بالای صفحه یا دکمه «ویرایش پروفایل» در برگه پروفایل ضربه بزنید. در آنجا می‌توانید نام، نام کاربری، بیوگرافی و تصویر حساب خود را تغییر دهید.",
        ),
        FaqItem(
            "2",
            "چگونه تیک آبی یا نشان تأیید هویت دریافت کنم؟",
            "از بخش تنظیمات وارد گزینه «درخواست تیک آبی» شوید، دسته‌بندی فعالیت خود را انتخاب نموده و مدرک شناسایی معتبر خود را بارگذاری کنید. تیم پشتیبانی ظرف ۴۸ ساعت کاری درخواست شما را بررسی خواهد کرد.",
        ),
    )

    val privacyFaqs = listOf(
        FaqItem(
            "3",
            "آیا پیام‌ها و گفت‌وگوهای ویستا امن هستند؟",
            "بله، تمامی پیام‌ها و گفت‌وگوها با پروتکل‌های پیشرفته رمزگذاری End-to-End محافظت می‌شوند و هیچ شخص ثالثی دسترسی به پیام‌های شما نخواهد داشت.",
        ),
        FaqItem(
            "4",
            "چگونه نشست‌های فعال دیگر را ببندم؟",
            "وارد بخش «حریم خصوصی و امنیت» شوید و روی «نشست‌های فعال» ضربه بزنید. در آنجا لیست تمامی دستگاه‌های متصل به حساب را مشاهده کرده و می‌توانید با یک کلیک از سایر دستگاه‌ها خارج شوید.",
        ),
    )

    val premiumFaqs = listOf(
        FaqItem(
            "5",
            "مزایای اشتراک ویستا پریمیوم چیست؟",
            "کاربران پریمیوم از نشان طلایی تأیید شده، استوری‌های ۴۸ ساعته، امکان ویرایش پست‌ها پس از انتشار، کپشن‌های طولانی‌تر تا ۱۰۰۰ کاراکتر و آپلود ویدیو با بالاترین کیفیت بهره‌مند می‌شوند.",
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
                            text = "سوالات متداول",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
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
                                tint = MaterialTheme.colorScheme.onSurface,
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
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                VistaSettingsSection(title = "حساب کاربری و هویت")
                VistaSettingsGroup {
                    accountFaqs.forEachIndexed { index, faq ->
                        val isExpanded = expandedStates[faq.id] == true
                        FaqRow(
                            faq = faq,
                            isExpanded = isExpanded,
                            onToggle = { expandedStates[faq.id] = !isExpanded },
                        )
                        if (index < accountFaqs.size - 1) VistaSettingsDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                VistaSettingsSection(title = "امنیت و حریم خصوصی")
                VistaSettingsGroup {
                    privacyFaqs.forEachIndexed { index, faq ->
                        val isExpanded = expandedStates[faq.id] == true
                        FaqRow(
                            faq = faq,
                            isExpanded = isExpanded,
                            onToggle = { expandedStates[faq.id] = !isExpanded },
                        )
                        if (index < privacyFaqs.size - 1) VistaSettingsDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                VistaSettingsSection(title = "ویستا پریمیوم")
                VistaSettingsGroup {
                    premiumFaqs.forEachIndexed { index, faq ->
                        val isExpanded = expandedStates[faq.id] == true
                        FaqRow(
                            faq = faq,
                            isExpanded = isExpanded,
                            onToggle = { expandedStates[faq.id] = !isExpanded },
                        )
                        if (index < premiumFaqs.size - 1) VistaSettingsDivider()
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FaqRow(
    faq: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = faq.question,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                ),
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = faq.answer,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                ),
                modifier = Modifier.padding(top = 10.dp, end = 4.dp),
            )
        }
    }
}
