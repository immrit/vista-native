package ir.coffevista.vista_native.features.services.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ServicesActionKind {
    NEARBY,
    GAME,
    GROUPS,
    CONTACTS,
    CUSTOM,
}

internal fun resolveServiceIconVector(
    name: String,
    fallback: ImageVector = Icons.Rounded.GridView,
): ImageVector = when (name.trim().lowercase()) {
    "radar", "nearby" -> Icons.Rounded.Radar
    "refresh" -> Icons.Rounded.Refresh
    "arrow_back" -> Icons.AutoMirrored.Rounded.ArrowBack
    "arrow_forward" -> Icons.AutoMirrored.Rounded.ArrowForward
    "chevron_left" -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
    "chevron_right" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
    "close" -> Icons.Rounded.Close
    "people", "groups" -> Icons.Rounded.Groups
    "contact_page", "contacts" -> Icons.Rounded.ContactPage
    "trophy", "emoji_events" -> Icons.Rounded.EmojiEvents
    "star", "stars" -> Icons.Rounded.Star
    "verified" -> Icons.Rounded.Verified
    "premium", "workspace_premium", "crown" -> Icons.Rounded.WorkspacePremium
    "lock_open" -> Icons.Rounded.LockOpen
    "error_outline" -> Icons.Rounded.ErrorOutline
    "cloud_off" -> Icons.Rounded.CloudOff
    "grid", "apps" -> Icons.Rounded.GridView
    "favorite", "heart" -> Icons.Rounded.Favorite
    "favorite_border" -> Icons.Rounded.FavoriteBorder
    "tune", "settings" -> Icons.Rounded.Tune
    "location", "location_on" -> Icons.Rounded.LocationOn
    "location_off" -> Icons.Rounded.LocationOff
    "flag" -> Icons.Outlined.Flag
    "chat" -> Icons.AutoMirrored.Rounded.Chat
    "share" -> Icons.Rounded.Share
    "person" -> Icons.Rounded.Person
    "person_add" -> Icons.Rounded.PersonAdd
    "female" -> Icons.Rounded.Female
    "male" -> Icons.Rounded.Male
    "play_arrow" -> Icons.Rounded.PlayArrow
    "bolt", "flash" -> Icons.Rounded.Bolt
    "fire", "hot", "local_fire_department" -> Icons.Rounded.LocalFireDepartment
    "search" -> Icons.Rounded.Search
    "sync" -> Icons.Rounded.Sync
    "shuffle" -> Icons.Rounded.Shuffle
    "replay" -> Icons.Rounded.Replay
    "phone" -> Icons.Rounded.Phone
    "send" -> Icons.AutoMirrored.Rounded.Send
    "game", "gamepad", "sports_esports" -> Icons.Rounded.SportsEsports
    "shopping_bag", "shop" -> Icons.Rounded.ShoppingBag
    "store", "storefront" -> Icons.Rounded.Storefront
    "music", "music_note" -> Icons.Rounded.MusicNote
    "sparkles", "ai", "auto_awesome" -> Icons.Rounded.AutoAwesome
    "news", "article" -> Icons.Rounded.Article
    "wallet", "payments" -> Icons.Rounded.AccountBalanceWallet
    "gift", "card_giftcard" -> Icons.Rounded.CardGiftcard
    "ticket", "confirmation_number" -> Icons.Rounded.ConfirmationNumber
    "book", "menu_book" -> Icons.AutoMirrored.Rounded.MenuBook
    "movie", "play_circle", "video" -> Icons.Rounded.PlayCircle
    "map", "explore" -> Icons.Rounded.Explore
    "shield", "verified_user" -> Icons.Rounded.VerifiedUser
    "support", "support_agent" -> Icons.Rounded.SupportAgent
    "calendar", "event" -> Icons.Rounded.Event
    "camera", "photo_camera" -> Icons.Rounded.PhotoCamera
    "code", "terminal" -> Icons.Rounded.Terminal
    "chart", "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
    "compass", "public", "web" -> Icons.Rounded.Public
    else -> fallback
}

@Composable
fun ServicesActionIcon(
    kind: ServicesActionKind,
    color: Color,
    modifier: Modifier = Modifier,
    iconName: String = "",
) {
    val defaultVector: ImageVector = when (kind) {
        ServicesActionKind.NEARBY -> Icons.Rounded.Radar
        ServicesActionKind.GAME -> Icons.Rounded.SportsEsports
        ServicesActionKind.GROUPS -> Icons.Rounded.Groups
        ServicesActionKind.CONTACTS -> Icons.Rounded.ContactPage
        ServicesActionKind.CUSTOM -> Icons.Rounded.GridView
    }
    val imageVector = if (iconName.isNotBlank()) {
        resolveServiceIconVector(iconName, fallback = defaultVector)
    } else {
        defaultVector
    }
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}

@Composable
fun ServicesVectorIcon(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = resolveServiceIconVector(name, fallback = Icons.Rounded.Circle),
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}

@Composable
fun ServicesMotifIcon(
    iconName: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = resolveServiceIconVector(iconName, fallback = Icons.Rounded.Star),
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}
