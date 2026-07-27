package ir.coffevista.vista_native.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import ir.coffevista.vista_native.core.designsystem.component.VistaAvatar
import ir.coffevista.vista_native.core.designsystem.component.VistaBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaCard
import ir.coffevista.vista_native.core.designsystem.component.VistaTextField
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing

@Preview(name = "Components Light RTL", locale = "fa", showBackground = true)
@Preview(name = "Components Dark RTL", locale = "fa", uiMode = 0x20, showBackground = true)
@Preview(name = "Components Light LTR", locale = "en", showBackground = true)
@Composable
private fun VistaComponentCatalogPreview() {
    VistaTheme {
        Column(
            modifier = Modifier.padding(VistaSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(VistaSpacing.Medium),
        ) {
            VistaButton(onClick = {}) { androidx.compose.material3.Text("ادامه") }
            VistaButton(onClick = {}, loading = true, variant = VistaButtonVariant.Outline) {
                androidx.compose.material3.Text("بارگذاری")
            }
            VistaTextField(
                value = "نمونه فارسی",
                onValueChange = {},
                label = "عنوان",
                supportingText = "متن راهنما",
                modifier = Modifier.fillMaxWidth(),
            )
            VistaAvatar("کاربر ویستا")
            VistaBadge("جدید")
            VistaMediaCard("زیرساخت رسانه", "داده نمایشی محصول نیست", loading = false)
            VistaMediaCard("", "", loading = true)
            VistaEmptyState("هنوز محتوایی نیست", "این یک وضعیت پایه Design System است.")
        }
    }
}
