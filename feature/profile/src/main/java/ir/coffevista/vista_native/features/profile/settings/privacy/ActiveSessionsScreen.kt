package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection

data class SessionItemUiModel(
    val id: String,
    val deviceName: String,
    val platform: String,
    val ip: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveSessionsViewModel = hiltViewModel(),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    var showTerminateAllDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSession = uiState.sessions.firstOrNull(SessionItemUiModel::isCurrent)
    val otherSessions = uiState.sessions.filterNot(SessionItemUiModel::isCurrent)
    val context = LocalContext.current
    uiState.feedbackMessage?.let { message ->
        LaunchedEffect(message) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "امنیت و نشست‌ها",
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
                    actions = {
                        IconButton(onClick = viewModel::refresh, enabled = !uiState.isLoading && !uiState.isMutating) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "به‌روزرسانی",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Current Device Hero Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Pulsing green circle
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                                    .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(44.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Online Tag
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50)),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentSession != null) "این دستگاه" else "وضعیت نامشخص",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        ),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentSession?.deviceName ?: "نشست فعلی تشخیص داده نشد",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                ),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = currentSession?.let { "${it.lastActive} • ${it.location}" }
                                    ?: "برای نمایش نشست فعلی، لیست را به‌روزرسانی کنید",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.errorMessage != null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(uiState.errorMessage!!, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = viewModel::refresh) { Text("تلاش مجدد") }
                        }
                    }
                } else if (otherSessions.isNotEmpty()) {
                    item {
                        VistaSettingsSection(title = "سایر دستگاه‌ها")
                    }

                    items(otherSessions, key = { it.id }) { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Terminate button
                                IconButton(
                                    onClick = {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            viewModel.authorizeSensitiveAction(activity) { viewModel.terminate(session) }
                                        } else {
                                            Toast.makeText(context, "امکان تایید هویت در این صفحه وجود ندارد", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !uiState.isMutating && currentSession != null,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "پایان نشست",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = session.deviceName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${session.lastActive} • ${session.location}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp,
                                        ),
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = when (session.platform) {
                                            "ios" -> Icons.Outlined.PhoneIphone
                                            "desktop" -> Icons.Outlined.Laptop
                                            else -> Icons.Outlined.PhoneAndroid
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { showTerminateAllDialog = true },
                            enabled = !uiState.isMutating && currentSession != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE53935),
                            ),
                        ) {
                            Text(
                                text = "خروج از سایر نشست‌ها",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        if (showTerminateAllDialog) {
            AlertDialog(
                onDismissRequest = { showTerminateAllDialog = false },
                title = {
                    Text(
                        text = "تایید خاتمه نشست‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Text(
                        text = "برای خروج از تمام دستگاه‌های دیگر، هویت خود را تایید کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTerminateAllDialog = false
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                viewModel.authorizeSensitiveAction(activity) { viewModel.terminateAllOther() }
                            } else {
                                Toast.makeText(context, "امکان تایید هویت در این صفحه وجود ندارد", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Text(
                            text = "تایید",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTerminateAllDialog = false }) {
                        Text("انصراف")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}
