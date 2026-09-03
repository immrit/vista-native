package ir.coffevista.vista_native.features.services.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
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
}

@Composable
fun ServicesActionIcon(
    kind: ServicesActionKind,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val imageVector: ImageVector = when (kind) {
        ServicesActionKind.NEARBY -> Icons.Rounded.Radar
        ServicesActionKind.GAME -> Icons.Rounded.SportsEsports
        ServicesActionKind.GROUPS -> Icons.Rounded.Groups
        ServicesActionKind.CONTACTS -> Icons.Rounded.ContactPage
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
    val imageVector: ImageVector = when (name.lowercase()) {
        "refresh" -> Icons.Rounded.Refresh
        "arrow_back" -> Icons.AutoMirrored.Rounded.ArrowBack
        "arrow_forward" -> Icons.AutoMirrored.Rounded.ArrowForward
        "chevron_left" -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
        "chevron_right" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        "close" -> Icons.Rounded.Close
        "people", "groups" -> Icons.Rounded.People
        "contact_page", "contacts" -> Icons.Rounded.ContactPage
        "trophy", "emoji_events" -> Icons.Rounded.EmojiEvents
        "star" -> Icons.Rounded.Star
        "verified" -> Icons.Rounded.Verified
        "premium", "workspace_premium" -> Icons.Rounded.WorkspacePremium
        "lock_open" -> Icons.Rounded.LockOpen
        "error_outline" -> Icons.Rounded.ErrorOutline
        "cloud_off" -> Icons.Rounded.CloudOff
        "grid" -> Icons.Rounded.GridView
        "favorite" -> Icons.Rounded.Favorite
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
        "search" -> Icons.Rounded.Search
        "sync" -> Icons.Rounded.Sync
        "shuffle" -> Icons.Rounded.Shuffle
        "replay" -> Icons.Rounded.Replay
        "phone" -> Icons.Rounded.Phone
        "send" -> Icons.AutoMirrored.Rounded.Send
        else -> Icons.Rounded.Circle
    }
    Icon(
        imageVector = imageVector,
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
    val imageVector: ImageVector = when (iconName.lowercase()) {
        "location" -> Icons.Rounded.LocationOn
        "trophy" -> Icons.Rounded.EmojiEvents
        "bolt" -> Icons.Rounded.Bolt
        "stars", "star" -> Icons.Rounded.Star
        "chat" -> Icons.AutoMirrored.Rounded.Chat
        "phone" -> Icons.Rounded.Phone
        "person_add" -> Icons.Rounded.PersonAdd
        "game", "gamepad" -> Icons.Rounded.SportsEsports
        "group", "people" -> Icons.Rounded.People
        else -> Icons.Rounded.Star
    }
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}
