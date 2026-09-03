package ir.coffevista.vista_native.features.feed.ui.appeal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

enum class AppealReason(val label: String) {
    IncorrectDecision("تصمیم تعدیل اشتباه بوده است"),
    ContextMissing("زمینهٔ محتوا در نظر گرفته نشده است"),
    Ownership("من مالک این محتوا هستم"),
    Other("دلیل دیگر"),
}

data class AppealUiState(
    val selectedReason: AppealReason = AppealReason.IncorrectDecision,
    val details: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AppealViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppealUiState())
    val uiState: StateFlow<AppealUiState> = mutableUiState.asStateFlow()

    fun selectReason(reason: AppealReason) {
        mutableUiState.update { it.copy(selectedReason = reason, errorMessage = null) }
    }

    fun updateDetails(value: String) {
        mutableUiState.update { it.copy(details = value, errorMessage = null) }
    }

    fun submit(postId: String) {
        val state = uiState.value
        val details = state.details.trim()
        if (details.length < 10) {
            mutableUiState.update { it.copy(errorMessage = "توضیحات اعتراض باید حداقل ۱۰ نویسه باشد") }
            return
        }
        if (state.isSubmitting || state.isSubmitted || postId.isBlank()) return

        val requestReason = "${state.selectedReason.label}\n\n$details"
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching { feedRepository.submitAppeal(postId, requestReason) }
                .onSuccess {
                    mutableUiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                }
                .onFailure { throwable ->
                    val message = when ((throwable as? HttpException)?.code()) {
                        409 -> "اعتراض قبلی شما هنوز در حال بررسی است"
                        404 -> "پستِ قابل اعتراض پیدا نشد"
                        else -> "ثبت اعتراض ناموفق بود؛ دوباره تلاش کنید"
                    }
                    mutableUiState.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealScreen(
    postId: String,
    onBack: () -> Unit,
) {
    val viewModel: AppealViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت اعتراض") },
                navigationIcon = { androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (state.isSubmitted) {
            AppealSuccess(
                onBack = onBack,
                modifier = Modifier.padding(padding),
            )
        } else {
            AppealForm(
                state = state,
                onReasonSelected = viewModel::selectReason,
                onDetailsChanged = viewModel::updateDetails,
                onSubmit = { viewModel.submit(postId) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppealForm(
    state: AppealUiState,
    onReasonSelected: (AppealReason) -> Unit,
    onDetailsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null)
                Spacer(Modifier.height(8.dp))
                Text("اعتراض شما توسط تیم ویستا بررسی می‌شود.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "لطفاً دلیل خود را دقیق و محترمانه توضیح دهید. نتیجهٔ بررسی از طریق اعلان اطلاع‌رسانی خواهد شد.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text("دلیل اعتراض", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = !menuExpanded }) {
            OutlinedTextField(
                value = state.selectedReason.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("انتخاب دلیل") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                AppealReason.entries.forEach { reason ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(reason.label) },
                        onClick = { onReasonSelected(reason); menuExpanded = false },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.details,
            onValueChange = onDetailsChanged,
            label = { Text("توضیحات") },
            supportingText = { Text("حداقل ۱۰ نویسه؛ ${state.details.length}/2000") },
            minLines = 6,
            maxLines = 10,
            isError = state.errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Outlined.Gavel, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ارسال اعتراض")
            }
        }
    }
}

@Composable
private fun AppealSuccess(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.height(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("اعتراض شما ثبت شد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("پس از بررسی، نتیجه از طریق اعلان به شما اطلاع داده می‌شود.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) { Text("بازگشت") }
    }
}
