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
private fun LegacyTermsAndConditionsScreen(
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
                            text = "قوانین و مقررات",
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
                            text = "شرایط استفاده از خدمات ویستا",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "به پلتفرم اجتماعی ویستا خوش آمدید. با ثبت‌نام یا استفاده از هر یک از بخش‌های ویستا، شما موافقت خود را با تمامی قوانین و مقررات این توافق‌نامه اعلام می‌دارید.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۱. حساب کاربری و امنیت",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "کاربران موظف به حفظ اطلاعات ورود و رمز عبور خود می‌باشند. هرگونه فعالیت با حساب کاربری متوجه صاحب اصلی حساب خواهد بود. ایجاد حساب‌های جعلی یا سوءاستفاده از هویت دیگران خلاف قوانین بوده و منجر به مسدودسازی حساب می‌شود.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۲. قوانین انتشار محتوا",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "انتشار هرگونه محتوای توهین‌آمیز، خلاف اخلاق، نقض‌کننده حقوق کپی‌رایت، خشونت‌آمیز یا ناقض قوانین جاری کشور ممنوع است. ویستا این حق را برای خود محفوظ می‌دارد که بدون اطلاع قبلی محتوای متخلف را حذف نماید.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "۳. خدمات اشتراک و پرداخت",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اشتراک‌های ویژه (ویستا پریمیوم) از درگاه‌های معتبر خریداری می‌شوند. هرگونه تراکنش قطعی بوده و قابلیت بازپرداخت برای دوره‌های مصرف‌شده وجود ندارد.",
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
