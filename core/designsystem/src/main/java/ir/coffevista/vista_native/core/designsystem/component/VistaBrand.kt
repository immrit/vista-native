package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ir.coffevista.vista_native.core.designsystem.R

@Composable
fun VistaAuthLogo(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    Image(
        painter = painterResource(
            if (darkTheme) {
                R.drawable.vista_auth_logo_dark
            } else {
                R.drawable.vista_auth_logo_light
            },
        ),
        contentDescription = "نشان ویستا",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
