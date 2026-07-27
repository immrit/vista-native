package ir.coffevista.vista_native.features.profile.ui

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity

sealed interface OwnProfileUiState {
    data object Loading : OwnProfileUiState
    data class Content(
        val profile: OwnProfileEntity,
        val isRefreshing: Boolean = false
    ) : OwnProfileUiState
    data class Error(val error: AppError) : OwnProfileUiState
}
