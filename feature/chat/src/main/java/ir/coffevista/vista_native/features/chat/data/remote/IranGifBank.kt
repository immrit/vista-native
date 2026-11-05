package ir.coffevista.vista_native.features.chat.data.remote

import ir.coffevista.vista_native.features.chat.domain.model.GifItem

/**
 * Built-in curated catalog of popular chat reaction GIFs with Persian and English keywords.
 * Guaranteed to be available offline and under national intranet filtering in Iran.
 */
object IranGifBank {

    data class CuratedGif(
        val id: String,
        val url: String,
        val width: Int,
        val height: Int,
        val tags: List<String>,
    ) {
        fun toGifItem(): GifItem = GifItem(
            id = id,
            url = IranMediaProxy.toIranAccessibleUrl(url, preview = false),
            previewUrl = IranMediaProxy.toIranAccessibleUrl(url, preview = true),
            width = width,
            height = height,
        )
    }

    private val ITEMS = listOf(
        // Laughing / خنده
        CuratedGif(
            id = "curated_laugh_cat",
            url = "https://media.giphy.com/media/ICOgUNjpvO0PC/giphy.gif",
            width = 320,
            height = 240,
            tags = listOf("خنده", "خنده‌دار", "گربه", "laugh", "funny", "cat", "lol", "hahaha", "جوک"),
        ),
        CuratedGif(
            id = "curated_laugh_minion",
            url = "https://media.giphy.com/media/10JhviFuU2gWD6/giphy.gif",
            width = 300,
            height = 200,
            tags = listOf("خنده", "مینیون", "ریسه", "laugh", "minion", "lol", "rofl", "قهقهه"),
        ),
        CuratedGif(
            id = "curated_laugh_baby",
            url = "https://media.giphy.com/media/9w9Z2ZOJF80na/giphy.gif",
            width = 300,
            height = 220,
            tags = listOf("خنده", "بچه", "کودک", "laugh", "baby", "cute", "خوشحال"),
        ),
        CuratedGif(
            id = "curated_laugh_leo",
            url = "https://media.giphy.com/media/kd9BlRovbPOykLBMqX/giphy.gif",
            width = 320,
            height = 180,
            tags = listOf("خنده", "اشاره", "دی‌کاپریو", "point", "leo", "haha", "دیدیش", "نگاه"),
        ),

        // Love & Heart / عشق و محبت
        CuratedGif(
            id = "curated_love_heart",
            url = "https://media.giphy.com/media/26BRv0ThflsDTjDUs/giphy.gif",
            width = 300,
            height = 300,
            tags = listOf("عشق", "قلب", "دوستت دارم", "love", "heart", "romance", "عاشقانه", "kiss", "بوس"),
        ),
        CuratedGif(
            id = "curated_love_bear_hug",
            url = "https://media.giphy.com/media/l4pTfx2qLszoacZRS/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("بغل", "عشق", "خرس", "hug", "love", "cuddle", "آغوش", "مهربان"),
        ),
        CuratedGif(
            id = "curated_love_eyes",
            url = "https://media.giphy.com/media/3ofT5ySFXZ01oJ7aoM/giphy.gif",
            width = 300,
            height = 300,
            tags = listOf("عشق", "نگاه", "چشم", "love", "crush", "cute", "جذاب", "دلبر"),
        ),

        // Thumbs up & Approval / تایید و تشویق
        CuratedGif(
            id = "curated_thumbs_up_great",
            url = "https://media.giphy.com/media/111ebonMs90YLu/giphy.gif",
            width = 300,
            height = 200,
            tags = listOf("تایید", "عالی", "لایک", "thumbsup", "ok", "yes", "باشه", "دمت گرم", "آفرین"),
        ),
        CuratedGif(
            id = "curated_applause_clapping",
            url = "https://media.giphy.com/media/pubCVnPISkWbe/giphy.gif",
            width = 320,
            height = 240,
            tags = listOf("تشویق", "دست", "کف", "clap", "applause", "bravo", "احسنت", "آفرین"),
        ),
        CuratedGif(
            id = "curated_nod_agree",
            url = "https://media.giphy.com/media/NEvPzZ8bd1V4Y/giphy.gif",
            width = 300,
            height = 200,
            tags = listOf("تایید", "سر", "قبول", "nod", "agree", "yes", "موافقم", "درسته"),
        ),

        // Dance & Party / رقص و جشن
        CuratedGif(
            id = "curated_dance_snoop",
            url = "https://media.giphy.com/media/GeimqsH0TLDt4tScGw/giphy.gif",
            width = 300,
            height = 300,
            tags = listOf("رقص", "شادی", "پارتی", "dance", "party", "happy", "موزیک", "قر"),
        ),
        CuratedGif(
            id = "curated_celebration_confetti",
            url = "https://media.giphy.com/media/g9582DNuQppxC/giphy.gif",
            width = 320,
            height = 240,
            tags = listOf("جشن", "تبریک", "تولد", "celebrate", "party", "congrats", "مبارک", "شادی"),
        ),
        CuratedGif(
            id = "curated_dance_cat",
            url = "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("رقص", "گربه", "شادی", "dance", "cat", "cute", "قر بده"),
        ),

        // Wow & Shock / تعجب و شوک
        CuratedGif(
            id = "curated_wow_mindblown",
            url = "https://media.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif",
            width = 320,
            height = 240,
            tags = listOf("تعجب", "شوک", "واو", "wow", "mindblown", "omg", "پشمام", "عجب", "برگام"),
        ),
        CuratedGif(
            id = "curated_shock_cat",
            url = "https://media.giphy.com/media/MDJ9IbxxvDUQM/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("تعجب", "گربه", "شگفت", "shock", "cat", "surprised", "یا خدا"),
        ),

        // Sad & Crying / غم و گریه
        CuratedGif(
            id = "curated_sad_crying",
            url = "https://media.giphy.com/media/d2lcHJTG5Tscg/giphy.gif",
            width = 300,
            height = 240,
            tags = listOf("غم", "گریه", "ناراحت", "sad", "cry", "tears", "اشک", "افسرده", "حیف"),
        ),
        CuratedGif(
            id = "curated_sad_kitten",
            url = "https://media.giphy.com/media/OPU6wzx8JrHna/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("غم", "بچه گربه", "ناراحت", "sad", "kitten", "cry", "مظلوم", "دلشکسته"),
        ),

        // Greetings & Bye / سلام و خداحافظ
        CuratedGif(
            id = "curated_wave_hello",
            url = "https://media.giphy.com/media/Nx0rz3jtxt969j4Jn3/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("سلام", "درود", "احوال", "hello", "hi", "wave", "چطوری", "صبح بخیر"),
        ),
        CuratedGif(
            id = "curated_bye_wave",
            url = "https://media.giphy.com/media/m9eG1qVjvNINLFtg24/giphy.gif",
            width = 300,
            height = 240,
            tags = listOf("خداحافظ", "بای", "فعلا", "bye", "goodbye", "wave", "خدافظ", "شب بخیر"),
        ),

        // Sleep & Tired / خواب و خستگی
        CuratedGif(
            id = "curated_sleep_cat",
            url = "https://media.giphy.com/media/mvyByQFywcRaw/giphy.gif",
            width = 300,
            height = 200,
            tags = listOf("خواب", "خسته", "شب بخیر", "sleep", "tired", "goodnight", "بیهوش", "خمیازه"),
        ),

        // Coffee & Tea / قهوه و چای
        CuratedGif(
            id = "curated_coffee_sip",
            url = "https://media.giphy.com/media/3oKIPnAiaMCws8nOsE/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("قهوه", "کافه", "چای", "coffee", "tea", "cafe", "نوشیدنی", "انرژی"),
        ),

        // Fire & Cool / آتیش و خفن
        CuratedGif(
            id = "curated_fire_lit",
            url = "https://media.giphy.com/media/nrXif9YExO9EI/giphy.gif",
            width = 300,
            height = 250,
            tags = listOf("آتیش", "خفن", "عالی", "fire", "lit", "cool", "سوختم", "ترکوندی"),
        ),
    )

    fun trending(limit: Int = 20): List<GifItem> {
        return ITEMS.take(limit.coerceIn(1, 50)).map { it.toGifItem() }
    }

    fun search(query: String, limit: Int = 20): List<GifItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return trending(limit)

        val words = normalized.split(Regex("[\\s_\\-]+")).filter { it.isNotBlank() }

        val matches = ITEMS.filter { item ->
            words.any { word ->
                item.tags.any { tag ->
                    tag.contains(word, ignoreCase = true) || word.contains(tag, ignoreCase = true)
                }
            }
        }

        val result = if (matches.isNotEmpty()) matches else ITEMS
        return result.take(limit.coerceIn(1, 50)).map { it.toGifItem() }
    }
}
