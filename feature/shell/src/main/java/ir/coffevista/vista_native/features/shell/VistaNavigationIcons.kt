package ir.coffevista.vista_native.features.shell

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR

@Composable
internal fun VistaNavigationIcon(
    tab: ShellTab,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = selected,
        animationSpec = tween(durationMillis = 160),
        label = "vistaNavIconCrossfade",
    ) { isSelected ->
        val iconRes = when (tab) {
            ShellTab.Feed -> if (isSelected) DesignSystemR.drawable.ic_nav_home else DesignSystemR.drawable.ic_nav_home_outline
            ShellTab.Search -> if (isSelected) DesignSystemR.drawable.ic_nav_search else DesignSystemR.drawable.ic_nav_search_outline
            ShellTab.Services -> if (isSelected) DesignSystemR.drawable.ic_nav_services else DesignSystemR.drawable.ic_nav_services_outline
            ShellTab.Chat -> if (isSelected) DesignSystemR.drawable.ic_nav_chat else DesignSystemR.drawable.ic_nav_chat_outline
            ShellTab.Profile -> if (isSelected) DesignSystemR.drawable.ic_nav_profile else DesignSystemR.drawable.ic_nav_profile_outline
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = color,
            modifier = modifier.size(24.dp),
        )
    }
}
