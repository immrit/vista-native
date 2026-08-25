package ir.coffevista.vista_native.features.profile.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyPrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "سیاست حریم خصوصی",
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Text(
                            text = "سیاست حفظ حریم خصوصی کاربران ویستا",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ویستا متعهد به حفظ بالاترین استانداردهای امنیتی و حریم خصوصی برای کاربران خود است. این سند نحوه جمع‌آوری، استفاده و حفاظت از اطلاعات شما را تشریح می‌کند.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۱. اطلاعات جمع‌آوری شده",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اطلاعات ثبت‌نامی شامل نام کاربری، ایمیل، شماره همراه (در صورت ثبت) و اطلاعات دستگاه برای بهبود کیفیت سرویس‌دهی ذخیره می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۲. امنیت و رمزنگاری",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تمامی رمزهای عبور به صورت هش‌شده (Bcrypt/Argon2) ذخیره می‌شوند و داده‌های حساس درون سرورهای اختصاصی با بالاترین سطوح امنیتی محافظت می‌گردند.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۳. عدم اشتراک با شخص ثالث",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ویستا هیچ‌گونه اطلاعات شخصی شما را با نهادها یا شرکت‌های تبلیغاتی ثالث به اشتراک نخواهد گذاشت.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
