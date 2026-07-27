package ir.coffevista.vista_native.features.shell

import kotlinx.serialization.Serializable

@Serializable data object FeedGraph
@Serializable data object FeedRoot
@Serializable data class FeedDetail(val reference: String = "foundation")

@Serializable data object SearchGraph
@Serializable data object SearchRoot
@Serializable data class SearchDetail(val reference: String = "foundation")

@Serializable data object ServicesGraph
@Serializable data object ServicesRoot
@Serializable data class ServicesDetail(val reference: String = "foundation")

@Serializable data object ChatGraph
@Serializable data object ChatRoot
@Serializable data class ChatDetail(val reference: String = "foundation")

@Serializable data object ProfileGraph
@Serializable data object ProfileRoot
@Serializable data class ProfileDetail(val reference: String = "foundation")
