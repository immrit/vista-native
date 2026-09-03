package ir.coffevista.vista_native.features.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "vistaNavScale",
    )
    val iconRes = when (tab) {
        ShellTab.Feed -> if (selected) DesignSystemR.drawable.ic_nav_home else DesignSystemR.drawable.ic_nav_home_outline
        ShellTab.Search -> DesignSystemR.drawable.ic_nav_search
        ShellTab.Services -> DesignSystemR.drawable.ic_nav_services
        ShellTab.Chat -> if (selected) DesignSystemR.drawable.ic_nav_chat else DesignSystemR.drawable.ic_nav_chat_outline
        ShellTab.Profile -> if (selected) DesignSystemR.drawable.ic_nav_profile else DesignSystemR.drawable.ic_nav_profile_outline
    }
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(24.dp)
            .scale(scale),
    )
}

