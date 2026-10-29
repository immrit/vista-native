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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
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
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val contacts = queryDeviceContacts(context)
            viewModel.syncContacts(contacts)
        } else {
            viewModel.onContactsPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val contacts = queryDeviceContacts(context)
            viewModel.syncContacts(contacts)
        }
    }

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
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_CONTACTS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val contacts = queryDeviceContacts(context)
                                    viewModel.syncContacts(contacts)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            onOpenAppSettings = { openAppSettings(context) },
                        )
                    }
                    is ContactsRailUiState.Empty -> {
                        EmptyContactsCard(
                            onInvite = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "سلام! من در برنامه ویستا عضوم. بیا اینجا با هم در ارتباط باشیم:\nhttps://cafevista.ir"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, "دعوت دوستان به ویستا"))
                            }
                        )
                    }
                    is ContactsRailUiState.Success -> {
                        val filteredUsers = remember(contactsState.users, searchQuery) {
                            if (searchQuery.isBlank()) {
                                contactsState.users
                            } else {
                                contactsState.users.filter {
                                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                                        it.username.contains(searchQuery, ignoreCase = true) ||
                                        it.phoneNumber.contains(searchQuery)
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            // Search bar
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.vistaColors.contentSecondary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = VistaFontFamily,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.5.sp,
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(VistaBrandColors.Indigo),
                                            decorationBox = { innerTextField ->
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "جستجوی مخاطب در ویستا...",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontFamily = VistaFontFamily,
                                                            color = MaterialTheme.vistaColors.contentSecondary.copy(alpha = 0.7f),
                                                            fontSize = 13.5.sp,
                                                        ),
                                                    )
                                                }
                                                innerTextField()
                                            },
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Clear,
                                                    contentDescription = "پاک کردن",
                                                    tint = MaterialTheme.vistaColors.contentSecondary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Header count pill
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = VistaBrandColors.Indigo.copy(alpha = 0.08f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.People,
                                            contentDescription = null,
                                            tint = VistaBrandColors.Indigo,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${contactsState.users.size} نفر از مخاطبین شما در ویستا حضور دارند",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VistaFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                color = VistaBrandColors.Indigo,
                                                fontSize = 12.5.sp,
                                            ),
                                        )
                                    }
                                }
                            }

                            if (filteredUsers.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "مخاطبی با این نام یا شماره پیدا نشد",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = VistaFontFamily,
                                                color = MaterialTheme.vistaColors.contentSecondary,
                                                fontSize = 13.sp,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                items(filteredUsers, key = { it.id }) { user ->
                                    ContactItemRow(
                                        user = user,
                                        onClick = { onContactClick(user.id) },
                                    )
                                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "کاربر تایید شده",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VistaBrandColors.Indigo.copy(alpha = 0.08f),
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "پیام",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = VistaBrandColors.Indigo,
                        fontSize = 11.5.sp,
                    ),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = VistaBrandColors.Indigo,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onGrantPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
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
                TextButton(onClick = onOpenAppSettings) {
                    Text(
                        text = "باز کردن تنظیمات برنامه",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = VistaFontFamily,
                            color = VistaBrandColors.Indigo,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsCard(
    onInvite: () -> Unit,
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(VistaBrandColors.Indigo.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.People,
                        contentDescription = null,
                        tint = VistaBrandColors.Indigo,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "هنوز از مخاطبینت کسی عضو ویستا نشده",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "دوستانت را با ارسال لینک اختصاصی به ویستا دعوت کن تا با هم گپ بزنید.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onInvite,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دعوت دوستان به ویستا",
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

private fun queryDeviceContacts(context: Context): List<String> {
    val phoneNumbers = LinkedHashSet<String>()
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
    runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numberIndex != -1) {
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(numberIndex)
                    if (!raw.isNullOrBlank()) {
                        val cleaned = raw.replace("[\\s\\-\\(\\)]".toRegex(), "")
                        if (cleaned.isNotBlank()) {
                            phoneNumbers.add(cleaned)
                        }
                    }
                }
            }
        }
    }
    return phoneNumbers.toList()
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        },
    )
}
