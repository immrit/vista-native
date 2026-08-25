package ir.coffevista.vista_native.features.profile.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onBack: () -> Unit,
    onOpenSupportConversation: (conversationId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactUsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullName by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var subjectText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(uiState.successNonce) {
        if (uiState.successNonce > 0L) {
            fullName = ""
            emailText = ""
            subjectText = ""
            messageText = ""
            snackbarHostState.showSnackbar("پیام شما با موفقیت ارسال شد. با تشکر از همراهی شما.")
        }
    }
    uiState.supportConversationId?.let { conversationId ->
        LaunchedEffect(conversationId) {
            onOpenSupportConversation(conversationId, uiState.supportConversationTitle)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "تماس با ما",
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

                VistaSettingsSection(title = "راه‌های ارتباطی")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Email,
                        iconColor = Color(0xFF2196F3),
                        iconContainerColor = Color(0xFF2196F3).copy(alpha = 0.12f),
                        title = "ایمیل پشتیبانی",
                        subtitle = "info@cafevista.ir",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:info@cafevista.ir")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.HeadsetMic,
                        iconColor = Color(0xFF4CAF50),
                        iconContainerColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        title = "پشتیبانی درون‌برنامه‌ای",
                        subtitle = "گفتگوی آنلاین با تیم پشتیبانی ویستا",
                        onClick = {
                            viewModel.openSupportChat()
                        },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feedback Form
                VistaSettingsSection(title = "ارسال پیام یا بازخورد")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام و نام خانوادگی") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("ایمیل") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("موضوع") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            label = { Text("متن پیام یا نظر شما") },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.submit(fullName, emailText, subjectText, messageText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isSubmitting,
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else Text("ارسال پیام", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
