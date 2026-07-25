package ir.coffevista.vista_native.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.R

enum class VistaBrandAsset {
    MARK,
    SPLASH,
    AUTH,
}

@Composable
fun VistaBrandMark(
    modifier: Modifier = Modifier,
    asset: VistaBrandAsset = VistaBrandAsset.MARK,
) {
    @DrawableRes val drawable = when (asset) {
        VistaBrandAsset.MARK -> R.drawable.vista_logo_mark
        VistaBrandAsset.SPLASH -> R.drawable.vista_logo_splash
        VistaBrandAsset.AUTH -> if (isSystemInDarkTheme()) {
            R.drawable.vista_logo_auth_dark
        } else {
            R.drawable.vista_logo_auth_light
        }
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = "نشان ویستا",
        modifier = modifier.size(if (asset == VistaBrandAsset.SPLASH) 200.dp else 104.dp),
        contentScale = ContentScale.Fit,
    )
}
