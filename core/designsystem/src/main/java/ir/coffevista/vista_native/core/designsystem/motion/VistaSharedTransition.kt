package ir.coffevista.vista_native.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalVistaSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalVistaAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val VistaSharedBoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.85f, stiffness = 450f)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.vistaSharedBounds(key: Any): Modifier {
    val sharedScope = LocalVistaSharedTransitionScope.current
    val animatedScope = LocalVistaAnimatedVisibilityScope.current
    if (sharedScope == null || animatedScope == null) return this
    return with(sharedScope) {
        this@vistaSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = VistaSharedBoundsTransform,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.vistaSharedElement(key: Any): Modifier {
    val sharedScope = LocalVistaSharedTransitionScope.current
    val animatedScope = LocalVistaAnimatedVisibilityScope.current
    if (sharedScope == null || animatedScope == null) return this
    return with(sharedScope) {
        this@vistaSharedElement.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = VistaSharedBoundsTransform,
        )
    }
}
