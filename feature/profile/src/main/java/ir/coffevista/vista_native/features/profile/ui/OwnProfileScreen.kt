package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaAvatar
import ir.coffevista.vista_native.core.designsystem.component.VistaBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.core.designsystem.component.VistaLoadingState
import ir.coffevista.vista_native.core.designsystem.component.VistaScaffold
import ir.coffevista.vista_native.core.designsystem.component.VistaTopAppBar
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnProfileScreen(
    viewModel: OwnProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VistaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            VistaTopAppBar(title = "نمایه شما")
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is OwnProfileUiState.Loading -> {
                VistaLoadingState(
                    label = "در حال دریافت اطلاعات...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is OwnProfileUiState.Error -> {
                VistaErrorState(
                    title = "خطا در دریافت اطلاعات",
                    message = state.error.messageFa,
                    onRetry = { viewModel.handleAction(OwnProfileAction.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is OwnProfileUiState.Content -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.handleAction(OwnProfileAction.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(VistaLayout.ScreenHorizontal),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(VistaSpacing.Large))
                        
                        VistaAvatar(
                            displayName = state.profile.fullName,
                            modifier = Modifier.padding(bottom = VistaSpacing.Medium)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VistaSpacing.Small)
                        ) {
                            Text(
                                text = state.profile.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.profile.isVerified) {
                                VistaBadge(label = "تایید شده")
                            }
                        }
                        
                        val username = state.profile.username
                        if (!username.isNullOrBlank()) {
                            Text(
                                text = "@$username",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = VistaSpacing.Micro)
                            )
                        }
                        
                        val bio = state.profile.bio
                        if (!bio.isNullOrBlank()) {
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = VistaSpacing.Large)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(VistaSpacing.XLarge))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStat(label = "پست‌ها", value = state.profile.postCount)
                            ProfileStat(label = "دنبال‌کننده‌ها", value = state.profile.followerCount)
                            ProfileStat(label = "دنبال‌شوندگان", value = state.profile.followingCount)
                        }
                        
                        Spacer(modifier = Modifier.height(VistaSpacing.XXLarge))
                        
                        VistaButton(
                            onClick = onLogout,
                            variant = VistaButtonVariant.Destructive,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("خروج از حساب")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
