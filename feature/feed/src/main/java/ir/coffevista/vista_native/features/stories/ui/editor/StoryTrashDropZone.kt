package ir.coffevista.vista_native.features.stories.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.StoryTrashDropZone(
    isVisible: Boolean,
    isHovered: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 44.dp),
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isHovered) 1.35f else 1f,
            label = "trash_scale",
        )
        val bgColor = if (isHovered) Color(0xFFE53935) else Color.Black.copy(alpha = 0.65f)
        val borderColor = if (isHovered) Color.White else Color.White.copy(alpha = 0.35f)

        Box(
            modifier = Modifier
                .scale(scale)
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "حذف المان",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
