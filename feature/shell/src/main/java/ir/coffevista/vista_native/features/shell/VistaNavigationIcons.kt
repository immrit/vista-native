package ir.coffevista.vista_native.features.shell

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR

@Composable
internal fun VistaNavigationIcon(
    tab: ShellTab,
    selected: Boolean,
    color: Color,
    iconOverride: String = "",
    modifier: Modifier = Modifier,
) {
    val resolvedKey = iconOverride.trim().lowercase().ifBlank {
        when (tab) {
            ShellTab.Feed -> "home"
            ShellTab.Search -> "search"
            ShellTab.Ai -> "sparkles"
            ShellTab.Services -> "grid"
            ShellTab.Chat -> "chat"
            ShellTab.Profile -> "profile"
        }
    }

    Crossfade(
        targetState = Pair(selected, resolvedKey),
        animationSpec = tween(durationMillis = 160),
        label = "vistaNavIconCrossfade",
    ) { (isSelected, iconKey) ->
        val drawableRes = when (iconKey) {
            "home", "feed" -> if (isSelected) DesignSystemR.drawable.ic_nav_home else DesignSystemR.drawable.ic_nav_home_outline
            "search", "explore" -> if (isSelected) DesignSystemR.drawable.ic_nav_search else DesignSystemR.drawable.ic_nav_search_outline
            "grid", "services" -> if (isSelected) DesignSystemR.drawable.ic_nav_services else DesignSystemR.drawable.ic_nav_services_outline
            "chat" -> if (isSelected) DesignSystemR.drawable.ic_nav_chat else DesignSystemR.drawable.ic_nav_chat_outline
            "profile" -> if (isSelected) DesignSystemR.drawable.ic_nav_profile else DesignSystemR.drawable.ic_nav_profile_outline
            else -> null
        }

        if (drawableRes != null) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                tint = color,
                modifier = modifier.size(22.dp),
            )
        } else {
            val vector = when (iconKey) {
                "sparkles", "ai" -> NavVectors.sparkles(filled = isSelected)
                "brain" -> NavVectors.brain()
                "bot" -> NavVectors.bot()
                "compass" -> NavVectors.compass()
                "bolt" -> NavVectors.bolt()
                "gamepad" -> NavVectors.gamepad()
                "bell", "notifications" -> Icons.Rounded.Notifications
                "plus" -> Icons.Rounded.Add
                "star" -> Icons.Rounded.Star
                else -> NavVectors.sparkles(filled = isSelected)
            }
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = color,
                modifier = modifier.size(22.dp),
            )
        }
    }
}

private object NavVectors {
    private val Black = SolidColor(Color.Black)

    fun sparkles(filled: Boolean): ImageVector = if (filled) SparklesFilled else SparklesOutline
    fun brain(): ImageVector = BrainIcon
    fun bot(): ImageVector = BotIcon
    fun compass(): ImageVector = CompassIcon
    fun bolt(): ImageVector = BoltIcon
    fun gamepad(): ImageVector = GamepadIcon

    private val SparklesFilled: ImageVector by lazy {
        ImageVector.Builder("NavSparklesFilled", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = Black, pathFillType = PathFillType.NonZero) {
                moveTo(11f, 2f)
                lineTo(13.1f, 8.9f)
                lineTo(20f, 11f)
                lineTo(13.1f, 13.1f)
                lineTo(11f, 20f)
                lineTo(8.9f, 13.1f)
                lineTo(2f, 11f)
                lineTo(8.9f, 8.9f)
                close()
                moveTo(18.5f, 2.5f)
                lineTo(19.4f, 5.1f)
                lineTo(22f, 6f)
                lineTo(19.4f, 6.9f)
                lineTo(18.5f, 9.5f)
                lineTo(17.6f, 6.9f)
                lineTo(15f, 6f)
                lineTo(17.6f, 5.1f)
                close()
            }
        }.build()
    }

    private val SparklesOutline: ImageVector by lazy {
        ImageVector.Builder("NavSparklesOutline", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = Black,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(11f, 3f)
                lineTo(12.9f, 9.1f)
                lineTo(19f, 11f)
                lineTo(12.9f, 12.9f)
                lineTo(11f, 19f)
                lineTo(9.1f, 12.9f)
                lineTo(3f, 11f)
                lineTo(9.1f, 9.1f)
                close()
                moveTo(18.5f, 3f)
                lineTo(19.2f, 5.3f)
                lineTo(21.5f, 6f)
                lineTo(19.2f, 6.7f)
                lineTo(18.5f, 9f)
                lineTo(17.8f, 6.7f)
                lineTo(15.5f, 6f)
                lineTo(17.8f, 5.3f)
                close()
            }
        }.build()
    }

    private val BrainIcon: ImageVector by lazy {
        ImageVector.Builder("NavBrain", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = Black,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(12f, 5f)
                curveTo(10.5f, 3.5f, 7f, 4f, 6.5f, 6.5f)
                curveTo(4.2f, 7f, 3.5f, 10f, 5f, 12f)
                curveTo(3.8f, 14f, 4.8f, 17.5f, 7.5f, 17.8f)
                curveTo(8.5f, 19.5f, 11f, 20f, 12f, 18.5f)
                moveTo(12f, 5f)
                curveTo(13.5f, 3.5f, 17f, 4f, 17.5f, 6.5f)
                curveTo(19.8f, 7f, 20.5f, 10f, 19f, 12f)
                curveTo(20.2f, 14f, 19.2f, 17.5f, 16.5f, 17.8f)
                curveTo(15.5f, 19.5f, 13f, 20f, 12f, 18.5f)
            }
        }.build()
    }

    private val BotIcon: ImageVector by lazy {
        ImageVector.Builder("NavBot", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = Black,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                verticalLineTo(7f)
                moveTo(5f, 9f)
                lineTo(19f, 9f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
                moveTo(9f, 14f)
                lineTo(9.01f, 14f)
                moveTo(15f, 14f)
                lineTo(15.01f, 14f)
            }
        }.build()
    }

    private val CompassIcon: ImageVector by lazy {
        ImageVector.Builder("NavCompass", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = Black,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2.5f)
                arcTo(9.5f, 9.5f, 0f, true, true, 11.99f, 2.5f)
                close()
                moveTo(15.5f, 8.5f)
                lineTo(13.5f, 13.5f)
                lineTo(8.5f, 15.5f)
                lineTo(10.5f, 10.5f)
                close()
            }
        }.build()
    }

    private val BoltIcon: ImageVector by lazy {
        ImageVector.Builder("NavBolt", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = Black) {
                moveTo(13f, 2f)
                lineTo(4.5f, 13.5f)
                horizontalLineTo(11f)
                lineTo(10f, 22f)
                lineTo(19.5f, 10.5f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
    }

    private val GamepadIcon: ImageVector by lazy {
        ImageVector.Builder("NavGamepad", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = Black,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 8f)
                horizontalLineTo(18f)
                curveTo(20f, 8f, 21.5f, 10f, 21f, 13f)
                lineTo(20.2f, 17f)
                curveTo(19.8f, 18.5f, 18f, 19f, 17f, 18f)
                lineTo(15f, 16f)
                horizontalLineTo(9f)
                lineTo(7f, 18f)
                curveTo(6f, 19f, 4.2f, 18.5f, 3.8f, 17f)
                lineTo(3f, 13f)
                curveTo(2.5f, 10f, 4f, 8f, 6f, 8f)
                close()
                moveTo(7.5f, 11f)
                verticalLineTo(14f)
                moveTo(6f, 12.5f)
                horizontalLineTo(9f)
            }
        }.build()
    }
}
