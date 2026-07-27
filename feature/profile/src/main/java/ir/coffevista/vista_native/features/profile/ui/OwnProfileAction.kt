package ir.coffevista.vista_native.features.profile.ui

sealed interface OwnProfileAction {
    data object Refresh : OwnProfileAction
    data object Retry : OwnProfileAction
}
