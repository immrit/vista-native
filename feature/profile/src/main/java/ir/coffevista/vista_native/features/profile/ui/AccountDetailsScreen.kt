package ir.coffevista.vista_native.features.profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.profile.R
import ir.coffevista.vista_native.features.profile.ui.components.gregorianIsoToJalaliDisplay

/**
 * Account Details Screen - Complete Flutter parity implementation
 * Shows user profile header, account type, membership details, stats, bio, and user ID footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountDetailsScreen(
    profile: ProfileHeaderModel,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "جزییات اکانت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_back),
                                contentDescription = "بازگشت",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Profile Header Card
                ProfileHeaderCard(profile)

                // Account Type Card
                AccountTypeCard(profile)

                // Membership Card
                MembershipCard(profile)

                // Additional Account Details Card
                AdditionalDetailsCard(profile)

                // Stats Card
                StatsCard(profile)

                // Bio Card
                if (!profile.bio.isNullOrBlank()) {
                    BioCard(profile.bio)
                }

                Spacer(Modifier.height(8.dp))

                // Account ID Footer
                AccountIdFooter(userId = userId)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: ProfileHeaderModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 72dp Avatar
            if (profile.avatarUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName.ifBlank { profile.username ?: "" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u200E@${profile.username ?: ""}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (profile.isVerified) {
                        VerificationBadgeIcon(
                            isVerified = true,
                            verificationType = profile.verificationType,
                            role = profile.role,
                            size = 16.dp,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    if (profile.isPrivate) {
                        Text(
                            text = " 🔒",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTypeCard(profile: ProfileHeaderModel) {
    val resolvedType = resolveVerificationBadgeType(
        isVerified = profile.isVerified,
        verificationType = profile.verificationType,
        role = profile.role,
    )

    val (title, description, iconColor, iconBgColor) = when (resolvedType) {
        ResolvedVerificationBadgeType.BLACK_TICK -> Tuple4(
            "اکانت مؤسس و بنیان‌گذار",
            "این اکانت متعلق به تیم مؤسس و بنیان‌گذاران ویستا می‌باشد و دارای نشان رسمی تأیید هویت است.",
            Color(0xFF1E1E2E),
            Color(0xFFE0E7FF),
        )
        ResolvedVerificationBadgeType.GOLD_TICK -> Tuple4(
            "اکانت تأیید شده طلایی",
            "این اکانت دارای عضویت ویژه و نشان رسمی طلایی ویستا می‌باشد.",
            Color(0xFFD97706),
            Color(0xFFFEF3C7),
        )
        ResolvedVerificationBadgeType.BLUE_TICK -> Tuple4(
            "اکانت تأیید شده رسمی",
            "هویت این کاربر به صورت رسمی توسط تیم ویستا تأیید شده است.",
            Color(0xFF2563EB),
            Color(0xFFDBEAFE),
        )
        ResolvedVerificationBadgeType.NONE -> Tuple4(
            "اکانت عمومی",
            "اکانت استاندارد کاربری در پلتفرم ویستا.",
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column {
            Text(
                text = "نوع اکانت",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (resolvedType != ResolvedVerificationBadgeType.NONE) {
                        VerificationBadgeIcon(
                            isVerified = true,
                            verificationType = profile.verificationType,
                            role = profile.role,
                            size = 28.dp,
                        )
                    } else {
                        Text("👤", fontSize = 24.sp)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (resolvedType != ResolvedVerificationBadgeType.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    iconColor.copy(alpha = 0.6f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun MembershipCard(profile: ProfileHeaderModel) {
    val joinOrder = profile.joinOrder.takeIf { it > 0 }
    val joinedAt = profile.createdAt
        ?.take(10)
        ?.let(::gregorianIsoToJalaliDisplay)
    if (joinOrder == null && joinedAt == null) return

    val tier = joinOrder?.let { order ->
        when {
            order <= 100 -> Tuple4("عضو بنیان‌گذار  #$order", Color(0xFFB45309), Color(0xFFFEF3C7), R.drawable.ic_badge_founder)
            order <= 1000 -> Tuple4("از اولین هزار نفر  #$order", Color(0xFF1D4ED8), Color(0xFFDBEAFE), R.drawable.ic_badge_rocket)
            order <= 10000 -> Tuple4("عضو پیشگام  #$order", Color(0xFF6D28D9), Color(0xFFF3E8FF), R.drawable.ic_badge_bolt)
            else -> Tuple4("عضو شماره  #$order", Color(0xFF475569), Color(0xFFF1F5F9), R.drawable.ic_badge_star)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column {
            Text(
                text = "جزییات عضویت",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            tier?.let { (tierLabel, tierColor, tierBgColor, tierIconRes) ->
                DetailRow(
                    iconResId = tierIconRes,
                    iconTint = tierColor,
                    label = "ترتیب عضویت",
                    value = tierLabel,
                    valueColor = tierColor,
                    iconBg = tierBgColor,
                )
            }
            if (tier != null && joinedAt != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
            joinedAt?.let { date ->
                DetailRow(
                    iconResId = R.drawable.ic_detail_calendar,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "تاریخ عضویت",
                    value = date,
                    valueColor = MaterialTheme.colorScheme.onBackground,
                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
            }


            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AdditionalDetailsCard(profile: ProfileHeaderModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column {
            Text(
                text = "اطلاعات تکمیلی",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            DetailRow(
                iconResId = R.drawable.ic_detail_shield,
                iconTint = if (profile.isVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "وضعیت تایید حساب",
                value = if (profile.isVerified) "تایید شده رسمی" else "تایید نشده",
                valueColor = if (profile.isVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                iconBg = if (profile.isVerified) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            DetailRow(
                iconResId = R.drawable.ic_detail_edit,
                iconTint = MaterialTheme.colorScheme.primary,
                label = "تعداد تغییر نام کاربری",
                value = "۰",
                valueColor = MaterialTheme.colorScheme.onBackground,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatsCard(profile: ProfileHeaderModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "آمار فعالیت",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(count = profile.postCount, label = "پست")
                StatColumn(count = profile.followerCount, label = "دنبال‌کننده")
                StatColumn(count = profile.followingCount, label = "دنبال‌شونده")
            }
        }
    }
}

@Composable
private fun StatColumn(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BioCard(bio: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "بیوگرافی",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = bio,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun AccountIdFooter(userId: String) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "شناسه کاربری (User ID)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = userId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("User ID", userId))
                    Toast.makeText(context, "شناسه کاربری کپی شد", Toast.LENGTH_SHORT).show()
                },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_detail_copy),
                    contentDescription = "کپی شناسه",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    iconResId: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    label: String,
    value: String,
    valueColor: Color,
    iconBg: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconResId),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
