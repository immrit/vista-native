package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.util.JsonReader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage

object TelegramEmojiLookup {
    private val emojiToAsset = mutableMapOf<String, String>()
    @Volatile
    private var isLoaded = false

    fun initialize(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                context.assets.open("emoji/modern_emoji_map.json").use { inputStream ->
                    val reader = JsonReader(inputStream.bufferedReader())
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if (name == "map") {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val emoji = reader.nextName()
                                val path = reader.nextString()
                                val fileName = path.substringAfterLast("/")
                                val assetUrl = "file:///android_asset/emoji/modern/$fileName"
                                emojiToAsset[emoji] = assetUrl

                                val withoutVs16 = emoji.replace("\uFE0F", "")
                                if (withoutVs16 != emoji) {
                                    emojiToAsset.putIfAbsent(withoutVs16, assetUrl)
                                }
                            }
                            reader.endObject()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                    reader.close()
                }
                isLoaded = true
            } catch (e: Exception) {
                isLoaded = true
            }
        }
    }

    fun getAssetUrl(emoji: String): String? {
        val direct = emojiToAsset[emoji]
        if (direct != null) return direct

        val withoutVs16 = emoji.replace("\uFE0F", "")
        return emojiToAsset[withoutVs16]
    }
}

/**
 * کامپوننت رندر اختصاصی ایموجی‌های گرافیکی تلگرام (Apple-Style)
 */
@Composable
fun TelegramEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = remember(size, density) { with(density) { size.roundToPx() } }
    val bitmap = remember(emoji, sizePx) {
        TelegramEmojiSpan.getBitmap(context, emoji, sizePx)
    }

    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        androidx.compose.foundation.Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription ?: emoji,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.85f).sp,
            )
        }
    }
}
