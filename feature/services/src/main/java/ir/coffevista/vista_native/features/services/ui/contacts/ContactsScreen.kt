package ir.coffevista.vista_native.features.services.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.ContactVistaUser
import ir.coffevista.vista_native.features.services.ui.ContactsRailUiState
import ir.coffevista.vista_native.features.services.ui.ServicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    onContactClick: (String) -> Unit,
    viewModel: ServicesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "مخاطبین در ویستا",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (val contactsState = state.contactsState) {
                    is ContactsRailUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = VistaBrandColors.Indigo,
                            )
                        }
                    }
                    is ContactsRailUiState.PermissionRequired -> {
                        PermissionRequiredCard(
                            onGrantPermission = {
                                // Trigger permission request
                            },
                        )
                    }
                    is ContactsRailUiState.Empty -> {
                        EmptyContactsCard()
                    }
                    is ContactsRailUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(contactsState.users, key = { it.id }) { user ->
                                ContactItemRow(
                                    user = user,
                                    onClick = { onContactClick(user.id) },
                                )
                            }
                        }
                    }
                    is ContactsRailUiState.Error -> {
                        ErrorCard(
                            message = contactsState.message,
                            onRetry = { viewModel.loadHub() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    user: ContactVistaUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (user.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(VistaBrandColors.Indigo.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.fullName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VistaBrandColors.Indigo,
                        fontSize = 18.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.vistaColors.contentSecondary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContactPage,
                    contentDescription = null,
                    tint = MaterialTheme.vistaColors.contentSecondary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "برای پیدا کردن دوستانت، دسترسی به مخاطبین لازم است",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onGrantPermission,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اجازه دسترسی",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.People,
                    contentDescription = null,
                    tint = MaterialTheme.vistaColors.contentSecondary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هنوز کسی از مخاطبینت ویستا نصب نکرده",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "خطا در بارگذاری مخاطبین",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.vistaColors.contentSecondary,
                ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = "تلاش مجدد",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = VistaFontFamily,
                        color = VistaBrandColors.Indigo,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
