package ir.coffevista.vista_native.features.stories.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.ui.player.StoryElementsOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    onClose: () -> Unit,
    onStoryPublished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoryEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    var showStickerSheet by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("بله") }
    var pollOption2 by remember { mutableStateOf("خیر") }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }

    var showMentionDialog by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }

    // Media Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri, isVideo = false)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri, isVideo = true)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onStoryPublished()
        }
    }

    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            if (uiState.mediaUri == null) {
                // Media Picker Selection View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "ایجاد استوری جدید",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "یک عکس یا ویدیو برای استوری خود انتخاب کنید",
                            fontSize = 13.sp,
                            color = Color.Gray,
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Card(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                modifier = Modifier.weight(1f).height(140.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = VistaBrandColors.Indigo,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("عکس از گالری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Card(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                modifier = Modifier.weight(1f).height(140.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = VistaBrandColors.Pink,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("ویدیو از گالری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        TextButton(onClick = onClose) {
                            Text("انصراف و بازگشت", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                // Active Story Editor View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Media background
                    AsyncImage(
                        model = uiState.mediaUri,
                        contentDescription = "پیش‌نمایش استوری",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    // Interactive Elements
                    StoryElementsOverlay(
                        elements = uiState.elements,
                        onPollVote = {},
                        onLinkClick = {},
                        onMentionClick = {},
                    )

                    // Top Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Text tool button
                            IconButton(
                                onClick = { showTextDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = "متن", tint = Color.White)
                            }

                            // Sticker tool button
                            IconButton(
                                onClick = { showStickerSheet = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Default.Poll, contentDescription = "استیکرها", tint = Color.White)
                            }

                            // 24h / 48h Duration Chip
                            FilterChip(
                                selected = uiState.durationType == StoryDurationType.Hours48,
                                onClick = {
                                    viewModel.setDuration(
                                        if (uiState.durationType == StoryDurationType.Hours24) StoryDurationType.Hours48
                                        else StoryDurationType.Hours24,
                                    )
                                },
                                label = {
                                    Text(
                                        if (uiState.durationType == StoryDurationType.Hours48) "۴۸ ساعت (طلایی)" else "۲۴ ساعت",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    selectedContainerColor = VistaBrandColors.Indigo,
                                ),
                            )
                        }
                    }

                    // Upload Progress Indicator
                    if (uiState.isUploading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = uiState.uploadStatus ?: "در حال انتشار استوری…",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Bottom Action Bar (Privacy & Publish Button)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Privacy Selector
                        FilterChip(
                            selected = uiState.privacyType == StoryPrivacyType.CloseFriends,
                            onClick = {
                                viewModel.setPrivacy(
                                    if (uiState.privacyType == StoryPrivacyType.Everyone) StoryPrivacyType.CloseFriends
                                    else StoryPrivacyType.Everyone,
                                )
                            },
                            label = {
                                Text(
                                    if (uiState.privacyType == StoryPrivacyType.CloseFriends) "دوستان صمیمی" else "همه کاربران",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                selectedContainerColor = Color(0xFF2E7D32),
                            ),
                        )

                        // Publish Button
                        Button(
                            onClick = viewModel::publishStory,
                            enabled = uiState.canPublish,
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VistaBrandColors.Indigo,
                                contentColor = Color.White,
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ارسال به استوری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Text Tool Dialog
            if (showTextDialog) {
                AlertDialog(
                    onDismissRequest = { showTextDialog = false },
                    title = { Text("افزودن متن به استوری") },
                    text = {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("متن خود را بنویسید…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addTextElement(textInput)
                            textInput = ""
                            showTextDialog = false
                        }) {
                            Text("تایید")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTextDialog = false }) { Text("انصراف") }
                    },
                )
            }

            // Sticker Selector Bottom Sheet
            if (showStickerSheet) {
                ModalBottomSheet(onDismissRequest = { showStickerSheet = false }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("انتخاب استیکر تعاملی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            StickerOption(icon = Icons.Default.Poll, label = "نظرسنجی") {
                                showStickerSheet = false
                                showPollDialog = true
                            }
                            StickerOption(icon = Icons.Default.Link, label = "لینک") {
                                showStickerSheet = false
                                showLinkDialog = true
                            }
                            StickerOption(icon = Icons.Default.AlternateEmail, label = "منشن") {
                                showStickerSheet = false
                                showMentionDialog = true
                            }
                            StickerOption(icon = Icons.Default.AddLocation, label = "مکان") {
                                showStickerSheet = false
                                showLocationDialog = true
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Poll Dialog
            if (showPollDialog) {
                AlertDialog(
                    onDismissRequest = { showPollDialog = false },
                    title = { Text("ایجاد نظرسنجی") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                placeholder = { Text("سوال نظرسنجی…") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = pollOption1,
                                onValueChange = { pollOption1 = it },
                                placeholder = { Text("گزینه اول") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = pollOption2,
                                onValueChange = { pollOption2 = it },
                                placeholder = { Text("گزینه دوم") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addPollElement(pollQuestion, pollOption1, pollOption2)
                            pollQuestion = ""
                            showPollDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPollDialog = false }) { Text("انصراف") }
                    },
                )
            }

            // Link Dialog
            if (showLinkDialog) {
                AlertDialog(
                    onDismissRequest = { showLinkDialog = false },
                    title = { Text("افزودن پیوند اینترنتی") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = linkUrl,
                                onValueChange = { linkUrl = it },
                                placeholder = { Text("https://example.com") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = linkTitle,
                                onValueChange = { linkTitle = it },
                                placeholder = { Text("عنوان نمایشی (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addLinkElement(linkUrl, linkTitle.ifBlank { null })
                            linkUrl = ""
                            linkTitle = ""
                            showLinkDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLinkDialog = false }) { Text("انصراف") }
                    },
                )
            }

            // Mention Dialog
            if (showMentionDialog) {
                AlertDialog(
                    onDismissRequest = { showMentionDialog = false },
                    title = { Text("منشن کاربر") },
                    text = {
                        OutlinedTextField(
                            value = mentionUsername,
                            onValueChange = { mentionUsername = it },
                            placeholder = { Text("نام کاربری (بدون @)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addMentionElement(mentionUsername)
                            mentionUsername = ""
                            showMentionDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMentionDialog = false }) { Text("انصراف") }
                    },
                )
            }

            // Location Dialog
            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    title = { Text("افزودن برچسب مکان") },
                    text = {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            placeholder = { Text("نام مکان یا شهر…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addLocationElement(locationName)
                            locationName = ""
                            showLocationDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLocationDialog = false }) { Text("انصراف") }
                    },
                )
            }
        }
    }
}

@Composable
private fun StickerOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
