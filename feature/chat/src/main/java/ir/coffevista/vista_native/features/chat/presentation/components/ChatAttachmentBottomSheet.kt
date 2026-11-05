package ir.coffevista.vista_native.features.chat.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * شیت پیوست فایل، رسانه، عکس و صدا با رابط کاربری اختصاصی.
 * در حالت گالری، باتم‌شیت اختصاصی برنامه به سبک تلگرام نمایش داده می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAttachmentBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
    onFilesSelected: (List<Uri>) -> Unit,
) {
    val context = LocalContext.current
    var isGalleryMode by rememberSaveable { mutableStateOf(false) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val selected = cameraUri
        cameraUri = null
        if (success && selected != null) onMediaSelected(listOf(selected))
        onDismiss()
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onFilesSelected(uris)
        onDismiss()
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onFilesSelected(uris)
        onDismiss()
    }

    val launchCamera: () -> Unit = {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cacheDir, "chat_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.files", file).also { uri ->
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = if (!isGalleryMode) {
                {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }
            } else null,
        ) {
            AnimatedContent(
                targetState = isGalleryMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "attachment-mode-transition",
            ) { inGallery ->
                if (inGallery) {
                    ChatGalleryPickerContent(
                        onDismiss = onDismiss,
                        onBack = { isGalleryMode = false },
                        onMediaSelected = { uris ->
                            onMediaSelected(uris)
                            onDismiss()
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "ارسال پیوست",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 20.dp),
                        )

                        // گزینه‌های اصلی پیوست (گالری اختصاصی، سند، موسیقی، دوربین)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            AttachmentOptionItem(
                                icon = Icons.Default.Image,
                                label = "گالری",
                                gradient = listOf(Color(0xFF29B6F6), Color(0xFF0288D1)),
                                onClick = {
                                    isGalleryMode = true
                                },
                            )

                            AttachmentOptionItem(
                                icon = Icons.Default.Description,
                                label = "سند",
                                gradient = listOf(Color(0xFFE53935), Color(0xFFC62828)),
                                onClick = {
                                    documentPickerLauncher.launch(arrayOf("application/pdf"))
                                },
                            )

                            AttachmentOptionItem(
                                icon = Icons.Default.Audiotrack,
                                label = "موسیقی",
                                gradient = listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2)),
                                onClick = {
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                },
                            )

                            AttachmentOptionItem(
                                icon = Icons.Default.CameraAlt,
                                label = "دوربین",
                                gradient = listOf(Color(0xFFFFA726), Color(0xFFF57C00)),
                                onClick = launchCamera,
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = gradient.first().copy(alpha = 0.4f), spotColor = gradient.last())
                .background(Brush.linearGradient(gradient), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
